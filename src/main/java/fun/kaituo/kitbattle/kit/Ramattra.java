package fun.kaituo.kitbattle.kit;

import fun.kaituo.kitbattle.KitBattle;
import fun.kaituo.kitbattle.util.PlayerData;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Ramattra extends PlayerData {

    private boolean isHeavenlyMode = false;
    private Map<Integer, ItemStack> originalInventory = new HashMap<>();
    private int heavenlyTaskId = -1;

    // 拳风的冷却时间（单独设置）
    private long punchCooldownTicks = 0;
    private final long maxPunchCooldownTicks = 12; // 1秒冷却

    // 记录已经受到伤害的敌人
    private final Set<UUID> damagedEntities = new HashSet<>();

    // 记录拳风的偏移方向（true 表示向右，false 表示向左）
    private boolean punchOffsetDirection = true;

    public Ramattra(Player p) {
        super(p);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isHeavenlyMode) {
            exitHeavenlyMode();
        }
    }

    @Override
    public boolean castSkill() {
        if (isHeavenlyMode) {
            p.sendMessage(ChatColor.RED + "你已经在天罚形态中！");
            return false;
        }

        enterHeavenlyMode();
        return true;
    }

    private void enterHeavenlyMode() {
        isHeavenlyMode = true;

        // 记录背包并清空（保留盔甲）
        PlayerInventory inventory = p.getInventory();
        for (int i = 0; i < inventory.getSize(); i++) {
            if (i >= 36 && i <= 39) continue; // 跳过盔甲槽位
            originalInventory.put(i, inventory.getItem(i));
            inventory.setItem(i, null);
        }

        // 添加视觉效果和抗性提升效果
        p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 300, 0, false, false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 300, 3, false, false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 300, 2, false, false));
        p.sendMessage(ChatColor.DARK_PURPLE + "惩罚他们！");

        // 设置15秒后退出天罚形态
        heavenlyTaskId = Bukkit.getScheduler().scheduleSyncDelayedTask(KitBattle.inst(), this::exitHeavenlyMode, 300);
    }

    private void exitHeavenlyMode() {
        if (!isHeavenlyMode) return;

        isHeavenlyMode = false;

        // 归还背包物品（保留盔甲）
        PlayerInventory inventory = p.getInventory();
        for (Map.Entry<Integer, ItemStack> entry : originalInventory.entrySet()) {
            inventory.setItem(entry.getKey(), entry.getValue());
        }
        originalInventory.clear();

        // 移除抗性提升效果
        p.removePotionEffect(PotionEffectType.RESISTANCE);
        p.removePotionEffect(PotionEffectType.STRENGTH);
        p.removePotionEffect(PotionEffectType.ABSORPTION);

        // 重置天罚形态的冷却时间
        coolDownTicks = maxCoolDownTicks;

        // 取消任务
        if (heavenlyTaskId != -1) {
            Bukkit.getScheduler().cancelTask(heavenlyTaskId);
            heavenlyTaskId = -1;
        }
    }

    @Override
    public void tick() {
        super.tick(); // 调用父类的 tick 方法处理天罚形态的冷却时间

        // 处理拳风的冷却时间
        if (punchCooldownTicks > 0) {
            punchCooldownTicks--;
        }
    }

    @EventHandler
    public void onPlayerLeftClick(PlayerInteractEvent e) {
        if (!isHeavenlyMode || !e.getPlayer().equals(p) || !e.getAction().equals(Action.LEFT_CLICK_AIR)) {
            return;
        }

        // 如果拳风处于冷却时间，则不发射拳风
        if (punchCooldownTicks > 0) {
            return;
        }

        // 发射拳风
        launchPunchWind();
    }

    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent e) {
        if (!isHeavenlyMode || !(e.getDamager() instanceof Player) || !e.getDamager().equals(p)) {
            return;
        }

        // 如果拳风处于冷却时间，则不发射拳风
        if (punchCooldownTicks > 0) {
            return;
        }

        // 发射拳风
        launchPunchWind();
    }

    private void launchPunchWind() {
        // 清空已受伤害的实体记录
        damagedEntities.clear();

        // 计算偏移方向
        Vector offset = calculatePunchOffset();

        // 发射拳风
        Vector direction = p.getEyeLocation().getDirection().normalize();
        Location start = p.getEyeLocation().add(direction.multiply(2)).add(offset); // 应用偏移
        new BukkitRunnable() {
            double distance = 0;
            final double maxDistance = 3; // 拳风范围
            final double step = 1.3;

            @Override
            public void run() {
                if (distance >= maxDistance) {
                    this.cancel();
                    return;
                }

                Location current = start.clone().add(direction.clone().multiply(distance));

                // 生成更大的粒子特效
                for (double x = -0.25; x <= 0.25; x += 0.25) {
                    for (double y = -0.25; y <= 0.25; y += 0.25) {
                        for (double z = -0.25; z <= 0.25; z += 0.25) {
                            Location particleLoc = current.clone().add(x, y, z);
                            p.getWorld().spawnParticle(Particle.CLOUD, particleLoc, 1, 0, 0, 0, 0);
                        }
                    }
                }

                // 检测命中所有实体（LivingEntity）
                for (LivingEntity entity : p.getWorld().getLivingEntities()) {
                    if (entity.equals(p)) continue; // 跳过自己

                    // 检查实体是否已经受到过伤害
                    if (damagedEntities.contains(entity.getUniqueId())) {
                        continue; // 如果已经受到过伤害，则跳过
                    }

                    // 检查实体是否在命中范围内
                    if (entity.getLocation().distanceSquared(current) < 4) { // 命中范围
                        entity.damage(10, p);
                        damagedEntities.add(entity.getUniqueId()); // 记录已受伤害的实体
                    }
                }

                distance += step;
            }
        }.runTaskTimer(KitBattle.inst(), 0, 1);

        // 进入拳风冷却时间
        punchCooldownTicks = maxPunchCooldownTicks;

        // 切换偏移方向
        punchOffsetDirection = !punchOffsetDirection;
    }

    /**
     * 计算拳风的偏移方向
     *
     * @return 偏移向量
     */
    private Vector calculatePunchOffset() {
        // 获取玩家的视线方向
        Vector direction = p.getEyeLocation().getDirection().normalize();

        // 计算垂直于视线方向的偏移向量
        Vector offset = new Vector(-direction.getZ(), 0, direction.getX()).normalize();

        // 根据当前偏移方向决定向左还是向右
        if (punchOffsetDirection) {
            return offset.multiply(0.5); // 向右偏移0.3格
        } else {
            return offset.multiply(-0.5); // 向左偏移0.3格
        }
    }
}