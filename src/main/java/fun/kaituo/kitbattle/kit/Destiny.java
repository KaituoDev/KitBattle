package fun.kaituo.kitbattle.kit;

import fun.kaituo.kitbattle.KitBattle;
import fun.kaituo.kitbattle.util.PlayerData;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Destiny extends PlayerData {
    private final int RELOAD_TICKS;
    private final int SHOT_COOLDOWN;
    private final int SHOTS_PER_CLICK;
    private final double RAY_RADIUS;
    private final double RAY_DISTANCE;
    private final double CLOSE_RANGE_DAMAGE;
    private final double FAR_RANGE_DAMAGE;
    private final double CLOSE_RANGE_DISTANCE;
    private final double MAX_SHOT_DEVIATION; // 最大射击偏移角度(弧度)

    private boolean isReloading = false;
    private int reloadTaskId = -1;
    private Random random = new Random();

    public Destiny(Player p) {
        super(p);
        RELOAD_TICKS = getConfigInt("reload-ticks");
        SHOT_COOLDOWN = getConfigInt("shot-cooldown");
        SHOTS_PER_CLICK = getConfigInt("shots-per-click");
        RAY_RADIUS = getConfigDouble("ray-radius");
        RAY_DISTANCE = getConfigDouble("ray-distance");
        CLOSE_RANGE_DAMAGE = getConfigDouble("close-range-damage");
        FAR_RANGE_DAMAGE = getConfigDouble("far-range-damage");
        CLOSE_RANGE_DISTANCE = getConfigDouble("close-range-distance");
        MAX_SHOT_DEVIATION = Math.toRadians(getConfigDouble("max-shot-deviation")); // 默认最大偏移2度
    }

    @EventHandler
    public void onPlayerShoot(PlayerInteractEvent e) {
        if (!e.getPlayer().getUniqueId().equals(playerId)) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = e.getItem();
        if (item == null || item.getType() != Material.WOLF_ARMOR) return;

        if (isReloading) {
            e.setCancelled(true);
            return;
        }

        // 检查弹药是否为空
        int ammoSlot = 1;
        ItemStack ammo = p.getInventory().getItem(ammoSlot);
        if (ammo == null || ammo.getType() != Material.IRON_NUGGET || ammo.getAmount() <= 0) {
            e.setCancelled(true);
            startReload();
            return;
        }

        // 执行4次独立发射
        for (int i = 0; i < SHOTS_PER_CLICK; i++) {
            Bukkit.getScheduler().runTaskLater(KitBattle.inst(), () -> {
                if (p == null || !p.isOnline() || isReloading) return;

                // 每次发射前检查弹药
                ItemStack currentAmmo = p.getInventory().getItem(ammoSlot);
                if (currentAmmo == null || currentAmmo.getType() != Material.IRON_NUGGET || currentAmmo.getAmount() <= 0) {
                    startReload();
                    return;
                }

                // 消耗弹药
                currentAmmo.setAmount(currentAmmo.getAmount() - 1);
                p.getInventory().setItem(ammoSlot, currentAmmo);

                // 获取实时视角并添加随机偏移后发射
                Location eyeLoc = p.getEyeLocation();
                Vector direction = eyeLoc.getDirection().normalize();
                direction = applyRandomDeviation(direction); // 应用随机偏移
                fireRay(eyeLoc, direction);
            }, i); // 每次间隔1tick
        }

        p.setCooldown(Material.WOLF_ARMOR, SHOT_COOLDOWN);
    }

    private Vector applyRandomDeviation(Vector originalDirection) {
        // 生成随机偏移角度
        double deviationAngle = random.nextDouble() * MAX_SHOT_DEVIATION;
        double rotationAngle = random.nextDouble() * 2 * Math.PI; // 随机旋转方向

        // 创建一个垂直于原始方向的向量作为旋转轴
        Vector perpendicular = originalDirection.clone().crossProduct(new Vector(0, 1, 0));
        if (perpendicular.lengthSquared() < 0.0001) {
            perpendicular = originalDirection.clone().crossProduct(new Vector(1, 0, 0));
        }
        perpendicular.normalize();

        // 应用旋转
        return originalDirection.clone().rotateAroundAxis(perpendicular, deviationAngle)
                .rotateAroundAxis(originalDirection, rotationAngle);
    }

    private void fireRay(Location startLoc, Vector direction) {
        RayTraceResult result = p.getWorld().rayTrace(
                startLoc,
                direction,
                RAY_DISTANCE,
                FluidCollisionMode.NEVER,
                true,
                RAY_RADIUS,
                entity -> entity != p && entity instanceof LivingEntity
        );

        Location endPoint;
        if (result != null) {
            if (result.getHitEntity() != null) {
                // 使用射线实际命中的位置，而不是实体的脚底位置
                endPoint = result.getHitPosition().toLocation(p.getWorld());
                handleHit(startLoc, endPoint, (LivingEntity) result.getHitEntity());
            } else {
                endPoint = result.getHitPosition().toLocation(p.getWorld());
            }
        } else {
            endPoint = startLoc.clone().add(direction.multiply(RAY_DISTANCE));
        }

        displayRay(startLoc, endPoint);
        p.playSound(startLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.4f, 1.8f);
    }

    private void handleHit(Location startLoc, Location hitLoc, LivingEntity target) {
        double distance = startLoc.distance(hitLoc);
        double damage = distance <= CLOSE_RANGE_DISTANCE ? CLOSE_RANGE_DAMAGE : FAR_RANGE_DAMAGE;

        target.damage(damage, p);
    }

    private void displayRay(Location start, Location end) {
        Vector path = end.toVector().subtract(start.toVector());
        double length = path.length();
        path.normalize();

        // 更细更小的子弹特效
        for (double d = 0; d < length; d += 0.3) {
            Location point = start.clone().add(path.clone().multiply(d));
            p.getWorld().spawnParticle(
                    Particle.ELECTRIC_SPARK,
                    point,
                    1,
                    0, 0, 0,
                    0.01
            );
        }
    }
    private void startReload() {
        if (isReloading) return;

        isReloading = true;
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§e§l装弹中..."));
        p.playSound(p.getLocation(), Sound.BLOCK_PISTON_EXTEND, 1.0f, 1.5f);

        if (reloadTaskId != -1) {
            Bukkit.getScheduler().cancelTask(reloadTaskId);
        }

        reloadTaskId = Bukkit.getScheduler().scheduleSyncDelayedTask(KitBattle.inst(), () -> {
            p.getInventory().setItem(1, new ItemStack(Material.IRON_NUGGET, 30));
            p.updateInventory();
            isReloading = false;
            reloadTaskId = -1;

            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§a§l装弹完成！"));
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
        }, RELOAD_TICKS);
    }

    @Override
    public void onDestroy() {
        if (reloadTaskId != -1) {
            Bukkit.getScheduler().cancelTask(reloadTaskId);
        }
        super.onDestroy();
    }
}