package fun.kaituo.kitbattle.kit;

import fun.kaituo.kitbattle.KitBattle;
import fun.kaituo.kitbattle.util.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static fun.kaituo.kitbattle.KitBattle.SOUND_VOLUME;

public class Judge extends PlayerData {
    private final Set<FallingBlock> anvils = new HashSet<>();
    private final int radius;
    private final double damage;
    private final double distance;

    public Judge(Player player) {
        super(player);
        radius = getConfigInt("radius");
        damage = getConfigDouble("damage");
        distance = getConfigDouble("distance");
    }

    @Override
    public void onDestroy() {
        removeAnvils();
        super.onDestroy();
    }

    @Override
    public void onQuit() {
        removeAnvils();
        super.onQuit();
    }

    @Override
    public void tick() {
        for (FallingBlock anvil : anvils) {
            if (anvil.isDead()) {
                continue;
            }
            for (Entity victim : anvil.getWorld().getNearbyEntities(anvil.getLocation(),
                    0.5, 0.5, 0.5, target -> target instanceof Player && !target.equals(p) && KitBattle.inst().isInArena((Player) target))) {
                ((Player) victim).damage(damage, p);
            }
        }
        super.tick();
    }

    @Override
    public boolean castSkill() {
        // Get the player to lock on to (within 20 blocks)
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

        // Trigger Anvil rain skill
        Bukkit.getScheduler().runTaskLater(KitBattle.inst(), () -> triggerAnvilRain(target), 1L);
        return  true;
    }

    private void removeAnvils() {
        for (FallingBlock anvil : anvils) {
            anvil.remove();
        }
        anvils.clear();
    }

    private void spawnAnvil(Location l) {
        FallingBlock anvil = p.getWorld().spawn(l, FallingBlock.class,
                fallingBlock -> fallingBlock.setBlockData(Material.ANVIL.createBlockData()));

        anvil.setDropItem(false); // Prevent the anvil from dropping as an item
        anvil.setVelocity(new Vector(0, -0.5, 0));
        anvil.setHurtEntities(false);

        anvils.add(anvil);
    }

    private void triggerAnvilRain(Player target) {
        List<Location> locationOffsets = new ArrayList<>();
        for (int x = - radius; x < radius; x += 1) {
            for (int z = - radius; z < radius; z += 1) {
                Location offset = new Location(target.getWorld(), x, 0, z);
                if (offset.toVector().length() > radius) {
                    continue;
                }
                locationOffsets.add(offset);
            }
        }
        Random rand = new Random();
        // Generate 20 anvils in a 5-block radius around the target's head
        for (int i = 0; i < 20; i++) {
            int index = rand.nextInt(locationOffsets.size());
            Location offset = locationOffsets.get(index);
            locationOffsets.remove(index);
            taskIds.add(Bukkit.getScheduler().runTaskLater(KitBattle.inst(), () -> {
                if (!KitBattle.inst().isInArena(target)) {
                    return;
                }
                Location targetLocation = target.getLocation().add(0, 10, 0); // 10 blocks above target
                targetLocation.setX(targetLocation.getBlockX() + 0.5);
                targetLocation.setY(targetLocation.getBlockY());
                targetLocation.setZ(targetLocation.getBlockZ() + 0.5);
                targetLocation = targetLocation.add(offset);
                spawnAnvil(targetLocation);
            }, i * 2).getTaskId());
        }
    }

    @EventHandler
    public void onAnvilLand(EntityChangeBlockEvent e) {
        for (FallingBlock anvil : new HashSet<>(anvils)) {
            if (e.getEntity().equals(anvil)) {
                e.setCancelled(true);
                anvil.getWorld().playSound(anvil.getLocation(), Sound.BLOCK_ANVIL_LAND, SOUND_VOLUME, 1);
                anvil.remove();
                anvils.remove(anvil);
                return;
            }
        }
    }
}



