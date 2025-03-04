package fun.kaituo.kitbattle.kit;

import fun.kaituo.kitbattle.KitBattle;
import fun.kaituo.kitbattle.util.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Random;

public class Judge extends PlayerData {

    public Judge(Player player) {
        super(player);
    }

    @Override
    public boolean castSkill() {
        // Get the player to lock on to (within 20 blocks)
        RayTraceResult result = p.getWorld().rayTraceEntities(
                p.getEyeLocation(),
                p.getEyeLocation().getDirection(),
                20,
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
        Bukkit.getScheduler().runTaskLater(KitBattle.inst(), () -> triggerAnvilRain(p, target), 1L);
        return  true;
    }

    private void triggerAnvilRain(Player player, Player target) {
        Location targetLocation = target.getLocation().add(0, 10, 0); // 10 blocks above target
        Random rand = new Random();

        // Generate 20 anvils in a 5-block radius around the target's head
        for (int i = 0; i < 20; i++) {
            double offsetX = rand.nextDouble() * 10 - 5; // Random position within a 5-block radius
            double offsetZ = rand.nextDouble() * 10 - 5;
            Location anvilLocation = targetLocation.clone().add(offsetX, 0, offsetZ);

            // Spawn the anvil at this location
            BlockData data = Material.ANVIL.createBlockData();
            FallingBlock anvil =  player.getWorld().spawnFallingBlock(anvilLocation,data);

            anvil.setGravity(false); // Disable gravity initially to make it stay in the air
            anvil.setVelocity(new Vector(0, 0, 0)); // Make it stationary

            // Set the anvil to start falling after 1 second
            Bukkit.getScheduler().runTaskLater(KitBattle.inst(), () -> {
                anvil.setGravity(true); // Enable gravity after 1 second to make it fall
                anvil.setVelocity(new Vector(0, -0.5, 0)); // Apply downward velocity to make it fall
            }, 20L); // 20 ticks = 1 second
        }
    }

    @EventHandler
    public void onAnvilDamage(EntityDamageByEntityEvent event) {
        // Check if the entity causing the damage is a falling anvil
        if (event.getDamager() instanceof FallingBlock anvil) {
            if (anvil.getBlockData().getMaterial() == Material.ANVIL) {
                // Check if the damaged entity is a player
                if (event.getEntity() instanceof Player target) {
                    // Ensure the damage is being dealt by an anvil spawned by the skill
                    if (target.getHealth() > 0) {
                        // Deal damage as an anvil falling
                        event.setDamage(5.0); // Adjust the damage as needed
                    }
                }
            }
        }
    }
}



