package fun.kaituo.kitbattle.kits;

import fun.kaituo.kitbattle.util.PlayerData;
import org.bukkit.*;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class StormKnight extends PlayerData {
    private int stormDuration;
    private int slownessAmplifier;
    private int resistanceAmplifier;
    private int jumpboostAmplifier;
    public StormKnight(Player p){
        super(p);
        stormDuration = getConfigInt("storm-duration");
        slownessAmplifier = getConfigInt("slowness-amplifier");
        resistanceAmplifier = getConfigInt("resistance-amplifier");
        jumpboostAmplifier = getConfigInt("jump-boost-amplifier");
    }
    public boolean castSkill(Player p){
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, stormDuration, slownessAmplifier));
        p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, stormDuration, resistanceAmplifier));
        p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, stormDuration, jumpboostAmplifier));
        World world = p.getWorld();
        world.playSound(p.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 10, 0);
        playerTaskIds.get(p).add(Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player v : getNearbyPlayers(p, 3)) {
                Arrow a = p.launchProjectile(Arrow.class, new Vector(0, c.getDouble("stormknight.first-arrow-downward-speed"), 0));
                Location l = v.getLocation().clone();
                l.setY(l.getY() + 2.5 );
                a.teleport(l);
                broadcastEntityDestroyPacket(a);
            }
            world.spawnParticle(Particle.CLOUD, p.getLocation(), 50, 2, 2, 2);
        }, 20).getTaskId());
        playerTaskIds.get(p).add(Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player v : getNearbyPlayers(p, 3)) {
                Arrow a = p.launchProjectile(Arrow.class, new Vector(0, c.getDouble("stormknight.second-arrow-downward-speed"), 0));
                Location l = v.getLocation().clone();
                l.setY(l.getY() + 2.5 );
                a.teleport(l);
                broadcastEntityDestroyPacket(a);
            }
            world.spawnParticle(Particle.CLOUD, p.getLocation(), 75, 3, 3, 3);
            world.playSound(p.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 10, 0);
        }, 40).getTaskId());
        playerTaskIds.get(p).add(Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player v : getNearbyPlayers(p, 3)) {
                Arrow a = p.launchProjectile(Arrow.class, new Vector(0, c.getDouble("stormknight.third-arrow-downward-speed"), 0));
                Location l = v.getLocation().clone();
                l.setY(l.getY() + 2.5 );
                a.teleport(l);
                broadcastEntityDestroyPacket(a);
            }
            world.spawnParticle(Particle.CLOUD, p.getLocation(), 100, 4, 4, 4);
        }, 60).getTaskId());
        return true;
    }

    }

}
