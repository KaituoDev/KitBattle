package fun.kaituo.kitbattle.kit;

import fun.kaituo.kitbattle.util.PlayerData;
import org.bukkit.Material;
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
    private static final double DAMAGE_CLOSE = 12.0; // 20 格以内的伤害
    private static final double DAMAGE_FAR = 8.0;   // 20 格外的伤害
    private static final int MAX_DISTANCE = 50;     // 最大检测距离
    private static final int DAMAGE_DISTANCE_THRESHOLD = 20; // 伤害衰减阈值
    private static final int COOLDOWN_TICKS = 60;   // 末影珍珠的冷却时间（3秒）

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

        // 检测物品是否是火枪
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta() || !MUSKET_NAME.equals(item.getItemMeta().getDisplayName())) {
            return;
        }

        // 检查物品冷却
        if (p.hasCooldown(Material.ECHO_SHARD)) {

            return;
        }

        // 获取玩家眼睛位置，并稍微向前偏移
        Vector direction = p.getEyeLocation().getDirection();
        Vector offset = direction.clone().multiply(1.5); // 让射线起点稍微向前移动 1.5 格，避免检测到自己
        RayTraceResult result = p.getWorld().rayTraceEntities(
                p.getEyeLocation().add(offset), // 调整射线起点，防止命中自己
                direction, // 视线方向
                MAX_DISTANCE,
                entity -> entity instanceof LivingEntity && !entity.equals(p) // 过滤掉自己
        );

        if (result != null && result.getHitEntity() instanceof LivingEntity) {
            LivingEntity target = (LivingEntity) result.getHitEntity();
            double distance = p.getLocation().distance(target.getLocation());

            double damage = (distance <= DAMAGE_DISTANCE_THRESHOLD) ? DAMAGE_CLOSE : DAMAGE_FAR;
            target.damage(damage, p); // 造成伤害

        }

        // 设置冷却时间
        p.setCooldown(Material.ECHO_SHARD, COOLDOWN_TICKS);
    }
}
