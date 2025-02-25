package fun.kaituo.kitbattle;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import fun.kaituo.gameutils.GameUtils;
import fun.kaituo.gameutils.game.Game;
import fun.kaituo.kitbattle.command.KitBattleGo;
import fun.kaituo.kitbattle.kits.Batter;
import fun.kaituo.kitbattle.kits.BlackWolf;
import fun.kaituo.kitbattle.kits.BladeMaster;
import fun.kaituo.kitbattle.kits.Kit;
import fun.kaituo.kitbattle.listener.ChooseKitSign;
import fun.kaituo.kitbattle.listener.InfiniteFirepowerSign;
import fun.kaituo.kitbattle.util.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.PluginCommand;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

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

    private final Map<String, Kit> kits = new HashMap<>();

    private final Random random = new Random();
    // Change this to match the actual spawn location names.
    private final List<String> spawnLocNames = List.of("spawn1", "spawn2", "spawn3", "spawn4");
    private final List<Location> spawnLocs = new ArrayList<>();

    private double cooldownReductionMultiplier;
    private InfiniteFirepowerSign infiniteFirepowerSign;
    private ItemStack backItem;

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

    public Set<Player> getNearbyEnemies(Player p, double radius) {
        Set<Player> result = new HashSet<>();
        for (Entity e : p.getNearbyEntities(radius,radius,radius)) {
            if (p.equals(e)) {
                continue;
            }
            if (!playerIds.contains(e.getUniqueId())) {
                continue;
            }
            result.add((Player) e);
        }
        return result;
    }


    public Player getNearestEnemy(Player p, double radius) {
        Player result = null;
        for (Entity e : p.getNearbyEntities(radius,70,radius)) {
            if (p.equals(e)) {
                continue;
            }
            if (!playerIds.contains(e.getUniqueId())) {
                continue;
            }
            if (result == null) {
                result = (Player) e;
            } else if (p.getLocation().distance(e.getLocation()) < p.getLocation().distance(result.getLocation())) {
                result = (Player) e;
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

    public void toArena(Player p, Kit kit) {
        reset(p);
        kit.applyInventory(p);
        kit.applyPotionEffects(p);
        p.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, -1, 4, false, false));
        // Give player glowing and regeneration for 3 seconds
        p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 60, 0, false, false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 9, false, false));
        p.teleport(getRandomSpawnLoc());

        PlayerData data = new PlayerData(kit);
        playerIdDataMap.put(p.getUniqueId(), data);
    }

    public void toHub(Player p) {
        reset(p);
        p.getInventory().addItem(getMenu());
        p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, -1, 4, false, false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, -1, 0, false, false));
        p.teleport(location);
        playerIdDataMap.remove(p.getUniqueId());
    }

    public Kit getKit(String name) {
        return kits.get(name);
    }

    public Set<String> getKitNames() {
        return kits.keySet();
    }

    public void registerKit(Kit kit) {
        kits.put(kit.getClass().getSimpleName(), kit);
    }

    public boolean isInfiniteFirepower() {
        return infiniteFirepowerSign.isInfiniteFirepower();
    }

    public double getCooldownReductionMultiplier() {
        return cooldownReductionMultiplier;
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
        registerKit(new Batter());
        registerKit(new BlackWolf());
        registerKit(new BladeMaster());
    }

    @EventHandler
    public void onPlayerUseBackItem(PlayerInteractEvent e) {
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
            Kit kit = data.getKit();
            Bukkit.getScheduler().runTaskLater(this, () -> toArena(p, kit), 1);
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

        PlayerData data = playerIdDataMap.get(p.getUniqueId());
        if (data != null) {
            data.load(p);
        } else {
            toHub(p);
        }
    }

    @Override
    public void removePlayer(Player p) {
        PlayerData data = playerIdDataMap.get(p.getUniqueId());
        if (data != null) {
            data.save(p);
        }
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
        instance = this;
        super.onEnable();
        saveDefaultConfig();
        updateExtraInfo("§e职业战争", getLoc("hub"));
        initSigns();
        initSpawnLocs();
        registerCommand();
        registerKits();
        cooldownReductionMultiplier = getConfig().getDouble("cooldown-reduction-multiplier");
        backItem = getItem("back");
        protocolManager = ProtocolLibrary.getProtocolManager();
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        for (UUID uuid : new ArrayList<>(playerIds)) {
            Player p = Bukkit.getPlayer(uuid);
            assert p != null;
            removePlayer(p);
            p.teleport(GameUtils.inst().getLobby().getLocation());
        }
    }
}
