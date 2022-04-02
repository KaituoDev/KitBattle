package fun.kaituo;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import fun.kaituo.event.PlayerChangeGameEvent;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SuspiciousStewMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.io.IOException;
import java.util.*;

import static fun.kaituo.GameUtils.world;

public class KitBattleGame extends Game implements Listener {
    private static final KitBattleGame instance = new KitBattleGame((KitBattle) Bukkit.getPluginManager().getPlugin("KitBattle"));
    Scoreboard kitBattle;
    HashMap<Player, List<Long>> coolDown;
    HashMap<Player, List<Integer>> playerTaskIds;
    //HashMap<Player, Long> cd2;
    int particleNumber = 30;
    float soundVolume = 1.0f;
    FileConfiguration c;
    ProtocolManager pm;
    public Location spawnLocations[] = {new Location(world, 41.5,80.0625,1000.5,90,0),
        new Location(world, 15.5,66,1000.5,90,0),
        new Location(world, 28.5,66,976.5,45,0),
        new Location(world, -6.5,66,1021.5,180,0),
        new Location(world, -12.5,66,1000.5,-90,0),
        new Location(world, -12.5,66,978.5,-45,0),
        new Location(world, -40.5,55,954.5,-90,0)};

    private KitBattleGame(KitBattle plugin) {
        this.plugin = plugin;
        c = plugin.getConfig();
        players = plugin.players;
        kitBattle = Bukkit.getScoreboardManager().getNewScoreboard();
        kitBattle.registerNewObjective("kitBattleKills", "dummy", "职业战争击败榜");
        kitBattle.getObjective("kitBattleKills").setDisplaySlot(DisplaySlot.SIDEBAR);
        coolDown = new HashMap<>();
        playerTaskIds = new HashMap<>();
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, ()-> {
            for (Player p : players) {
                if (coolDown.get(p) == null) {
                    List<Long> l = new ArrayList<>(2);
                    l.add(0,0L);
                    l.add(1,0L);
                    coolDown.put(p, l);
                } else {
                    List<Long> l = coolDown.get(p);
                    //p.sendMessage(String.valueOf(l.get(0)));
                    if (l.get(0) > 0) {
                        l.set(0, l.get(0) - 1);
                    }
                    if (l.get(1) != 0) {
                        p.setLevel((int) Math.ceil(((float)l.get(0)) / 20));
                        p.setExp(((float)(l.get(1) - l.get(0))) / l.get(1));
                    }

                }
                if (playerTaskIds.get(p) == null) {
                    playerTaskIds.put(p, new ArrayList<>());
                }
            }
        }, 1,1 );
        //cd2 = new HashMap<Player, Long>();
        initializeGame(plugin, "KitBattle", "§a职业战争", new Location(world, 0,201,1000), new BoundingBox(-300, -64, 700, 300, 320, 1300));

        Bukkit.getScheduler().runTask(plugin, () -> {
            pm = ProtocolLibrary.getProtocolManager();
        });
    }

    public static KitBattleGame getInstance() {
        return instance;
    }

    @EventHandler
    public void preventDamage(EntityDamageEvent ede) {
        BoundingBox hub = new BoundingBox(-500, 200, 500, 500, 256, 1500);
        if (hub.contains(ede.getEntity().getLocation().toVector())) {
            ede.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent pde) {
        Player player = pde.getEntity();
        Player killer = pde.getEntity().getKiller();

        clearCoolDownAndTask(player);
        if (killer == null) {
            return;
        } //不是玩家
        //if (!player.getScoreboardTags().contains("zyzz")) { return; }//死亡者没有tag

        if (player.equals(killer)) {
            return;
        } //自杀判定
        if (players.contains(killer)) {
            Score score = kitBattle.getObjective("kitBattleKills").getScore(killer);
            score.setScore(score.getScore() + 1);//加分
        } else {
            return;//不处于游戏中
        }

        if (player.getGameMode().equals(GameMode.ADVENTURE)) {
            player.setAllowFlight(false);
        }

        if (killer.getInventory().getChestplate() == null) {
            if (killer.getInventory().getHelmet() != null) {
                if (killer.getInventory().getHelmet().getType().equals(Material.ZOMBIE_HEAD)) {
                    killer.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 2));
                }
            }
        }
        ItemStack stew = new ItemStack(Material.SUSPICIOUS_STEW, 1);
        SuspiciousStewMeta stewMeta = (SuspiciousStewMeta) stew.getItemMeta();
        stewMeta.addCustomEffect(new PotionEffect(PotionEffectType.REGENERATION, 160, 0), true);
        stew.setItemMeta(stewMeta);
        killer.getInventory().addItem(stew);
        /*
        if (killer.getInventory().getChestplate().getItemMeta() == null) {
            return;
        }//没有meta
        //这里添加内容

        switch (killer.getInventory().getChestplate().getItemMeta().getDisplayName()) {
            case ""
            case "战士胸甲":
            case "坦克胸甲":
            case "太空人胸甲":
            case "海盗胸甲":
            case "地狱战士胸甲":
            case "忍者胸甲":
            case "审判者胸甲":
            case "咏春胸甲":
                double maxHealth = killer.getMaxHealth();
                if (killer.getMaxHealth() - killer.getHealth() <= 4) {
                    killer.setHealth(killer.getMaxHealth());
                } else {
                    killer.setHealth(killer.getHealth() + 4);
                }
                break;
            case "弓箭手胸甲":
            case "神箭游侠胸甲":
                killer.getInventory().addItem(new ItemStack(Material.ARROW, 8));
                break;
            case "熊孩子胸甲":
                killer.getInventory().addItem(new ItemStack(Material.EGG, 4));
                killer.getInventory().addItem(new ItemStack(Material.SNOWBALL, 8));
                break;


        }
              */

    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent phe) {
        if (!players.contains(phe.getEntity().getShooter())) {
            return;
        }
        if ((phe.getEntity() instanceof Fireball)) {
            if (!(phe.getEntity().getShooter() instanceof Player)) {
                return;
            }
            if (phe.getEntity().getScoreboardTags().contains("kitBattleMage")) {
                phe.setCancelled(true);
                phe.getEntity().remove();
                world.createExplosion(phe.getEntity().getLocation(), 2F, false, false, (Entity) phe.getEntity().getShooter());
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent pie) {
        if (!pie.getAction().equals(Action.RIGHT_CLICK_AIR) && !pie.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }// 不是右键
        if (pie.getItem() == null) {
            return;
        }//没有物品
        if (pie.getItem().getItemMeta() == null) {
            return;
        }//没有meta
        Player executor = pie.getPlayer();
        if (!players.contains(executor)) {
            return;
        }//不在zyzz里
        if (executor.getLocation().getY() > 128) {
            return;
        }//在高空
        //这里开始添加内容

        switch (pie.getItem().getItemMeta().getDisplayName()) {
            case "裂地":
                Player victim = getNearestPlayer(executor, 6);
                if (victim != null) {
                    if (checkCoolDown(executor, (long)(800 * getCoolDownReductionMultiplier()))) {
                        executor.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 20, 49));
                        executor.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 40, 4));
                        executor.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 40, 0));
                        world.playSound(executor.getLocation(),Sound.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, soundVolume, 1.5f);
                        BlockData d = null;
                        Location l = executor.getLocation().clone();
                        if (!world.getBlockAt(l.getBlockX(),l.getBlockY(),l.getBlockZ()).getType().equals(Material.AIR)) {
                            d = world.getBlockData(l.getBlockX(),l.getBlockY(),l.getBlockZ());
                        } else if (!world.getBlockAt(l.getBlockX(),l.getBlockY() - 1,l.getBlockZ()).getType().equals(Material.AIR)) {
                            d = world.getBlockData(l.getBlockX(),l.getBlockY() - 1,l.getBlockZ());
                        }
                        if (d != null) {
                            world.spawnParticle(Particle.BLOCK_CRACK, executor.getLocation(), particleNumber, 3, 3, 3, d);
                        } else {
                            world.spawnParticle(Particle.BLOCK_CRACK, executor.getLocation(), particleNumber, 3, 3, 3, world.getBlockData(0, 75, 0));
                        }
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            executor.setVelocity(new Vector(0,0,0));
                            executor.teleport(victim);
                            world.createExplosion(victim.getLocation(), 1, false, false, executor);
                        }, 20);
                    }
                }
                break;
            case "制毒":
                if (!executor.getInventory().contains(Material.TIPPED_ARROW)) {
                    if (checkCoolDown(executor, (long)(300 * getCoolDownReductionMultiplier()))) {
                        ItemStack arrow = new ItemStack(Material.TIPPED_ARROW);
                        PotionMeta meta = (PotionMeta)arrow.getItemMeta();
                        meta.setBasePotionData(new PotionData(PotionType.POISON));
                        meta.addCustomEffect(new PotionEffect(PotionEffectType.POISON, 40, 2), true);
                        arrow.setItemMeta(meta);
                        executor.getInventory().addItem(arrow);
                        world.playSound(executor.getLocation(), Sound.BLOCK_GRASS_BREAK , SoundCategory.PLAYERS, soundVolume, 1);
                        world.spawnParticle(Particle.SPORE_BLOSSOM_AIR , executor.getLocation(), particleNumber, 3, 3, 3);
                    }
                }
                break;
            case "剑技":
                Set<Player> victims = getNearbyPlayers(executor,3);
                if (!victims.isEmpty()) {
                    if (checkCoolDown(executor, (long)(200 * getCoolDownReductionMultiplier()))) {
                        for (Player v : victims) {
                            Arrow a = executor.launchProjectile(Arrow.class, new Vector(0, -2, 0));
                            Location l = v.getLocation().clone();
                            l.setY(l.getY() + 2.5 );
                            a.teleport(l);
                            broadcastEntityDestroyPacket(a);
                            world.playSound(executor.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP , SoundCategory.PLAYERS, soundVolume, 1);
                            world.spawnParticle(Particle.SWEEP_ATTACK, executor.getLocation(), particleNumber, 3, 3, 3);
                        }
                    }
                }
                break;
            case "火球术":
                if (checkCoolDown(executor, (long)(280 * getCoolDownReductionMultiplier()))) {
                    //Fireball fireball = executor.launchProjectile(Fireball.class, executor.getEyeLocation().getDirection().normalize().multiply(0.8));
                    Fireball fireball = (Fireball) world.spawnEntity(executor.getEyeLocation().clone().add(executor.getEyeLocation().getDirection()), EntityType.FIREBALL, false);
                    fireball.setShooter(executor);
                    fireball.getScoreboardTags().add("kitBattleMage");
                    world.playSound(executor.getLocation(), Sound.ENTITY_GHAST_SHOOT , SoundCategory.PLAYERS, soundVolume, 1);
                    world.spawnParticle(Particle.SPELL_WITCH, executor.getLocation(), particleNumber, 3, 3, 3);
                }
                break;
            case "英魂":
                if (checkCoolDown(executor, (long)(999 * getCoolDownReductionMultiplier()))) {
                    executor.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 999999, 1));
                    executor.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 999999, 0));
                    world.playSound(executor.getLocation(), Sound.ENTITY_WITHER_AMBIENT , SoundCategory.PLAYERS, soundVolume, 1);
                    world.spawnParticle(Particle.VILLAGER_ANGRY, executor.getLocation(), particleNumber, 3, 3, 3);
                }
                break;
            case "闪身":
                Player target = getNearestPlayer(executor, 200);
                if (target != null) {
                    if (checkCoolDown(executor, (long)(200 * getCoolDownReductionMultiplier()))) {
                        Vector vec = target.getLocation().toVector().subtract(executor.getLocation().toVector());
                        if (vec.length() < 5) {
                            executor.teleport(target);
                        } else {
                            Vector movementVec = vec.multiply(5 / vec.length());
                            executor.teleport(executor.getLocation().add(movementVec));
                        }
                        world.playSound(executor.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT , SoundCategory.PLAYERS, soundVolume, 1);
                        world.spawnParticle(Particle.DRAGON_BREATH, executor.getLocation(), particleNumber, 3, 3, 3);
                    }
                }
                break;
            case "幻术":
                Player target2 = getNearestPlayer(executor, 6);
                if (target2 != null) {
                    if (checkCoolDown(executor, (long)(240 * getCoolDownReductionMultiplier()))) {
                        Location loc1 = executor.getLocation();
                        Location loc2 = target2.getLocation();
                        executor.teleport(loc2);
                        target2.teleport(loc1);
                        world.playSound(executor.getLocation(), Sound.ENTITY_SHULKER_TELEPORT , SoundCategory.PLAYERS, soundVolume, 1);
                        world.spawnParticle(Particle.DRAGON_BREATH, executor.getLocation(), particleNumber, 3, 3, 3);
                    }
                }
                break;
            case "重力禁锢":
                Set<Player> victims2 = getNearbyPlayers(executor,4);
                if (!victims2.isEmpty()) {
                    if (checkCoolDown(executor, (long)(600 * getCoolDownReductionMultiplier()))) {
                        for (Player v : victims2) {
                            v.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 3));
                            world.playSound(executor.getLocation(), Sound.ENTITY_SHULKER_BULLET_HIT , SoundCategory.PLAYERS, soundVolume, 1);
                            world.spawnParticle(Particle.SCRAPE, executor.getLocation(), particleNumber, 3, 3, 3);
                        }
                    }
                }
                break;
            case "唤魔":
                Player target3 = getNearestPlayer(executor, 200);
                if (target3 != null) {
                    if (checkCoolDown(executor, (long)(600 * getCoolDownReductionMultiplier()))) {
                        target3.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0));
                        target3.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 0));
                        Location l = target3.getLocation();
                        Location l2 = l.clone(); l2.setX(l2.getX() - 1);
                        Location l3 = l.clone(); l3.setX(l3.getX() + 1);
                        Location l4 = l.clone(); l4.setZ(l4.getZ() - 1);
                        Location l5 = l.clone(); l5.setZ(l5.getZ() + 1);
                        EvokerFangs[] fangs = new EvokerFangs[5];
                        fangs[0] = (EvokerFangs) world.spawnEntity(l, EntityType.EVOKER_FANGS);
                        fangs[1] = (EvokerFangs) world.spawnEntity(l2, EntityType.EVOKER_FANGS);
                        fangs[2] = (EvokerFangs) world.spawnEntity(l3, EntityType.EVOKER_FANGS);
                        fangs[3] = (EvokerFangs) world.spawnEntity(l4, EntityType.EVOKER_FANGS);
                        fangs[4] = (EvokerFangs) world.spawnEntity(l5, EntityType.EVOKER_FANGS);

                        for (EvokerFangs fang : fangs) {
                            fang.setOwner(executor);
                        }
                    }
                }
                break;
            case "飞行":
                if (checkCoolDown(executor, (long)(800 * getCoolDownReductionMultiplier()))) {
                    executor.setAllowFlight(true);
                    Bukkit.getScheduler().runTaskLater(plugin, ()-> {
                        executor.setAllowFlight(false);
                    }, 100);
                    world.playSound(executor.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH , SoundCategory.PLAYERS, soundVolume, 1);
                    world.spawnParticle(Particle.END_ROD, executor.getLocation(), particleNumber, 3, 3, 3);
                }
                break;
            case "狂乱":
                if (checkCoolDown(executor, (long)(300 * getCoolDownReductionMultiplier()))) {
                    executor.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 60, 1));
                    executor.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 1));
                    world.playSound(executor.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL , SoundCategory.PLAYERS, soundVolume, 2);
                    world.spawnParticle(Particle.VILLAGER_ANGRY, executor.getLocation(), particleNumber, 3, 3, 3);
                }
                break;
            case "自爆":
                world.createExplosion(executor.getLocation(), 3, false, false, executor);
                executor.setHealth(0);
                break;
            case "无冕":
                if (checkCoolDown(executor, (long)(300 * getCoolDownReductionMultiplier()))) {
                    executor.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20, 0));
                    executor.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20, 2));
                    world.playSound(executor.getLocation(), Sound.BLOCK_BEACON_ACTIVATE , SoundCategory.PLAYERS, soundVolume, 2);
                    world.spawnParticle(Particle.SPELL_INSTANT, executor.getLocation(), particleNumber, 3, 3, 3);
                }
                break;
            case "唤起风暴":
                if (checkCoolDown(executor, (long)(300 * getCoolDownReductionMultiplier()))) {
                    executor.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 4, 2));
                    executor.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 4, 2));
                    executor.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 4, 250));
                    playerTaskIds.get(executor).add(Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        for (Player v : getNearbyPlayers(executor, 3)) {
                            Arrow a = executor.launchProjectile(Arrow.class, new Vector(0, c.getDouble("stormknight.first-arrow-downward-speed"), 0));
                            Location l = v.getLocation().clone();
                            l.setY(l.getY() + 2.5 );
                            a.teleport(l);
                            broadcastEntityDestroyPacket(a);
                        }
                        world.spawnParticle(Particle.CLOUD, executor.getLocation(), particleNumber, 2, 2, 2);
                    }, 20).getTaskId());
                    playerTaskIds.get(executor).add(Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        for (Player v : getNearbyPlayers(executor, 3)) {
                            Arrow a = executor.launchProjectile(Arrow.class, new Vector(0, c.getDouble("stormknight.second-arrow-downward-speed"), 0));
                            Location l = v.getLocation().clone();
                            l.setY(l.getY() + 2.5 );
                            a.teleport(l);
                            broadcastEntityDestroyPacket(a);
                        }
                        world.spawnParticle(Particle.CLOUD, executor.getLocation(), particleNumber, 3, 3, 3);
                    }, 40).getTaskId());
                    playerTaskIds.get(executor).add(Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        for (Player v : getNearbyPlayers(executor, 3)) {
                            Arrow a = executor.launchProjectile(Arrow.class, new Vector(0, c.getDouble("stormknight.third-arrow-downward-speed"), 0));
                            Location l = v.getLocation().clone();
                            l.setY(l.getY() + 2.5 );
                            a.teleport(l);
                            broadcastEntityDestroyPacket(a);
                        }
                        world.spawnParticle(Particle.CLOUD, executor.getLocation(), particleNumber, 4, 4, 4);
                    }, 60).getTaskId());
                }
        }
    }

    private Player getNearestPlayer(Player p, double radius) {
        Player target = null;
        for (Entity e : p.getNearbyEntities(radius,70,radius)) {
            if (p.equals(e)) {
                continue;
            }
            if (!players.contains(e)) {
                continue;
            }
            if (target == null) {
                target = (Player) e;
            } else {
                if (p.getLocation().distance(e.getLocation()) < p.getLocation().distance(target.getLocation())) {
                    target = (Player) e;
                }
            }
        }

        return target;
    }

    private Set<Player> getNearbyPlayers(Player p, double radius) {
        Set<Player> result = new HashSet<>();
        for (Entity e : p.getNearbyEntities(radius,70,radius)) {
            if (p.equals(e)) {
                continue;
            }
            if (!players.contains(e)) {
                continue;
            }
            result.add((Player) e);
        }
        return result;
    }

    @EventHandler
    public void preventDropping(PlayerDropItemEvent pdie) {
        if (players.contains(pdie.getPlayer())) {
            pdie.setCancelled(true);
        }
    }


    @EventHandler
    public void cancelExplosion(EntityExplodeEvent eee) {
        if (eee.getEntity() instanceof Fireball) {
            if (eee.getEntity().getScoreboardTags().contains("kitBattleMage")) {
                eee.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void cancelProjectileDamage(EntityDamageByEntityEvent edbee) {
        if (!(edbee.getEntity() instanceof Player)) {
            return;
        }
        if (!(edbee.getDamager() instanceof Projectile)) {
            return;
        }
        if (((Projectile) edbee.getDamager()).getShooter() == null) {
            return;
        }
        if (!(((Projectile) edbee.getDamager()).getShooter() instanceof Player)) {
            return;
        }
        if (edbee.getDamager().equals(edbee.getEntity())) {
            edbee.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerChangeGame(PlayerChangeGameEvent pcge) {
        players.remove(pcge.getPlayer());
        clearCoolDownAndTask(pcge.getPlayer());
    }

    @EventHandler
    public void clearKills(PlayerInteractEvent pie) {
        if (pie.getClickedBlock() == null) {
            return;
        }
        Location location = pie.getClickedBlock().getLocation();
        long x = location.getBlockX();
        long y = location.getBlockY();
        long z = location.getBlockZ();
        if (x == 0 && y == 202 && z == 1003) {
            if (!players.contains(pie.getPlayer())) {
                players.add(pie.getPlayer());
            }
            pie.getPlayer().setScoreboard(kitBattle);
        } else if (x == 0 && y == 203 && z == 997) {
            kitBattle.getObjective("kitBattleKills").unregister();
            kitBattle.registerNewObjective("kitBattleKills", "dummy", "职业战争击败榜");
            kitBattle.getObjective("kitBattleKills").setDisplaySlot(DisplaySlot.SIDEBAR);
        }
    }

    @Override
    protected void initializeGameRunnable() {
        gameRunnable = () -> {
            Bukkit.getPluginManager().registerEvents(this, plugin);
        };
    }

    @Override
    protected void savePlayerQuitData(Player p) throws IOException {
        players.remove(p);
    }

    @Override
    protected void rejoin(Player player) {

    }

    private void sendEntityDestroyPacket(Player p, Entity e) {
        PacketContainer removeArrow = pm.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
        List<Integer> idList = new ArrayList<>();
        idList.add(e.getEntityId());
        removeArrow.getIntLists().write(0, idList);
        //removeArrow.getIntegerArrays().write(0,new int[] {projectile.getEntityId()});
        try {
            pm.sendServerPacket(p, removeArrow);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private void broadcastEntityDestroyPacket(Entity e) {
        PacketContainer removeArrow = pm.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
        List<Integer> idList = new ArrayList<>();
        idList.add(e.getEntityId());
        removeArrow.getIntLists().write(0, idList);
        //removeArrow.getIntegerArrays().write(0,new int[] {projectile.getEntityId()});
        try {
            pm.broadcastServerPacket(removeArrow);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public void clearCoolDownAndTask(Player p) {
        p.setLevel(0);
        p.setExp(0);
        if (coolDown.get(p) != null) {
            coolDown.get(p).set(0, 0L);
            coolDown.get(p).set(1, 0L);
        }
        if (playerTaskIds.get(p) != null) {
            for (int id : playerTaskIds.get(p)) {
                Bukkit.getScheduler().cancelTask(id);
            }
            playerTaskIds.get(p).clear();
        }
        /*
        if (cd2.get(p) != null) {
            cd2.remove(p);
        }

         */
    }

    public boolean checkCoolDown(Player p, long cd) {
        p.sendMessage("cd为" + cd + "游戏刻");
        if (coolDown.get(p).get(0) == 0) {
            coolDown.get(p).set(0, cd);
            coolDown.get(p).set(1, cd);
            return true;
        } else {
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§4§l技能冷却中！"));
            return false;
        }
    }

    private double getCoolDownReductionMultiplier() {
        if (world.getBlockAt(4, 202, 1000).isBlockPowered()) {
            return  (1 - c.getDouble("cooldown-reduction-multiplier"));
        } else {
            return 1;
        }
    }
}
