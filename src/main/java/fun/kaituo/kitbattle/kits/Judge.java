package fun.kaituo.kitbattle.kits;

import com.comphenix.protocol.wrappers.BlockPosition;
import fun.kaituo.kitbattle.KitBattle;
import fun.kaituo.kitbattle.util.PlayerData;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;

import java.util.Random;

public class Judge extends PlayerData {
    private final int distance;
    public Judge(Player p) {
        super(p);
        distance = getConfigInt("distance");
    }

    public boolean castSkill(Player p) {
        RayTraceResult result = p.getWorld().rayTraceEntities(
                p.getEyeLocation(),
                p.getEyeLocation().getDirection(),
                distance,
                e -> e != p && e instanceof Player && KitBattle.inst().isInArena((Player) e)
        );
        if (result == null) {
            return false;
        }
        Player target = (Player) result.getHitEntity();
        if (target == null) {
            return false;
        }

        // 获取目标位置的坐标
        double x = target.getLocation().getX();
        double y = target.getLocation().getY() + 10; // 上方10格
        double z = target.getLocation().getZ();
        Location targetLoc = new Location(p.getWorld(), x, y, z);
        int X =(int)x;
        int Y =(int)y;
        int Z =(int)z;

        // 生成一个平面圆形范围内的随机位置
        double radius = 5.0;
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                World world = p.getWorld();
                if (ticks >= 10) { // 两秒钟后停止生成
                    cancel();
                    return;
                }

                // 随机生成铁砧位置
                BlockPosition randomPos = getRandomPositionInCircle(new BlockPosition(X, Y, Z), radius);
                Location randomLoc = new Location(p.getWorld(), randomPos.getX(), randomPos.getY(), randomPos.getZ());

                // 生成铁砧
                BlockData data = Material.ANVIL.createBlockData();
                FallingBlock fallingBlock = world.spawnFallingBlock(new Location(world, X, Y, Z), data);
                fallingBlock.setHurtEntities(true);
                fallingBlock.setDropItem(false);

                // 设置铁砧1秒后掉落，并且触地后销毁
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        fallingBlock.setGravity(true); // 开始掉落
                    }
                }.runTaskLater(KitBattle.inst(), 20); // 延迟1秒

                // 铁砧触地后销毁
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (fallingBlock.isOnGround()) {
                            fallingBlock.remove(); // 销毁铁砧
                        }
                    }
                }.runTaskLater(KitBattle.inst(), 40); // 2秒后检查并销毁

                ticks++;
            }
        }.runTaskTimer(KitBattle.inst(), 0, 4); // 每0.2秒生成一个铁砧

        return true;
    }

    public BlockPosition getRandomPositionInCircle(BlockPosition center, double radius) {
        Random random = new Random();
        double angle = random.nextDouble() * Math.PI * 2;

        // 生成一个随机半径（0 到给定半径）
        double r = random.nextDouble() * radius;

        // 通过极坐标公式生成随机的 x 和 z 坐标
        double xOffset = r * Math.cos(angle);
        double zOffset = r * Math.sin(angle);

        // 根据偏移量计算新的坐标
        int X = center.getX() + (int) xOffset;
        int Z = center.getZ() + (int) zOffset;


        // 保持 y 值不变，返回新的随机坐标
        return new BlockPosition(X, center.getY(), Z);
    }
}








