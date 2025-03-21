package fun.kaituo.kitbattle.kit;

import fun.kaituo.kitbattle.KitBattle;
import fun.kaituo.kitbattle.util.PlayerData;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class KonpakuYoumu extends PlayerData implements Listener {
    private boolean isSkillActive = false;
    private static final Map<UUID, Long> cooldowns = new HashMap<>(); // 存储玩家冷却时间的Map
    private Map<UUID, BukkitRunnable> immuneTasks = new HashMap<>(); // 存储免疫状态的任务
    private Map<UUID, BukkitRunnable> particleTasks = new HashMap<>(); // 存储粒子效果任务的Map


    private static final double FINALE_DAMAGE = 4.0; // 技能伤害
    private static final int DURATION_TICKS = 40; // 2秒，40 ticks
    private static final int RADIUS = 3; // 圆柱体半径
    private static final int HEIGHT = 5; // 圆柱体高度
    private static final double UPWARD_FORCE = 0.15; // 向上的动能

    private static final int COOL_DOWN_TICKS = 240;

    private BukkitRunnable skillTask;
    private Set<UUID> affectedEntities = new HashSet<>();

    private Map<UUID, Long> immunePlayers = new HashMap<>(); // 存储免疫玩家的Map

    public KonpakuYoumu(Player p) {

        super(p);
        KitBattle.inst().getServer().getPluginManager().registerEvents(this, KitBattle.inst());
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
        stopParticleEffect(player);
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
        if (angleWithHorizontal > 45) {
            generateParticleEffect(center); // 递归调用，直到找到满足条件的直径
            return;
        }

        // 随机生成一个偏移向量（长度 0 到 1 格）
        double offsetX = (random.nextDouble() - 0.5) * 2; // [-1, 1]
        double offsetY = (random.nextDouble() - 0.5) * 2; // [-1, 1]
        double offsetZ = (random.nextDouble() - 0.5) * 2; // [-1, 1]
        double offsetLength = Math.sqrt(offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ);
        if (offsetLength > 1) {
            // 如果偏移向量的长度大于 1，则归一化
            offsetX /= offsetLength;
            offsetY /= offsetLength;
            offsetZ /= offsetLength;
        }

        // 从一端向另一端生成粒子，并沿偏移向量移动
        for (double t = 0; t <= 1; t += 0.05) {
            double x = x1 + t * (x2 - x1);
            double y = y1 + t * (y2 - y1);
            double z = z1 + t * (z2 - z1);

            // 应用偏移向量
            x += offsetX;
            y += offsetY;
            z += offsetZ;

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

        double radius = 7; // 球体半径
        Random random = new Random();

        // 每 tick 生成 1 个樱花粒子
        for (int i = 0; i < 3; i++) {
            // 随机生成一个点（整个球体表面）
            double theta = random.nextDouble() * Math.PI; // 极角 [0, π]
            double phi = random.nextDouble() * 2 * Math.PI; // 方位角 [0, 2π]

            // 计算点的坐标
            double x = radius * Math.sin(theta) * Math.cos(phi);
            double y = radius * Math.cos(theta);
            double z = radius * Math.sin(theta) * Math.sin(phi);

            // 生成樱花粒子
            Location particleLocation = center.clone().add(x, y, z);
            world.spawnParticle(Particle.CHERRY_LEAVES, particleLocation, 1, 0, 0, 0, 0);
        }
    }

    @EventHandler
    public void Q(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // 检查玩家手持的物品是否为樱花簇且包含“剑技”字符
        if (item != null && item.getType() == Material.PINK_PETALS) { // 确保物品类型正确
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.getDisplayName().contains("剑技")) {
                // 检查冷却时间
                if (player.hasCooldown(Material.PINK_PETALS)) {
                    return; // 如果处于冷却时间，则直接返回
                }

                // 获取玩家视线方向
                Vector direction = player.getEyeLocation().getDirection().normalize();

                // 计算射线末端位置
                Location start = player.getEyeLocation();
                Location end = start.clone().add(direction.multiply(10));

                // 检查射线与水平面的夹角
                double angleWithHorizontal = Math.toDegrees(Math.atan2(direction.getY(), Math.sqrt(direction.getX() * direction.getX() + direction.getZ() * direction.getZ())));
                if (Math.abs(angleWithHorizontal) > 30) {
                    return; // 夹角大于30度，不执行任何操作
                }

                // 检查射线是否触及方块
                Block hitBlock = player.getWorld().rayTraceBlocks(start, direction, 10, FluidCollisionMode.NEVER, true) != null ? player.getWorld().rayTraceBlocks(start, direction, 10, FluidCollisionMode.NEVER, true).getHitBlock() : null;
                if (hitBlock == null) {
                    // 没有触及方块，传送玩家到射线末端
                    player.teleport(end);
                } else {
                    // 触及方块，传送玩家到第一个触及的方块向射线起点一格距离的位置
                    Location hitLocation = hitBlock.getLocation().add(direction.multiply(-0.1));

                    // 检查 hitLocation 是否超过了起点位置
                    if (hitLocation.distanceSquared(start) > start.distanceSquared(hitLocation)) {
                        // 如果超过了起点位置，则传送玩家回起点
                        player.teleport(start);
                    } else {
                        // 否则，传送玩家到 hitLocation
                        player.teleport(hitLocation);
                    }
                }
                // 对路径上的所有实体造成伤害
                double damage = 12; // 伤害值
                double radius = 1.5; // 伤害半径
                double stepSize = 0.1; // 每步检测的间隔距离

                // 沿着路径逐段检测实体
                Location currentLocation = start.clone();
                while (currentLocation.distance(end) > stepSize) {
                    // 移动到下一个检测点
                    currentLocation.add(direction.clone().multiply(stepSize));

                    // 生成樱花粒子
                    player.getWorld().spawnParticle(Particle.CHERRY_LEAVES, currentLocation, 3, 1, 1, 1, 0.5);
                    // 检测当前点的实体
                    for (Entity entity : player.getWorld().getNearbyEntities(currentLocation, radius, radius, radius)) {
                        if (entity instanceof LivingEntity && !entity.equals(player)) {
                            ((LivingEntity) entity).damage(damage, player);
                        }
                    }
                }


                player.setCooldown(Material.PINK_PETALS, COOL_DOWN_TICKS);
            }
        }
    }

    // 启动粒子效果任务
    @EventHandler
    public void W(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // 检查玩家手持的物品是否为品红色染料且包含“空观剑”词条
        if (item != null && item.getType() == Material.PINK_DYE) { // 确保物品类型正确
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.getDisplayName().contains("空观剑")) {
                // 检查冷却时间
                if (player.hasCooldown(Material.PINK_DYE)) {
                    return; // 如果处于冷却时间，则直接返回
                }

                // 设置玩家免疫状态
                immunePlayers.put(player.getUniqueId(), System.currentTimeMillis() + 1500); // 1.5秒免疫

                // 启动粒子效果任务
                startParticleEffect(player);

                // 启动定时任务，1.5秒后自动取消免疫状态
                BukkitRunnable immuneTask = new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (immunePlayers.containsKey(player.getUniqueId())) {
                            immunePlayers.remove(player.getUniqueId()); // 移除免疫状态
                            stopParticleEffect(player); // 取消粒子效果任务
                        }
                    }
                };
                immuneTask.runTaskLater(KitBattle.inst(), 30); // 1.5秒后执行（30 ticks = 1.5秒）
                immuneTasks.put(player.getUniqueId(), immuneTask); // 存储任务

                // 设置物品冷却时间
                player.setCooldown(Material.PINK_DYE, 480); // 24秒冷却时间
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (immunePlayers.containsKey(player.getUniqueId())) {
                long immuneTime = immunePlayers.get(player.getUniqueId());
                if (System.currentTimeMillis() <= immuneTime) {
                    // 免疫伤害
                    event.setCancelled(true);

                    // 给予伤害来源向上的动量并造成伤害
                    if (event.getDamager() instanceof LivingEntity) {
                        LivingEntity damager = (LivingEntity) event.getDamager();
                        damager.damage(15, player); // 造成15点伤害
                        damager.setVelocity(new Vector(0, 1, 0)); // 向上的动量
                    }

                    // 玩家进入旁观者模式
                    player.setGameMode(GameMode.SPECTATOR);

                    // 1秒后传送至伤害来源的位置并切换回冒险模式
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (event.getDamager() instanceof LivingEntity) {
                                LivingEntity damager = (LivingEntity) event.getDamager();
                                player.teleport(damager.getLocation());
                                player.setGameMode(GameMode.ADVENTURE);
                            }
                        }
                    }.runTaskLater(KitBattle.inst(), 30); // 1秒后执行
                }

                // 移除免疫状态并取消粒子效果任务
                immunePlayers.remove(player.getUniqueId());
                stopParticleEffect(player);

                // 取消免疫状态的任务
                BukkitRunnable immuneTask = immuneTasks.get(player.getUniqueId());
                if (immuneTask != null) {
                    immuneTask.cancel(); // 取消任务
                    immuneTasks.remove(player.getUniqueId()); // 从Map中移除任务
                }
            }
        }
    }

    // 启动粒子效果任务
    private void startParticleEffect(Player player) {
        BukkitRunnable particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!immunePlayers.containsKey(player.getUniqueId())) {
                    this.cancel(); // 如果玩家不再免疫，取消任务
                    particleTasks.remove(player.getUniqueId()); // 从Map中移除任务
                    return;
                }
                // 生成白色药水粒子效果
                player.getWorld().spawnParticle(Particle.EFFECT, player.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0);
            }
        };
        particleTask.runTaskTimer(KitBattle.inst(), 0, 5); // 每5 ticks执行一次
        particleTasks.put(player.getUniqueId(), particleTask); // 存储任务
    }

    // 取消粒子效果任务
    private void stopParticleEffect(Player player) {
        BukkitRunnable particleTask = particleTasks.get(player.getUniqueId());
        if (particleTask != null) {
            particleTask.cancel(); // 取消任务
            particleTasks.remove(player.getUniqueId()); // 从Map中移除任务
        }
    }
}


