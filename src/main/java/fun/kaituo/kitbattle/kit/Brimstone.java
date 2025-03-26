package fun.kaituo.kitbattle.kit;

import fun.kaituo.kitbattle.KitBattle;
import fun.kaituo.kitbattle.util.PlayerData;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Brimstone extends PlayerData {
    private final Set<UUID> activeSkills = new HashSet<>();
    private final Particle.DustOptions redDustOptions = new Particle.DustOptions(Color.RED, 2.5f);
    private final Particle.DustOptions coreDustOptions = new Particle.DustOptions(Color.fromRGB(255, 100, 100), 3.0f);
    private final int RayInterval;
    private final int RayDamage;
    private final int RayDuration;

    public Brimstone(Player p) {
        super(p);
        RayInterval = getConfigInt("interval");
        RayDamage = getConfigInt("damage");
        RayDuration = getConfigInt("duration");
    }

    @Override
    public void applyInventory() {
        super.applyInventory();
    }

    @Override
    public boolean castSkill() {
        if (activeSkills.contains(playerId)) {
            return false;
        }

        activeSkills.add(playerId);
        p.playSound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.5f, 0.7f);

        new BukkitRunnable() {
            int raysFired = 0;

            @Override
            public void run() {
                if (raysFired >= RayDuration || p == null || !p.isOnline()) {
                    activeSkills.remove(playerId);
                    cancel();
                    return;
                }

                shootRay();
                raysFired++;

                Location spawnLoc = p.getEyeLocation().add(p.getEyeLocation().getDirection().multiply(1));
                for (int i = 0; i < 3; i++) {
                    p.getWorld().spawnParticle(Particle.DUST,
                            spawnLoc,
                            25,
                            0.5, 0.5, 0.5,
                            0,
                            redDustOptions);
                }
            }
        }.runTaskTimer(KitBattle.inst(), 0, RayInterval);

        return true;
    }

    private void shootRay() {
        Location start = p.getEyeLocation();
        Vector direction = start.getDirection().normalize();
        double maxDistance = 200.0;
        double step = 0.2;
        Location hitLocation = null;
        Set<LivingEntity> hitEntities = new HashSet<>();

        // 首先检测射线路径上的第一个方块碰撞点
        for (double distance = 0; distance <= maxDistance; distance += step) {
            Location checkLoc = start.clone().add(direction.clone().multiply(distance));

            if (!checkLoc.getBlock().isPassable()) {
                hitLocation = checkLoc;
                break;
            }
        }

        // 如果没有命中任何方块，则使用最大距离的位置
        if (hitLocation == null) {
            hitLocation = start.clone().add(direction.clone().multiply(maxDistance));
        }

        // 计算实际光束长度
        double actualLength = start.distance(hitLocation);

        // 绘制精确长度的光束
        drawRay(start, hitLocation, actualLength);

        // 二次遍历检测实体，使用更精确的方法
        for (double distance = 0; distance <= actualLength; distance += step) {
            Location checkLoc = start.clone().add(direction.clone().multiply(distance));

            for (Entity entity : p.getWorld().getNearbyEntities(checkLoc, 1.0, 1.0, 1.0)) {
                if (entity instanceof LivingEntity && !entity.getUniqueId().equals(playerId)) {
                    hitEntities.add((LivingEntity) entity);
                }
            }
        }

        // 对所有命中的实体造成魔法伤害
        for (LivingEntity target : hitEntities) {
            EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(
                    p, target, EntityDamageEvent.DamageCause.MAGIC, RayDamage
            );
            Bukkit.getPluginManager().callEvent(event);

            if (!event.isCancelled()) {
                target.setLastDamageCause(event);
                target.setHealth(Math.max(0, target.getHealth() - event.getFinalDamage()));

                target.getWorld().spawnParticle(Particle.DRIPPING_LAVA,
                        target.getLocation().add(0, 1, 0),
                        10,
                        0.5, 0.5, 0.5,
                        0.1);
                target.getWorld().playSound(target.getLocation(),
                        Sound.ENTITY_ENDERMAN_HURT,
                        1.0f,
                        1.0f);
            }
        }
    }

    private void drawRay(Location start, Location end, double actualLength) {
        Vector path = end.toVector().subtract(start.toVector());
        path.normalize();

        // 精确计算粒子数量，确保覆盖整个光束长度
        int particleCount = (int) (actualLength * 3); // 每米3个粒子

        // 使用更精确的粒子分布算法
        for (int i = 0; i < particleCount; i++) {
            double ratio = (double) i / particleCount;
            double distance = ratio * actualLength;
            Location center = start.clone().add(path.clone().multiply(distance));

            // 核心光束
            p.getWorld().spawnParticle(Particle.DUST,
                    center,
                    2,
                    0.05, 0.05, 0.05,
                    0,
                    coreDustOptions);

            // 外围光束
            for (int j = 0; j < 2; j++) {
                Location offset = center.clone().add(
                        (Math.random() - 0.5) * 0.6,
                        (Math.random() - 0.5) * 0.6,
                        (Math.random() - 0.5) * 0.6
                );
                p.getWorld().spawnParticle(Particle.DUST,
                        offset,
                        1,
                        0, 0, 0,
                        0,
                        redDustOptions);
            }
        }

        // 终点爆炸效果
        p.getWorld().spawnParticle(Particle.LAVA,
                end,
                25,
                0.5, 0.5, 0.5,
                0.1);
        p.getWorld().spawnParticle(Particle.FLAME,
                end,
                35,
                0.7, 0.7, 0.7,
                0.1);
        p.getWorld().playSound(end, Sound.BLOCK_LAVA_EXTINGUISH, 1.0f, 1.2f);
    }

    @Override
    public void onDestroy() {
        activeSkills.remove(playerId);
        super.onDestroy();
    }

    @Override
    public void onQuit() {
        activeSkills.remove(playerId);
        super.onQuit();
    }
}