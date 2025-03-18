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

        // **计算主射线方向**
        Vector direction = p.getEyeLocation().getDirection().normalize();
        Vector start = p.getEyeLocation().toVector().add(direction.clone().multiply(1.2)); // 让子弹从玩家视线前方一点点发射

        // **计算最大射程终点**
        Vector end = start.clone().add(direction.clone().multiply(MAX_DISTANCE));

        // **检测方块碰撞**
        RayTraceResult blockHit = p.getWorld().rayTraceBlocks(p.getEyeLocation(), direction, MAX_DISTANCE);
        if (blockHit != null) {
            end = blockHit.getHitPosition(); // ✅ 确保子弹停止在方块前
        }

        // **检测实体碰撞**
        RayTraceResult entityHit = p.getWorld().rayTraceEntities(
                p.getEyeLocation(),
                direction,
                MAX_DISTANCE,
                entity -> entity instanceof LivingEntity && !entity.equals(p)
        );

        if (entityHit != null && entityHit.getHitEntity() instanceof LivingEntity) {
            LivingEntity target = (LivingEntity) entityHit.getHitEntity();
            Vector entityPosition = target.getLocation().toVector().add(new Vector(0, target.getHeight() / 2, 0)); // 瞄准目标中心

            // **如果生物比方块更近，则优先命中生物**
            if (blockHit == null || start.distance(entityPosition) < start.distance(end)) {
                // **计算命中点，使其与射线方向一致**
                double hitDistance = start.distance(entityPosition);
                end = start.clone().add(direction.clone().multiply(hitDistance)); // 保持射线方向不变

                double damage = (hitDistance <= DAMAGE_DISTANCE_THRESHOLD) ? DAMAGE_CLOSE : DAMAGE_FAR;
                target.damage(damage, p);
            }
        }

        // **生成子弹轨迹**
        spawnBulletParticles(p, start, end);

        // **设置冷却 UI**
        p.setCooldown(Material.ECHO_SHARD, COOLDOWN_TICKS);
    }

    /**
     * 生成子弹粒子轨迹（始终保持视线方向，命中目标或方块时停止）
     */
    private void spawnBulletParticles(Player shooter, Vector start, Vector end) {
        Vector direction = end.clone().subtract(start).normalize().multiply(0.5); // 每 0.5 格一个粒子
        Vector current = start.clone();

        while (current.distance(end) > 0.5) { // 只要没到终点就继续生成粒子
            shooter.getWorld().spawnParticle(Particle.SMOKE, current.getX(), current.getY(), current.getZ(), 1, 0, 0, 0, 0);
            current.add(direction);
        }
    }
}
