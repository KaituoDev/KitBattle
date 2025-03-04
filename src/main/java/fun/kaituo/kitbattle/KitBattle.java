package fun.kaituo.kitbattle;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import fun.kaituo.gameutils.GameUtils;
import fun.kaituo.gameutils.game.Game;
import fun.kaituo.kitbattle.command.KitBattleGo;
import fun.kaituo.kitbattle.listener.ChooseKitSign;
import fun.kaituo.kitbattle.listener.InfiniteFirepowerSign;
import fun.kaituo.kitbattle.listener.RecoverOnKillSign;
import fun.kaituo.kitbattle.util.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.*;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.util.*;

import static fun.kaituo.gameutils.util.Misc.getMenu;

@SuppressWarnings("unused")
public class KitBattle extends Game implements Listener {
    private static KitBattle instance;

    public static KitBattle inst() {
        return instance;
    }

    public static final int PARTICLE_COUNT = 30;
    public static final float SOUND_VOLUME = 1.0f;

    public final Set<UUID> playerIds = new HashSet<>();
    public final Map<UUID, PlayerData> playerIdDataMap = new HashMap<>();

    private final Map<String, Class<? extends PlayerData>> kitClasses = new HashMap<>();

    private final Random random = new Random();
    // Change this to match the actual spawn location names.
    private final List<String> spawnLocNames = List.of("spawn1", "spawn2", "spawn3", "spawn4");
    private final List<Location> spawnLocs = new ArrayList<>();

    private InfiniteFirepowerSign infiniteFirepowerSign;
    private RecoverOnKillSign recoverOnKillSign;
    private ItemStack backItem;
    private Scoreboard mainBoard;
    private Scoreboard kitBattleBoard;
    private Objective killsObjective;

    private ProtocolManager protocolManager;

    public static void reset(Player p) {
        for (PotionEffect effect : p.getActivePotionEffects()) {
            p.removePotionEffect(effect.getType());
        }
        p.getInventory().clear();
        p.setHealth(20);
        p.setFoodLevel(20);
        p.setSaturation(5);
        p.setLevel(0);
        p.setExp(0);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isInArena(Player p) {
        boolean inGame = playerIds.contains(p.getUniqueId());
        boolean hasChosenKit = playerIdDataMap.get(p.getUniqueId()) != null;
        return inGame && hasChosenKit;
    }

    public Set<Player> getNearbyEnemies(Player p, double radius) {
        Set<Player> result = new HashSet<>();
        for (Entity e : p.getNearbyEntities(radius,radius,radius)) {
            if (!(e instanceof Player)) {
                continue;
            }
            if (p.equals(e)) {
                continue;
            }
            if (!isInArena((Player) e)) {
                continue;
            }
            result.add((Player) e);
        }
        return result;
    }


    public Player getNearestEnemy(Player p, double radius) {
        Player result = null;
        for (Player enemy : getNearbyEnemies(p, radius)) {
            if (result == null) {
                result = enemy;
            } else if (p.getLocation().distance(enemy.getLocation()) < p.getLocation().distance(result.getLocation())) {
                result = enemy;
            }
        }
        return result;
    }

    public void fakeEntityDestroy(Entity e) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
        List<Integer> idList = new ArrayList<>();
        idList.add(e.getEntityId());
        packet.getIntLists().write(0, idList);
        protocolManager.broadcastServerPacket(packet);
    }

    public void toArena(Player p, Class<? extends PlayerData> kitClass) {
        reset(p);
        p.teleport(getRandomSpawnLoc());
        p.playSound(p, Sound.ITEM_ARMOR_EQUIP_GOLD, SOUND_VOLUME, 1);
        PlayerData data;
        try {
            Constructor<? extends PlayerData> constructor = kitClass.getConstructor(Player.class);
            data = constructor.newInstance(p);
        } catch (Exception e) {
            p.sendMessage("§c初始化职业 " + kitClass.getSimpleName() + " 失败！");
            throw new RuntimeException(e);
        }
        PlayerData originalData = playerIdDataMap.get(p.getUniqueId());
        if (originalData != null) {
            originalData.onDestroy(p);
        }
        playerIdDataMap.put(p.getUniqueId(), data);
    }

    public void toHub(Player p) {
        reset(p);
        p.getInventory().addItem(getMenu());
        p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, -1, 4, false, false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, -1, 0, false, false));
        p.teleport(location);
        PlayerData originalData = playerIdDataMap.get(p.getUniqueId());
        if (originalData != null) {
            originalData.onDestroy(p);
        }
        playerIdDataMap.remove(p.getUniqueId());
    }

    public Class<? extends PlayerData> getKitClass(String name) {
        return kitClasses.get(name);
    }

    public Set<String> getKitNames() {
        return kitClasses.keySet();
    }

    public boolean isInfiniteFirepower() {
        return infiniteFirepowerSign.isInfiniteFirepower();
    }

    public boolean shouldRecoverOnKill() {
        return recoverOnKillSign.shouldRecoverOnKill();
    }

    public double getCooldownReductionMultiplier() {
        return getConfig().getDouble("cooldown-reduction-multiplier");
    }

    private void initSpawnLocs() {
        for (String locName : spawnLocNames) {
            if (getLoc(locName) != null) {
                spawnLocs.add(getLoc(locName));
            } else {
                getLogger().warning("Spawn location " + locName + " not found.");
            }
        }
    }

    public Location getRandomSpawnLoc() {
        return spawnLocs.get(random.nextInt(spawnLocs.size()));
    }

    private void initSigns() {
        Bukkit.getPluginManager().registerEvents(new ChooseKitSign(this, getLoc("choose-kit-sign")), this);
        infiniteFirepowerSign = new InfiniteFirepowerSign(this, getLoc("infinite-firepower-sign"));
        Bukkit.getPluginManager().registerEvents(infiniteFirepowerSign, this);
        recoverOnKillSign = new RecoverOnKillSign(this, getLoc("recover-on-kill-sign"));
        Bukkit.getPluginManager().registerEvents(recoverOnKillSign, this);
    }

    private void saveKills() {
        for (String entry : kitBattleBoard.getEntries()) {
            Score score = killsObjective.getScore(entry);
            getConfig().set("kills." + entry, score.getScore());
        }
        saveConfig();
    }

    private void loadKills() {
        ConfigurationSection section = getConfig().getConfigurationSection("kills");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            Score score = killsObjective.getScore(key);
            score.setScore(section.getInt(key));
        }
    }

    private void initScoreboard() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        assert manager != null;
        mainBoard = manager.getMainScoreboard();
        kitBattleBoard = manager.getNewScoreboard();
        killsObjective = kitBattleBoard.registerNewObjective("kills", Criteria.DUMMY, "击败榜");
        killsObjective.setDisplaySlot(org.bukkit.scoreboard.DisplaySlot.SIDEBAR);
    }

    private void registerCommand() {
        PluginCommand cmd = getCommand("kitbattlego");
        if (cmd == null) {
            getLogger().warning("Command not found: kitbattlego. Did you add it to plugin.yml?");
            return;
        }
        KitBattleGo kitBattleGo = new KitBattleGo();
        cmd.setExecutor(kitBattleGo);
        cmd.setTabCompleter(kitBattleGo);
    }

    private void registerKits() {
        Reflections reflections = new Reflections("fun.kaituo.kitbattle.kits");
        Set<Class<? extends PlayerData>> kitClassesFound = reflections.getSubTypesOf(PlayerData.class);
        for (Class<? extends PlayerData> kitClass : kitClassesFound) {
            this.kitClasses.put(kitClass.getSimpleName(), kitClass);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        Player killer = victim.getKiller();
        if (!isInArena(victim)) {
            return;
        }
        if (killer == null || victim.equals(killer)) {
            Score victimScore = killsObjective.getScore(victim.getName());
            victimScore.setScore(victimScore.getScore() - 1);
            return;
        }
        if (!isInArena(killer)) {
            killer.sendMessage("§c你不在职业战争中，为何击杀了职业战争玩家？");
            return;
        }
        Score killerScore = killsObjective.getScore(killer.getName());
        killerScore.setScore(killerScore.getScore() + 1);
        if (shouldRecoverOnKill()) {
            Bukkit.getScheduler().runTaskLater(this, () ->
                    killer.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH, 2, 199, false, true)), 1);
        }
    }

    @EventHandler
    public void onPlayerUseBackItem(PlayerInteractEvent e) {
        if (!isInArena(e.getPlayer())) {
            return;
        }
        if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK) && !e.getAction().equals(Action.RIGHT_CLICK_AIR)) {
            return;
        }
        ItemStack item = e.getItem();
        if (item == null) {
            return;
        }
        if (!item.equals(backItem)) {
            return;
        }
        toHub(e.getPlayer());
    }

    @EventHandler
    public void onPlayerTryCastSkill(PlayerInteractEvent e) {
        if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK) && !e.getAction().equals(Action.RIGHT_CLICK_AIR)) {
            return;
        }
        if (!playerIds.contains(e.getPlayer().getUniqueId())) {
            return;
        }
        ItemStack item = e.getItem();
        if (item == null) {
            return;
        }
        if (item.getItemMeta() == null) {
            return;
        }
        // We use fortune enchantment to identify skill items
        if (!item.getItemMeta().hasEnchant(Enchantment.FORTUNE)) {
            return;
        }
        Player p = e.getPlayer();
        PlayerData data = playerIdDataMap.get(p.getUniqueId());
        assert data != null;
        data.tryCastSkill(p);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        if (!playerIds.contains(p.getUniqueId())) {
            return;
        }
        PlayerData data = playerIdDataMap.get(p.getUniqueId());
        if (data == null) {
            p.sendMessage("§c你怎么在休息室死掉了？");
            toHub(p);
        } else {
            Class<? extends PlayerData> kitClass = data.getClass();
            Bukkit.getScheduler().runTaskLater(this, () -> toArena(p, kitClass), 1);
        }
    }

    @EventHandler
    public void preventDropItem(PlayerDropItemEvent e) {
        if (playerIds.contains(e.getPlayer().getUniqueId())) {
            e.setCancelled(true);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void addPlayer(Player p) {
        playerIds.add(p.getUniqueId());
        p.setBedSpawnLocation(location, true);
        p.setScoreboard(kitBattleBoard);

        PlayerData data = playerIdDataMap.get(p.getUniqueId());
        if (data != null) {
            data.onRejoin(p);
        } else {
            toHub(p);
        }
    }

    @Override
    public void removePlayer(Player p) {
        PlayerData data = playerIdDataMap.get(p.getUniqueId());
        if (data != null) {
            data.onQuit(p);
        }
        p.setScoreboard(mainBoard);
        playerIds.remove(p.getUniqueId());
        reset(p);
    }

    @Override
    public void forceStop() {

    }

    @Override
    public void tick() {
        for (UUID uuid : playerIds) {
            PlayerData data = playerIdDataMap.get(uuid);
            // If player is not in the arena
            if (data == null) {
                return;
            }
            Player p = Bukkit.getPlayer(uuid);
            assert p != null;
            data.tick(p);
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();

        instance = this;
        saveDefaultConfig();
        updateExtraInfo("§e职业战争", getLoc("hub"));
        initSigns();
        initSpawnLocs();
        initScoreboard();
        loadKills();
        registerCommand();
        registerKits();
        backItem = getItem("back");
        protocolManager = ProtocolLibrary.getProtocolManager();
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        super.onDisable();

        saveKills();
        Bukkit.getScheduler().cancelTasks(this);
        for (UUID uuid : new ArrayList<>(playerIds)) {
            Player p = Bukkit.getPlayer(uuid);
            assert p != null;
            removePlayer(p);
            GameUtils.inst().join(p, GameUtils.inst().getLobby());
        }
    }
}
