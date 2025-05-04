package fun.kaituo.kitbattle;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import fun.kaituo.gameutils.GameUtils;
import fun.kaituo.gameutils.game.Game;
import fun.kaituo.kitbattle.command.KitBattleGo;
import fun.kaituo.kitbattle.listener.ChooseKitSign;
import fun.kaituo.kitbattle.listener.HitIntervalSign;
import fun.kaituo.kitbattle.listener.InfiniteFirepowerSign;
import fun.kaituo.kitbattle.listener.RecoverOnKillSign;
import fun.kaituo.kitbattle.util.PlayerData;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

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
    private HitIntervalSign hitIntervalSign;
    private ItemStack backItem;
    private Scoreboard mainBoard;
    private Scoreboard kitBattleBoard;
    private Objective killsObjective;

    private ProtocolManager protocolManager;

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
        PlayerData originalData = playerIdDataMap.get(p.getUniqueId());
        if (originalData != null) {
            originalData.onDestroy();
        } else {
            p.getInventory().clear();
            p.setSaturation(5);
            p.removePotionEffect(PotionEffectType.RESISTANCE);
            p.removePotionEffect(PotionEffectType.SATURATION);
        }

        // 设置玩家的无敌帧时间
        if (hasHitInterval()) {
            p.setMaximumNoDamageTicks(10); // 1秒无敌帧
        } else {
            p.setMaximumNoDamageTicks(0); // 无无敌帧
        }

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
        playerIdDataMap.put(p.getUniqueId(), data);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        // 处理所有实体的无敌时间
        if (!hasHitInterval()) {
            if (e.getEntity() instanceof LivingEntity) {
                LivingEntity entity = (LivingEntity) e.getEntity();
                // 强制设置当前无敌时间和最大无敌时间为0
                entity.setNoDamageTicks(0);
                entity.setMaximumNoDamageTicks(0);
            }
            return;
        }

        // 仅当HitInterval开启时才处理玩家的无敌时间
        if (e.getEntity() instanceof Player) {
            Player player = (Player) e.getEntity();
            if (!isInArena(player)) return;

            if (player.getNoDamageTicks() > 0) {
                e.setCancelled(true);
            }
        }
    }


    public void toHub(Player p) {
        PlayerData originalData = playerIdDataMap.get(p.getUniqueId());
        if (originalData != null) {
            originalData.onDestroy();
        }
        playerIdDataMap.remove(p.getUniqueId());
        p.getInventory().addItem(getMenu());
        p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, -1, 4, false, false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, -1, 0, false, false));
        p.teleport(getGameTeleportLocation());
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

    public boolean hasHitInterval() {return hitIntervalSign.HitInterval(); }

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
        hitIntervalSign = new HitIntervalSign(this, getLoc("hit-interval-sign"));
        Bukkit.getPluginManager().registerEvents(hitIntervalSign, this);
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
        mainBoard = manager.getMainScoreboard();
        kitBattleBoard = manager.getNewScoreboard();
        killsObjective = kitBattleBoard.registerNewObjective("kills", Criteria.DUMMY, "击败榜");
        killsObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
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
        try (ScanResult scanResult = new ClassGraph()
                .acceptPackages("fun.kaituo.kitbattle.kit") // 指定扫描的包
                .enableClassInfo()
                .scan()) {

            Set<Class<? extends PlayerData>> kitClasses = new HashSet<>(scanResult
                    .getSubclasses(PlayerData.class.getName()) // 获取子类
                    .loadClasses(PlayerData.class));

            for (Class<? extends PlayerData> kitClass : kitClasses) {
                this.kitClasses.put(kitClass.getSimpleName(), kitClass);
            }
        } catch (Exception e) {
            getLogger().warning("Failed to scan for kit classes");
            throw new RuntimeException(e);
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

    @Override
    @SuppressWarnings("deprecation")
    public void addPlayer(Player p) {
        playerIds.add(p.getUniqueId());
        p.setBedSpawnLocation(getGameTeleportLocation(), true);
        p.setScoreboard(kitBattleBoard);

        PlayerData data = playerIdDataMap.get(p.getUniqueId());
        if (data != null) {
            data.onRejoin();
        } else {
            toHub(p);
        }
    }

    @Override
    public void removePlayer(Player p) {
        PlayerData data = playerIdDataMap.get(p.getUniqueId());
        if (data != null) {
            data.onQuit();
        } else {
            p.getInventory().clear();
            p.setSaturation(5);
            p.removePotionEffect(PotionEffectType.RESISTANCE);
            p.removePotionEffect(PotionEffectType.SATURATION);
        }
        p.setScoreboard(mainBoard);
        playerIds.remove(p.getUniqueId());
    }

    @Override
    public void forceStop() {

    }

    @Override
    public void tick() {

    }

    @Override
    public void onEnable() {
        super.onEnable();
        instance = this;
        saveDefaultConfig();
        updateExtraInfo("§e职业战争", getLoc("hub"));
        backItem = getItem("back");
        protocolManager = ProtocolLibrary.getProtocolManager();
        initScoreboard();
        loadKills();
        initSpawnLocs();

        Bukkit.getScheduler().runTaskLater(this, () -> {
            initSigns();
            registerCommand();
            registerKits();
            Bukkit.getPluginManager().registerEvents(this, this);
        }, 1);
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
