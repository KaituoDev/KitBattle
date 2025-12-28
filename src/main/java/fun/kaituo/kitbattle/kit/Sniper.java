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
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class Sniper extends PlayerData {
    private static final String TELESCOPE_NAME = "狙击步枪";
    private static final double DAMAGE_IMPRECISE = 20.0; // 不精确射线的伤害
    private static final double DAMAGE_PRECISE = 35.0;  // 精确射线的伤害
    private static final int MAX_DISTANCE = 500;        // 最大检测距离
    private static final int COOLDOWN_TICKS = 120;       // 冷却时间
    private static final double IMPRECISE_SPREAD = 0.2; // 不精确射线的扩散范围
    private static final double SIDE_OFFSET = 0.2;      // 平行射线的左右偏移量

    private boolean isSniperMode = false; // 是否处于狙击模式

    public Sniper(Player p) {
        super(p);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        // 确保在销毁时移除减速效果
        p.removePotionEffect(PotionEffectType.SLOWNESS);
    }

    @EventHandler
    public void onUseTelescope(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // 确保事件只处理当前玩家的行为
        if (!player.equals(p.getPlayer())) {
            return;
        }

        // 获取物品
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta() || !TELESCOPE_NAME.equals(item.getItemMeta().getDisplayName())) {
            return;
        }

        // 检查物品冷却
        if (player.hasCooldown(Material.SPYGLASS)) {
            return;
        }

        // 左键发射不精确射线
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            shootImpreciseRay(player);
            player.setCooldown(Material.SPYGLASS, COOLDOWN_TICKS);
        }

        // 右键进入狙击模式
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            isSniperMode = true;
            player.sendMessage("§a进入狙击模式");
            // 添加缓慢 III 效果
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 2, false, false));
        }
    }

    @EventHandler
    public void onReleaseRightClick(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();

        // 确保事件只处理当前玩家的行为
        if (!player.equals(p.getPlayer())) {
            return;
        }

        // 检查是否处于狙击模式
        if (isSniperMode) {
            isSniperMode = false;
            shootPreciseRay(player);
            player.sendMessage("§c退出狙击模式");
            player.setCooldown(Material.SPYGLASS, COOLDOWN_TICKS);
            // 移除缓慢 III 效果
            player.removePotionEffect(PotionEffectType.SLOWNESS);
        }
    }

    private void shootImpreciseRay(Player player) {
        if (player.hasCooldown(Material.SPYGLASS)) {
            return;
        }

        // 计算主射线方向
        Vector direction = player.getEyeLocation().getDirection().normalize();
        Vector start = player.getEyeLocation().toVector().add(direction.clone().multiply(1.2)); // 让射线从玩家视线前方一点点发射

        // 添加随机偏移量以模拟不精确
        Vector spread = new Vector(
                (Math.random() - 0.5) * IMPRECISE_SPREAD,
                (Math.random() - 0.5) * IMPRECISE_SPREAD,
                (Math.random() - 0.5) * IMPRECISE_SPREAD
        );
        direction.add(spread).normalize();

        // 计算最大射程终点
        Vector end = start.clone().add(direction.clone().multiply(MAX_DISTANCE));

        // 检测方块碰撞
        RayTraceResult blockHit = player.getWorld().rayTraceBlocks(player.getEyeLocation(), direction, MAX_DISTANCE);
        if (blockHit != null) {
            end = blockHit.getHitPosition(); // 确保射线停止在方块前
        }

        // 检测实体碰撞
        RayTraceResult entityHit = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                direction,
                MAX_DISTANCE,
                entity -> entity instanceof LivingEntity && !entity.equals(p)
        );

        if (entityHit != null && entityHit.getHitEntity() instanceof LivingEntity) {
            LivingEntity target = (LivingEntity) entityHit.getHitEntity();
            target.damage(DAMAGE_IMPRECISE, player);
        }

        // 生成射线粒子轨迹
        spawnRayParticles(player, start, end, Particle.CRIT);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.0f);
    }

    private void shootPreciseRay(Player player) {
        if (player.hasCooldown(Material.SPYGLASS)) {
            return;
        }

        Vector direction = player.getEyeLocation().getDirection().normalize();
        Vector start = player.getEyeLocation().toVector().add(direction.clone().multiply(1.2)); // 让射线从玩家视线前方一点点发射

        // 发射三道平行射线
        shootParallelRays(player, start, direction, DAMAGE_PRECISE);

        // 播放音效
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.5f);
    }

    private void spawnRayParticles(Player shooter, Vector start, Vector end, Particle particle) {
        Vector direction = end.clone().subtract(start).normalize().multiply(0.5); // 每 0.5 格一个粒子
        Vector current = start.clone();

        while (current.distance(end) > 0.5) { // 只要没到终点就继续生成粒子
            shooter.getWorld().spawnParticle(particle, current.getX(), current.getY(), current.getZ(), 1, 0, 0, 0, 0);
            current.add(direction);
        }
    }

    /**
     * 发射三道平行射线，任意一条命中即为命中目标
     */
    private void shootParallelRays(Player player, Vector start, Vector direction, double damage) {
        // 计算左右偏移量
        Vector rightOffset = new Vector(-direction.getZ(), 0, direction.getX()).normalize().multiply(SIDE_OFFSET);
        Vector leftOffset = rightOffset.clone().multiply(-1);

        // 发射中间射线并生成粒子特效
        shootSingleRay(player, start, direction, damage);
        spawnRayParticles(player, start, start.clone().add(direction.clone().multiply(MAX_DISTANCE)), Particle.CRIT);

        // 发射右侧射线
        shootSingleRay(player, start.clone().add(rightOffset), direction, damage);

        // 发射左侧射线
        shootSingleRay(player, start.clone().add(leftOffset), direction, damage);
    }

    /**
     * 发射单条射线并检测命中
     */
    private void shootSingleRay(Player player, Vector start, Vector direction, double damage) {
        // 计算最大射程终点
        Vector end = start.clone().add(direction.clone().multiply(MAX_DISTANCE));

        // 检测方块碰撞
        RayTraceResult blockHit = player.getWorld().rayTraceBlocks(start.toLocation(player.getWorld()), direction, MAX_DISTANCE);
        if (blockHit != null) {
            end = blockHit.getHitPosition(); // 确保射线停止在方块前
        }

        // 检测实体碰撞
        RayTraceResult entityHit = player.getWorld().rayTraceEntities(
                start.toLocation(player.getWorld()),
                direction,
                MAX_DISTANCE,
                entity -> entity instanceof LivingEntity && !entity.equals(player)
        );

        if (entityHit != null && entityHit.getHitEntity() instanceof LivingEntity) {
            LivingEntity target = (LivingEntity) entityHit.getHitEntity();
            target.damage(damage, player);
        }
    }
}