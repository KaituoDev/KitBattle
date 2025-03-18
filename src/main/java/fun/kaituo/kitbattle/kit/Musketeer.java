package fun.kaituo.kitbattle.kit;

import fun.kaituo.kitbattle.KitBattle;
import fun.kaituo.kitbattle.util.PlayerData;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class Musketeer extends PlayerData {
    private static final String MUSKET_NAME = "火枪";
    private static final double DAMAGE_CLOSE = 14.0; // 20 格以内的伤害
    private static final double DAMAGE_FAR = 8.0;   // 20 格外的伤害
    private static final int MAX_DISTANCE = 200;     // 最大检测距离
    private static final int DAMAGE_DISTANCE_THRESHOLD = 30; // 伤害衰减阈值
    private static final int COOLDOWN_TICKS = 50;
    private static final double SIDE_OFFSET = 0.2;  // 平行射线的左右偏移量

    public Musketeer(Player p) {
        super(p);
    }

    @EventHandler
    public void onUseMusketeerGun(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        if (!p.equals(p.getPlayer())) return; // 确保是该职业的玩家

        // 只监听右键（空中或方块）
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // 获取物品
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta() || !MUSKET_NAME.equals(item.getItemMeta().getDisplayName())) {
            return;
        }

        // 检查物品冷却
        if (p.hasCooldown(Material.ECHO_SHARD)) {
            return;
        }

        // **播放爆炸音效**
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);

        // 计算主射线方向
        Vector direction = p.getEyeLocation().getDirection();
        Vector start = p.getEyeLocation().toVector().add(direction.clone().multiply(1.5)); // 让子弹从玩家视线前方一点点发射

        // 计算左右射线偏移
        Vector sideDirection = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize().multiply(SIDE_OFFSET);
        Vector leftStart = start.clone().add(sideDirection);
        Vector rightStart = start.clone().subtract(sideDirection);

        // 计算最大射程终点
        Vector end = start.clone().add(direction.clone().multiply(MAX_DISTANCE));
        Vector leftEnd = leftStart.clone().add(direction.clone().multiply(MAX_DISTANCE));
        Vector rightEnd = rightStart.clone().add(direction.clone().multiply(MAX_DISTANCE));

        // **检测方块碰撞**
        RayTraceResult blockHit = p.getWorld().rayTraceBlocks(p.getEyeLocation(), direction, MAX_DISTANCE);
        if (blockHit != null) {
            end = blockHit.getHitPosition(); // ✅ 正确使用 `getHitPosition()`
        }

        // 检测左右平行射线的方块碰撞
        RayTraceResult leftBlockHit = p.getWorld().rayTraceBlocks(p.getEyeLocation().add(sideDirection), direction, MAX_DISTANCE);
        if (leftBlockHit != null) {
            leftEnd = leftBlockHit.getHitPosition(); // ✅ 确保左射线不会穿墙
        }

        RayTraceResult rightBlockHit = p.getWorld().rayTraceBlocks(p.getEyeLocation().subtract(sideDirection), direction, MAX_DISTANCE);
        if (rightBlockHit != null) {
            rightEnd = rightBlockHit.getHitPosition(); // ✅ 确保右射线不会穿墙
        }

        // **检测实体碰撞**
        LivingEntity target = null;
        for (Vector startPoint : new Vector[]{start, leftStart, rightStart}) {
            RayTraceResult entityHit = p.getWorld().rayTraceEntities(
                    startPoint.toLocation(p.getWorld()),
                    direction,
                    MAX_DISTANCE,
                    entity -> entity instanceof LivingEntity && !entity.equals(p)
            );

            if (entityHit != null && entityHit.getHitEntity() instanceof LivingEntity) {
                LivingEntity hitTarget = (LivingEntity) entityHit.getHitEntity();
                Vector entityPosition = hitTarget.getLocation().toVector();

                if (startPoint.distance(entityPosition) < startPoint.distance(end)) {
                    target = hitTarget;
                    end = entityPosition;
                }
            }
        }

        // **如果命中生物，则造成伤害**
        if (target != null) {
            double distance = p.getLocation().distance(target.getLocation());
            double damage = (distance <= DAMAGE_DISTANCE_THRESHOLD) ? DAMAGE_CLOSE : DAMAGE_FAR;
            target.damage(damage, p);
        }

        // **生成子弹轨迹**
        spawnBulletParticles(p, start, end);

        // **设置冷却 UI**
        p.setCooldown(Material.ECHO_SHARD, COOLDOWN_TICKS);
    }

    /**
     * 生成子弹粒子轨迹（瞬间到达）
     */
    private void spawnBulletParticles(Player shooter, Vector start, Vector end) {
        Vector direction = end.clone().subtract(start).normalize().multiply(0.5); // 每 0.5 格一个粒子
        Vector current = start.clone();

        for (int i = 0; i < 100; i++) { // 限制最大 100 步，防止无限循环
            if (current.distance(end) < 0.5) {
                break; // 到达目标或最大射程
            }
            shooter.getWorld().spawnParticle(Particle.SMOKE, current.getX(), current.getY(), current.getZ(), 1, 0, 0, 0, 0);
            current.add(direction);
        }
    }
}
