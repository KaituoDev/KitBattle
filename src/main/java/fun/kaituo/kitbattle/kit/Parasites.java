package fun.kaituo.kitbattle.kit;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import fun.kaituo.kitbattle.util.PlayerData;
import fun.kaituo.kitbattle.KitBattle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class Parasites extends PlayerData implements Listener {
    private final UUID ownerUUID;
    private final List<Entity> summonedEntities = new ArrayList<>();
    private BukkitRunnable cleanupTask;
    private BukkitRunnable aiTask;
    private final Random random = new Random();
    private final int slownessDuration = getConfigInt("slowness-duration");
    private final int slownessAmplifier = getConfigInt("slowness-amplifier");
    private final int blindnessDuration = getConfigInt("blindness-duration");
    private final int blindnessAmplifier = getConfigInt("blindness-amplifier");

    public Parasites(Player p) {
        super(p);
        this.ownerUUID = p.getUniqueId();
        Bukkit.getPluginManager().registerEvents(this, KitBattle.inst());
    }

    @Override
    public void onDestroy() {
        clearSummonedEntities();
        if (aiTask != null && !aiTask.isCancelled()) {
            aiTask.cancel();
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (event.getEntity().getUniqueId().equals(ownerUUID)) {
            // 玩家死亡，清除所有召唤物的 ownerUUID 元数据，使其脱离友方范畴
            for (Entity entity : summonedEntities) {
                if (entity != null && !entity.isDead()) {
                    entity.removeMetadata("ownerUUID", KitBattle.inst());
                }
            }
            // 清除所有召唤物
            clearSummonedEntities();
        }
    }

    public boolean castSkill() {
        Location location = p.getLocation();
        if (aiTask != null && !aiTask.isCancelled()) {
            aiTask.cancel();
        }

        // 先清除上一波召唤物，并取消旧的清除任务
        clearSummonedEntities();

        int numSummons = 7;  // 生成3到5个召唤物
        for (int i = 0; i < numSummons; i++) {
            int finalI = i;
            Bukkit.getScheduler().runTaskLater(KitBattle.inst(), () -> {
                Location playerLocation = p.getLocation(); // 获取玩家当前坐标
                Location spawnLocation = getRandomOffsetLocation(playerLocation, 2.0); // 在玩家周围2格范围内生成
                Entity bug;
                if (finalI % 2 == 0) {
                    // 召唤蠹虫
                    bug = p.getWorld().spawn(spawnLocation, Silverfish.class);
                } else {
                    // 召唤末影螨
                    bug = p.getWorld().spawn(spawnLocation, Endermite.class);
                }
                bug.setCustomNameVisible(false);
                bug.setSilent(true);
                bug.setPersistent(false);
                bug.setMetadata("ownerUUID", new FixedMetadataValue(KitBattle.inst(), ownerUUID.toString()));
                summonedEntities.add(bug);
            }, i * 3L); // 每个召唤物间隔5 ticks生成
        }

        // 取消旧的清除任务，防止上一波的定时器仍在运行
        if (cleanupTask != null && !cleanupTask.isCancelled()) {
            cleanupTask.cancel();
        }

        // 启动新的清除任务
        cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                clearSummonedEntities();
            }
        };
        cleanupTask.runTaskLater(KitBattle.inst(), 400L);  // 20秒

        // 启动 AI 任务
        if (aiTask != null && !aiTask.isCancelled()) {
            aiTask.cancel();
        }
        aiTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateAI();
            }
        };
        aiTask.runTaskTimer(KitBattle.inst(), 10L, 10L);  // 每1秒更新一次 AI
        return true;
    }

    private Location getRandomOffsetLocation(Location center, double radius) {
        // 在中心点周围生成一个随机偏移的位置
        double offsetX = (random.nextDouble() - 0.5) * 2 * radius;
        double offsetZ = (random.nextDouble() - 0.5) * 2 * radius;
        return center.clone().add(offsetX, 0, offsetZ);
    }

    private void updateAI() {
        Player owner = Bukkit.getPlayer(ownerUUID);
        if (owner == null || !owner.isOnline()) {
            // 玩家离线，清除召唤物
            clearSummonedEntities();
            return;
        }

        for (Entity entity : summonedEntities) {
            if (entity instanceof Mob) {
                Mob mob = (Mob) entity;
                if (owner.isDead()) {
                    // 玩家死亡，召唤物攻击任何附近的生物（除了友方召唤物）
                    setWildAI(mob);
                } else {
                    // 玩家存活，召唤物攻击玩家的敌人（除了友方召唤物）
                    setFriendlyAI(mob, owner);
                }
            }
        }
    }

    private void setFriendlyAI(Mob mob, Player owner) {
        // 攻击玩家的敌人（除了友方召唤物）
        for (Entity nearbyEntity : mob.getNearbyEntities(10, 10, 10)) {
            if (nearbyEntity instanceof LivingEntity && !nearbyEntity.equals(owner)) {
                // 检查是否是友方召唤物
                if (isFriendlySummon(nearbyEntity)) {
                    continue; // 跳过友方召唤物
                }

                if (nearbyEntity instanceof Player) {
                    Player targetPlayer = (Player) nearbyEntity;
                    if (KitBattle.inst().isInArena(targetPlayer)) {
                        mob.setTarget((LivingEntity) nearbyEntity);
                        break;
                    }
                } else if (nearbyEntity instanceof Monster) {
                    mob.setTarget((LivingEntity) nearbyEntity);
                    break;
                }
            }
        }
    }

    private void setWildAI(Mob mob) {
        // 攻击任何附近的生物（除了友方召唤物）
        for (Entity nearbyEntity : mob.getNearbyEntities(10, 10, 10)) {
            if (nearbyEntity instanceof LivingEntity && !nearbyEntity.equals(mob)) {
                // 检查是否是友方召唤物
                if (isFriendlySummon(nearbyEntity)) {
                    continue; // 跳过友方召唤物
                }

                mob.setTarget((LivingEntity) nearbyEntity);
                break;
            }
        }
    }

    private boolean isFriendlySummon(Entity entity) {
        // 检查实体是否是友方召唤物
        if (entity.hasMetadata("ownerUUID")) {
            String ownerUUID = entity.getMetadata("ownerUUID").get(0).asString();
            return ownerUUID.equals(this.ownerUUID.toString());
        }
        return false;
    }

    private void clearSummonedEntities() {
        // 先检查并取消清除任务
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }

        // 清除召唤物
        for (Entity entity : summonedEntities) {
            if (entity != null && !entity.isDead()) {
                entity.remove();
            }
        }
        summonedEntities.clear();
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Silverfish || event.getDamager() instanceof Endermite) {
            Entity damager = event.getDamager();
            Player victim = event.getEntity() instanceof Player ? (Player) event.getEntity() : null;

            if (victim == null) return;

            // 检查召唤物的所有者
            if (damager.hasMetadata("ownerUUID")) {
                String ownerUUID = damager.getMetadata("ownerUUID").get(0).asString();
                if (victim.getUniqueId().toString().equals(ownerUUID)) {
                    event.setCancelled(true); // 阻止召唤物攻击它的主人
                    return;
                }
            }

            // 施加负面效果
            if (damager instanceof Silverfish) {
                victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slownessDuration, slownessAmplifier)); // 5秒缓慢
            } else if (damager instanceof Endermite) {
                victim.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, blindnessDuration, blindnessAmplifier)); // 5秒黑暗
            }
        }
    }
}