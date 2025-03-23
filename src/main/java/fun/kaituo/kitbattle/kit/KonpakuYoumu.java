package fun.kaituo.kitbattle.kit;

import fun.kaituo.kitbattle.KitBattle;
import fun.kaituo.kitbattle.util.PlayerData;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
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

    private static final int COOL_DOWN_TICKS = 600;

    private BukkitRunnable skillTask;
    private Set<UUID> affectedEntities = new HashSet<>();

    private Map<UUID, Long> immunePlayers = new HashMap<>(); // 存储免疫玩家的Map

    public KonpakuYoumu(Player p) {

        super(p);
        KitBattle.inst().getServer().getPluginManager().registerEvents(this, KitBattle.inst());
        p.setCooldown(Material.PINK_PETALS, 0);
        p.setCooldown(Material.PINK_DYE, 0);
        p.setCooldown(Material.POPPED_CHORUS_FRUIT, 0);
        p.setCooldown(Material.STONE_SWORD, 0);

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
            if (enemy.isDead() || !enemy.isValid() || enemy instanceof ArmorStand || enemy instanceof Pig) continue;
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
                    if (entity.isDead() || !entity.isValid() || entity instanceof ArmorStand) continue;


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

        Random random = new Random();

        // 1. 生成一个 0 度到 45 度之间的水平向量
        double angle = Math.toRadians(random.nextDouble() * 45); // 随机生成 0 度到 45 度的角度
        double x = 7 * Math.cos(angle); // 长度为 7 的 X 分量
        double z = 7 * Math.sin(angle); // 长度为 7 的 Z 分量
        Vector direction = new Vector(x, 0, z).normalize(); // 归一化向量

        // 2. 生成随机的垂直平面圆心角（绕 X 轴的旋转角度）
        double pitch = Math.toRadians(random.nextDouble() * 360); // 随机生成 0 度到 360 度的角度

        // 3. 将水平向量乘以圆心角（绕 X 轴旋转）
        double cosPitch = Math.cos(pitch);
        double sinPitch = Math.sin(pitch);
        double rotatedY = direction.getY() * cosPitch - direction.getZ() * sinPitch; // 旋转后的 Y 分量
        double rotatedZ = direction.getY() * sinPitch + direction.getZ() * cosPitch; // 旋转后的 Z 分量
        Vector rotatedDirection = new Vector(direction.getX(), rotatedY, rotatedZ).normalize(); // 旋转后的向量

        // 4. 生成反向量
        Vector oppositeDirection = rotatedDirection.clone().multiply(-1); // 反向量

        // 5. 生成随机向量（长度为 0 到 1 格）
        double offsetX = (random.nextDouble() - 0.5) * 2; // [-1, 1]
        double offsetY = (random.nextDouble() - 0.5) * 2; // [-1, 1]
        double offsetZ = (random.nextDouble() - 0.5) * 2; // [-1, 1]
        Vector randomOffset = new Vector(offsetX, offsetY, offsetZ).normalize().multiply(random.nextDouble()); // 随机向量

        // 6. 连接向量和反向量，生成直线
        for (double t = -7; t <= 7; t += 0.08) { // 从 -7 到 7，步长为 0.08
            Vector currentVector = rotatedDirection.clone().multiply(t); // 当前向量
            Location particleLocation = center.clone().add(currentVector); // 当前点的位置

            // 7. 应用随机向量
            particleLocation.add(randomOffset);

            // 生成粒子
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
                    Block targetBlock = player.getTargetBlock(null, 10); // 最大距离为 10 格
                    if (targetBlock == null) return;

                    // 获取目标方块的周围安全位置
                    Location safeLocation = findSafeLocationAroundBlock(targetBlock.getLocation(),player);
                    if (safeLocation != null) {
                        // 传送玩家到安全位置
                        player.teleport(safeLocation);
                    } else {
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



    /**
     * 在目标方块周围寻找一个安全位置（排除远离玩家的一侧）
     *
     * @param targetLocation 目标方块的坐标
     * @param player         玩家
     * @return 安全位置的 Location，如果未找到则返回 null
     */
    private Location findSafeLocationAroundBlock(Location targetLocation, Player player) {
        World world = targetLocation.getWorld();
        if (world == null) return null;

        // 获取玩家与目标方块的相对方向
        Vector playerDirection = player.getLocation().toVector().subtract(targetLocation.toVector()).normalize();

        // 优先检测目标方块上方
        Location aboveLocation = targetLocation.clone().add(0, 1, 0);
        if (isSafeLocation(aboveLocation)) {
            return aboveLocation;
        }

        // 检测四周（东、西、南、北），排除远离玩家的一侧
        Location[] sideLocations = {
                targetLocation.clone().add(1, 0, 0), // 东
                targetLocation.clone().add(-1, 0, 0), // 西
                targetLocation.clone().add(0, 0, 1), // 南
                targetLocation.clone().add(0, 0, -1) // 北
        };

        for (Location side : sideLocations) {
            // 计算目标方块到侧面的方向向量
            Vector sideDirection = side.toVector().subtract(targetLocation.toVector()).normalize();

            // 如果侧面方向与玩家方向相反（点积为负），则跳过
            if (playerDirection.dot(sideDirection) < 0) {
                continue; // 跳过远离玩家的一侧
            }

            if (isSafeLocation(side)) {
                return side;
            }
        }

        // 最后检测目标方块下方
        Location belowLocation = targetLocation.clone().add(0, -1, 0);
        if (isSafeLocation(belowLocation)) {
            return belowLocation;
        }

        // 如果未找到安全位置，返回 null
        return null;
    }

    /**
     * 检查目标位置是否安全（即目标位置及其上方一格是否为空气）
     *
     * @param location 目标位置
     * @return 是否安全
     */
    private boolean isSafeLocation(Location location) {
        World world = location.getWorld();
        if (world == null) return false;

        // 检查目标位置及其上方一格是否为空气
        Block targetBlock = world.getBlockAt(location);
        Block aboveBlock = world.getBlockAt(location.clone().add(0, 1, 0));
        return targetBlock.getType() == Material.AIR && aboveBlock.getType() == Material.AIR;
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
                player.setCooldown(Material.PINK_DYE, 800); // 40scd
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) {
            return;
        }
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


    @EventHandler
    public void E(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // 检查玩家手持的物品是否为POPPED_CHORUS_FRUIT且包含“六道剑”字符
        if (item != null && item.getType() == Material.POPPED_CHORUS_FRUIT) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.getDisplayName().contains("六道剑")) {
                // 检查冷却时间
                if (player.hasCooldown(Material.POPPED_CHORUS_FRUIT)) {
                    return; // 如果处于冷却时间，则直接返回
                }

                // 获取玩家视线方向
                Vector direction = player.getEyeLocation().getDirection().normalize();

                // 计算左右两道平行射线的方向
                Vector rightDirection = new Vector(-direction.getZ(), 0, direction.getX()).normalize();
                Vector leftDirection = new Vector(direction.getZ(), 0, -direction.getX()).normalize();

                // 发射三道射线并获取命中的实体
                LivingEntity hitEntity = shootRays(player, direction, rightDirection, leftDirection);

                if (hitEntity != null) {
                    // 以命中实体的位置为圆心，对半径5格的平面圆形内的所有实体造成伤害和三秒的缓慢2
                    affectEntitiesInRadius(hitEntity.getLocation(), 5, player);

                    // 使物品POPPED_CHORUS_FRUIT进入10秒的冷却
                    player.setCooldown(Material.POPPED_CHORUS_FRUIT, 350); // 15.5秒冷却时间（200 ticks）
                }
            }
        }
    }

    private LivingEntity shootRays(Player player, Vector direction, Vector rightDirection, Vector leftDirection) {
        World world = player.getWorld();
        Location start = player.getEyeLocation();

        // 调整起点位置，使其从玩家前方一定距离开始（例如2格）
        Location adjustedStart = start.clone().add(direction.clone().multiply(2)); // 从玩家前方2格开始

        // 发射三道射线
        LivingEntity hitEntity = shootRay(world, adjustedStart, direction);
        if (hitEntity == null) {
            hitEntity = shootRay(world, adjustedStart, rightDirection);
        }
        if (hitEntity == null) {
            hitEntity = shootRay(world, adjustedStart, leftDirection);
        }

        return hitEntity;
    }

    private LivingEntity shootRay(World world, Location start, Vector direction) {
        double maxDistance = 10; // 最大射程
        double stepSize = 0.3; // 每步检测的间隔距离

        Location currentLocation = start.clone();
        while (currentLocation.distance(start) < maxDistance) {
            currentLocation.add(direction.clone().multiply(stepSize));

            // 检测当前点的实体
            for (Entity entity : world.getNearbyEntities(currentLocation, 0.5, 0.5, 0.5)) {
                if (entity instanceof LivingEntity && !entity.equals(p.getPlayer())) {
                    return (LivingEntity) entity;
                }
            }
        }

        return null;
    }

    private void affectEntitiesInRadius(Location center, double radius, Player player) {
        World world = center.getWorld();
        if (world == null) return;

        // 生成五芒星粒子特效
        generatePentagram(center);

        for (Entity entity : world.getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof LivingEntity && !entity.equals(player)) {
                LivingEntity livingEntity = (LivingEntity) entity;

                // 造成伤害
                livingEntity.damage(8, player);

                // 给予缓慢2效果，持续3秒（60 ticks）
                livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
            }
        }
    }
    private void generatePentagram(Location center) {
        World world = center.getWorld();
        if (world == null) return;

        double radius = 7.0; // 五芒星的半径
        double angleIncrement = 2 * Math.PI / 5; // 五芒星的每个顶点之间的角度增量

        // 五芒星的五个顶点坐标
        Location[] points = new Location[5];
        for (int i = 0; i < 5; i++) {
            double angle = i * angleIncrement;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            double y = center.getY() + 1; // 在 center 的基础上向上偏移1格
            points[i] = new Location(world, x, y, z);
        }

        // 连接五芒星的顶点，生成粒子特效
        for (int i = 0; i < 5; i++) {
            Location start = points[i];
            Location end = points[(i + 2) % 5]; // 五芒星的连接方式：跳过两个顶点

            // 在两点之间生成粒子特效
            generateLineParticles(start, end, world);
        }
    }

    private void generateLineParticles(Location start, Location end, World world) {
        double stepSize = 0.1; // 每步的间隔距离
        Vector direction = end.toVector().subtract(start.toVector()).normalize();
        double distance = start.distance(end);

        // 从起点到终点逐段生成粒子
        for (double t = 0; t <= distance; t += stepSize) {
            Location particleLocation = start.clone().add(direction.clone().multiply(t));
            world.spawnParticle(Particle.FIREWORK, particleLocation, 1, 0, 0, 0, 0.01);
        }
    }



    /**
     * 发射剑气
     *
     * @param player 玩家
     * @param start  起点
     * @param end    终点
     */
    @EventHandler
    public void A(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack mainHandItem = player.getInventory().getItemInMainHand();
        ItemStack offHandItem = player.getInventory().getItemInOffHand();

        // 检查主手是否为含有“楼观剑”的铁剑，副手是否为含有“白楼剑”的石剑
        if (mainHandItem != null && mainHandItem.getType() == Material.IRON_SWORD &&
                offHandItem != null && offHandItem.getType() == Material.STONE_SWORD) {
            ItemMeta mainHandMeta = mainHandItem.getItemMeta();
            ItemMeta offHandMeta = offHandItem.getItemMeta();

            if (mainHandMeta != null && mainHandMeta.getDisplayName().contains("楼观剑") &&
                    offHandMeta != null && offHandMeta.getDisplayName().contains("白楼剑")) {

                // 检查副手石剑是否处于冷却状态
                if (player.hasCooldown(Material.STONE_SWORD)) {
                    return;
                }

                // 获取玩家视线方向（水平方向）
                Vector direction = player.getEyeLocation().getDirection().normalize();
                direction.setY(0); // 将Y轴分量设为0，确保剑气水平射出

                // 计算起点和终点
                Location start = player.getLocation().add(0, 1, 0); // 从玩家腰部高度开始
                Location end = start.clone().add(direction.multiply(15)); // 最大距离为15格

                // 发射剑气
                launchSwordWave(player, start, end);

                // 设置副手石剑的冷却时间为10秒（200 ticks）
                player.setCooldown(Material.STONE_SWORD, 200);
            }
        }
    }

    /**
     * 发射剑气
     *
     * @param player 玩家
     * @param start  起点
     * @param end    终点
     */
    private void launchSwordWave(Player player, Location start, Location end) {
        World world = start.getWorld();
        if (world == null) return;

        // 计算剑气的移动方向
        Vector direction = end.toVector().subtract(start.toVector()).normalize();
        double distance = start.distance(end);

        // 创建一个BukkitRunnable来移动剑气
        new BukkitRunnable() {
            double progress = 0; // 当前进度
            final double speed = 1; // 移动速度（每tick移动的格数）

            @Override
            public void run() {
                if (progress >= distance) {
                    this.cancel(); // 到达终点后取消任务
                    return;
                }

                // 计算当前剑气的位置
                Location currentLocation = start.clone().add(direction.clone().multiply(progress));

                // 生成剑气粒子特效
                generateSwordWaveParticles(currentLocation, world);

                // 对当前剑气位置的实体造成伤害
                for (Entity entity : world.getNearbyEntities(currentLocation, 1.0, 1.0, 1.0)) {
                    if (entity instanceof LivingEntity && !entity.equals(player)) {
                        ((LivingEntity) entity).damage(4.0, player); // 造成4点伤害
                    }
                }

                // 更新进度
                progress += speed;
            }
        }.runTaskTimer(KitBattle.inst(), 0, 1); // 每tick执行一次
    }

    /**
     * 生成剑气粒子特效
     *
     * @param center 剑气中心点
     * @param world  世界
     */
    private void generateSwordWaveParticles(Location center, World world) {
        // 生成一条直线上的粒子特效
        world.spawnParticle(Particle.SWEEP_ATTACK, center, 5, 0.1, 0.1, 0.1, 0); // 生成5个SWEEP_ATTACK粒子
        world.spawnParticle(Particle.CRIT, center, 3, 0.1, 0.1, 0.1, 0.1); // 生成3个CRIT粒子
    }

    @EventHandler
    public void coolDown(EntityDamageByEntityEvent event) {
        // 检查是否是玩家使用横扫攻击
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            ItemStack mainHandItem = player.getInventory().getItemInMainHand();

            // 检查主手是否为含有“楼观剑”的铁剑
            if (mainHandItem != null && mainHandItem.getType() == Material.IRON_SWORD) {
                ItemMeta mainHandMeta = mainHandItem.getItemMeta();
                if (mainHandMeta != null && mainHandMeta.getDisplayName().contains("楼观剑")) {

                    // 检查副手石剑是否处于冷却状态
                    if (player.hasCooldown(Material.STONE_SWORD)) {
                        // 获取当前剩余的冷却时间
                        int remainingCooldown = player.getCooldown(Material.STONE_SWORD);

                        // 减少冷却时间1秒（5 ticks）
                        if (remainingCooldown > 5) {
                            player.setCooldown(Material.STONE_SWORD, remainingCooldown - 5);
                        } else {
                            player.setCooldown(Material.STONE_SWORD, 0); // 冷却时间清零
                        }
                    }
                }
            }
        }
    }
}


