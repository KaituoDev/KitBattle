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
        Player p = event.getPlayer();

        // 确保事件只处理当前玩家的行为
        if (!p.equals(p.getPlayer())) {
            return;
        }

        // 获取物品
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta() || !TELESCOPE_NAME.equals(item.getItemMeta().getDisplayName())) {
            return;
        }

        // 检查物品冷却
        if (p.hasCooldown(Material.SPYGLASS)) {
            return;
        }

        // 左键发射不精确射线
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            shootImpreciseRay(p);
            p.setCooldown(Material.SPYGLASS, COOLDOWN_TICKS);
        }

        // 右键进入狙击模式
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            isSniperMode = true;
            p.sendMessage("§a进入狙击模式");
            // 添加缓慢 III 效果
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 2, false, false));
        }
    }

    @EventHandler
    public void onReleaseRightClick(PlayerItemHeldEvent event) {
        Player p = event.getPlayer();

        // 确保事件只处理当前玩家的行为
        if (!p.equals(p.getPlayer())) {
            return;
        }

        // 检查是否处于狙击模式
        if (isSniperMode) {
            isSniperMode = false;
            shootPreciseRay(p);
            p.sendMessage("§c退出狙击模式");
            p.setCooldown(Material.SPYGLASS, COOLDOWN_TICKS);
            // 移除缓慢 III 效果
            p.removePotionEffect(PotionEffectType.SLOWNESS);
        }
    }

    private void shootImpreciseRay(Player p) {
        // 计算主射线方向
        Vector direction = p.getEyeLocation().getDirection().normalize();
        Vector start = p.getEyeLocation().toVector().add(direction.clone().multiply(1.2)); // 让射线从玩家视线前方一点点发射

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
        RayTraceResult blockHit = p.getWorld().rayTraceBlocks(p.getEyeLocation(), direction, MAX_DISTANCE);
        if (blockHit != null) {
            end = blockHit.getHitPosition(); // 确保射线停止在方块前
        }

        // 检测实体碰撞
        RayTraceResult entityHit = p.getWorld().rayTraceEntities(
                p.getEyeLocation(),
                direction,
                MAX_DISTANCE,
                entity -> entity instanceof LivingEntity && !entity.equals(p)
        );

        if (entityHit != null && entityHit.getHitEntity() instanceof LivingEntity) {
            LivingEntity target = (LivingEntity) entityHit.getHitEntity();
            target.damage(DAMAGE_IMPRECISE, p);
        }

        // 生成射线粒子轨迹
        spawnRayParticles(p, start, end, Particle.CRIT);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.0f);
    }

    private void shootPreciseRay(Player p) {
        Vector direction = p.getEyeLocation().getDirection().normalize();
        Vector start = p.getEyeLocation().toVector().add(direction.clone().multiply(1.2)); // 让射线从玩家视线前方一点点发射

        // 发射三道平行射线
        shootParallelRays(p, start, direction, DAMAGE_PRECISE);

        // 播放音效
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.5f);
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
    private void shootParallelRays(Player p, Vector start, Vector direction, double damage) {
        // 计算左右偏移量
        Vector rightOffset = new Vector(-direction.getZ(), 0, direction.getX()).normalize().multiply(SIDE_OFFSET);
        Vector leftOffset = rightOffset.clone().multiply(-1);

        // 发射中间射线并生成粒子特效
        shootSingleRay(p, start, direction, damage);
        spawnRayParticles(p, start, start.clone().add(direction.clone().multiply(MAX_DISTANCE)), Particle.CRIT);

        // 发射右侧射线
        shootSingleRay(p, start.clone().add(rightOffset), direction, damage);

        // 发射左侧射线
        shootSingleRay(p, start.clone().add(leftOffset), direction, damage);
    }

    /**
     * 发射单条射线并检测命中
     */
    private void shootSingleRay(Player p, Vector start, Vector direction, double damage) {
        // 计算最大射程终点
        Vector end = start.clone().add(direction.clone().multiply(MAX_DISTANCE));

        // 检测方块碰撞
        RayTraceResult blockHit = p.getWorld().rayTraceBlocks(start.toLocation(p.getWorld()), direction, MAX_DISTANCE);
        if (blockHit != null) {
            end = blockHit.getHitPosition(); // 确保射线停止在方块前
        }

        // 检测实体碰撞
        RayTraceResult entityHit = p.getWorld().rayTraceEntities(
                start.toLocation(p.getWorld()),
                direction,
                MAX_DISTANCE,
                entity -> entity instanceof LivingEntity && !entity.equals(p)
        );

        if (entityHit != null && entityHit.getHitEntity() instanceof LivingEntity) {
            LivingEntity target = (LivingEntity) entityHit.getHitEntity();
            target.damage(damage, p);
        }
    }
}