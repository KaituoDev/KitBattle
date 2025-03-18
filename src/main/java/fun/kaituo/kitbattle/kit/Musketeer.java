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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class Musketeer extends PlayerData {
    private static final String MUSKET_NAME = "火枪";
    private static final double DAMAGE_CLOSE = 14.0; // 20 格以内的伤害
    private static final double DAMAGE_FAR = 8.0;   // 20 格外的伤害
    private static final int MAX_DISTANCE = 200;     // 最大检测距离
    private static final int DAMAGE_DISTANCE_THRESHOLD = 20; // 伤害衰减阈值
    private static final int COOLDOWN_TICKS = 60;   // 3 秒冷却

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

        // 计算射线路径
        Vector direction = p.getEyeLocation().getDirection();
        Vector start = p.getEyeLocation().toVector().add(direction.clone().multiply(1.5)); // 偏移 1.5 格，避免命中自己
        Vector end = start.clone().add(direction.clone().multiply(MAX_DISTANCE)); // 计算最大射程终点

        RayTraceResult result = p.getWorld().rayTraceEntities(
                p.getEyeLocation().add(direction.clone().multiply(1.5)),
                direction,
                MAX_DISTANCE,
                entity -> entity instanceof LivingEntity && !entity.equals(p)
        );

        // **如果命中目标，修改终点**
        if (result != null && result.getHitEntity() instanceof LivingEntity) {
            LivingEntity target = (LivingEntity) result.getHitEntity();
            double distance = p.getLocation().distance(target.getLocation());
            double damage = (distance <= DAMAGE_DISTANCE_THRESHOLD) ? DAMAGE_CLOSE : DAMAGE_FAR;

            target.damage(damage, p);

            end = target.getLocation().toVector(); // 让粒子轨迹停止在目标位置
        }

        // **生成子弹轨迹**
        spawnBulletParticles(p, start, end);

        // **设置冷却 UI**
        p.setCooldown(Material.ECHO_SHARD, COOLDOWN_TICKS);
    }

    /**
     * 生成子弹粒子轨迹（始终生成）
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
