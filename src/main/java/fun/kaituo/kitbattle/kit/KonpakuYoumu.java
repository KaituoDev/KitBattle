package fun.kaituo.kitbattle.kit;

import fun.kaituo.kitbattle.KitBattle;
import fun.kaituo.kitbattle.util.PlayerData;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.bukkit.Bukkit.getPlayer;

public class KonpakuYoumu extends PlayerData implements Listener {
    private boolean isSkillActive = false;

    private static final double FINALE_DAMAGE = 2.5; // 技能伤害
    private static final int DURATION_TICKS = 40; // 2秒，40 ticks
    private static final int RADIUS = 3; // 圆柱体半径
    private static final int HEIGHT = 5; // 圆柱体高度
    private static final double UPWARD_FORCE = 0.15; // 向上的动能

    private BukkitRunnable skillTask;
    private Set<UUID> affectedEntities = new HashSet<>();

    public KonpakuYoumu(Player p) {
        super(p);
    }

    @Override
    public boolean castSkill() {
        Player player = p.getPlayer();
        if (player == null) return false;

        // 获取玩家视线所看的方块，最大距离为 5 格
        Location targetBlockLocation = player.getTargetBlock(null, 5).getLocation();
        if (targetBlockLocation == null) {
            return false;
        }

        // 检查目标方块是否为空气
        Material targetBlockType = targetBlockLocation.getBlock().getType();
        if (targetBlockType == Material.AIR) {
            return false;
        }

        // 检查目标方块是否在 5 格内
        double distance = player.getLocation().distance(targetBlockLocation);
        if (distance > 5) {
            return false;
        }

        // 获取玩家视线所看的方块
        Location targetBlock = player.getTargetBlock(null, 20).getLocation();
        if (targetBlock == null) return false;

        // 获取圆柱体内的所有LivingEntity
        Set<LivingEntity> entities = getEntitiesInCylinder(targetBlock, RADIUS, HEIGHT);
        for (LivingEntity enemy : entities) {
            if (enemy.isDead() || !enemy.isValid()) continue;
            enemy.teleport(enemy.getLocation().add(0, 3, 0));
        }

        // 进入旁观者模式并传送到圆心上方10格处
        player.setGameMode(GameMode.SPECTATOR);
        Location spectatorLocation = targetBlock.clone().add(0, 10, 0);
        player.teleport(spectatorLocation);
        isSkillActive = true;
        Location effectCenter = targetBlockLocation.clone().add(0, 9, 0);
        // 开始技能效果
        skillTask = new BukkitRunnable() {


            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= DURATION_TICKS || player == null || !player.isOnline()) {
                    endSkill(player, targetBlock);
                    this.cancel();
                    return;
                }

                if (ticks % 4 == 0) {
                    generateParticleEffect(effectCenter);
                }
                generateCherryBlossomParticles(effectCenter);

                    for (LivingEntity entity : entities) {
                        if (entity.isDead() || !entity.isValid()) continue;


                        // 每6 tick造成伤害并给予向上的动能
                        if (ticks % 3 == 0) {
                            entity.damage(FINALE_DAMAGE, player);
                            entity.setVelocity(new Vector(0, UPWARD_FORCE, 0));
                        }

                        // 给予虚弱效果
                        entity.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, DURATION_TICKS - ticks, 10));
                        affectedEntities.add(entity.getUniqueId());
                    }

                    ticks++;

            }
        };

        skillTask.runTaskTimer(KitBattle.inst(), 0, 1);
        return true;
    }

    private Set<LivingEntity> getEntitiesInCylinder(Location center, int radius, int height) {
        Set<LivingEntity> entities = new HashSet<>();
        World world = center.getWorld();
        if (world == null) return entities;

        for (Entity entity : world.getNearbyEntities(center, radius, height, radius)) {
            if (entity instanceof LivingEntity && !entity.equals(p.getPlayer())) {
                entities.add((LivingEntity) entity);
            }
        }

        return entities;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (isSkillActive && affectedEntities.contains(event.getEntity().getUniqueId())) {
            // 取消无敌帧
            event.setCancelled(false); // 确保事件不会被取消
            ((LivingEntity) event.getEntity()).setNoDamageTicks(0); // 取消无敌帧
        }
    }

    public void endSkill(Player player, Location targetBlock) {
        if (targetBlock == null || targetBlock.getWorld() == null) {
            player.sendMessage(ChatColor.RED + "技能释放失败：无效的目标位置！");
            return;
        }

        // 确保玩家传送到目标方块的上方
        Location targetLocation = targetBlock.clone().add(0, 1, 0); // 传送到目标方块上方1格
        if (targetLocation.getWorld() == null) {
            player.sendMessage(ChatColor.RED + "技能释放失败：无效的传送位置！");
            return;
        }

        // 恢复玩家的游戏模式为冒险模式
        player.setGameMode(GameMode.ADVENTURE);

        // 传送玩家到目标方块上方
        player.teleport(targetLocation);

        // 移除所有受影响实体的虚弱效果
        for (UUID entityId : affectedEntities) {
            Entity entity = Bukkit.getEntity(entityId);
            if (entity instanceof LivingEntity) {
                ((LivingEntity) entity).removePotionEffect(PotionEffectType.WEAKNESS);
            }
        }

        affectedEntities.clear();
    }

    @Override
    public void onDestroy() {
        Player player = p.getPlayer();
        if (player != null && player.isOnline()) {
            // 确保玩家位置有效
            Location location = player.getLocation();
            if (location.getWorld() != null) {
                endSkill(player, location);
            }
        }
        super.onDestroy();
    }
    /**
     * 生成粒子效果
     *
     * @param center 粒子效果的中心点
     */
    private void generateParticleEffect(Location center) {
        World world = center.getWorld();
        if (world == null) return;

        double radius = 9; // 球体半径
        Random random = new Random();

        // 随机生成一个点
        double theta = random.nextDouble() * Math.PI; // 极角 [0, π]
        double phi = random.nextDouble() * 2 * Math.PI; // 方位角 [0, 2π]

        // 计算第一个端点的坐标
        double x1 = radius * Math.sin(theta) * Math.cos(phi);
        double y1 = radius * Math.cos(theta);
        double z1 = radius * Math.sin(theta) * Math.sin(phi);

        // 计算第二个端点的坐标（直径的另一端）
        double x2 = -x1;
        double y2 = -y1;
        double z2 = -z1;

        // 计算连线与水平面的夹角
        double deltaY = Math.abs(y2 - y1); // 垂直高度差
        double horizontalDistance = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(z2 - z1, 2)); // 水平距离
        double angleWithHorizontal = Math.toDegrees(Math.atan2(deltaY, horizontalDistance)); // 连线与水平面的夹角

        // 如果连线与水平面的夹角超过 60 度，则重新生成
        if (angleWithHorizontal > 60) {
            generateParticleEffect(center); // 递归调用，直到找到满足条件的直径
            return;
        }

        // 从一端向另一端生成粒子
        for (double t = 0; t <= 1; t += 0.05) {
            double x = x1 + t * (x2 - x1);
            double y = y1 + t * (y2 - y1);
            double z = z1 + t * (z2 - z1);
            Location particleLocation = center.clone().add(x, y, z);

            // 生成白色粒子（末地烛粒子）
            world.spawnParticle(Particle.FIREWORK, particleLocation, 1, 0, 0, 0, 0);
        }
    }
    /**
     * 生成樱花粒子特效
     *
     * @param center 粒子效果的中心点
     */
    private void generateCherryBlossomParticles(Location center) {
        World world = center.getWorld();
        if (world == null) return;

        double radius = 5; // 球体半径
        Random random = new Random();

        // 每 tick 生成 5 个樱花粒子
        for (int i = 0; i < 5; i++) {
            // 随机生成一个点（整个球体表面）
            double theta = random.nextDouble() * Math.PI; // 极角 [0, π]
            double phi = random.nextDouble() * 2 * Math.PI; // 方位角 [0, 2π]

            // 计算点的坐标
            double x = radius * Math.sin(theta) * Math.cos(phi);
            double y = radius * Math.cos(theta);
            double z = radius * Math.sin(theta) * Math.sin(phi);

            // 生成樱花粒子
            Location particleLocation = center.clone().add(x, y, z);
            world.spawnParticle(Particle.CHERRY_LEAVES, particleLocation, 2, 0, 0, 0, 0);
        }
    }

}