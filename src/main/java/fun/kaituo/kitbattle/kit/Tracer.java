package fun.kaituo.kitbattle.kit;

import fun.kaituo.kitbattle.KitBattle;
import fun.kaituo.kitbattle.util.PlayerData;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import static fun.kaituo.kitbattle.KitBattle.PARTICLE_COUNT;
import static fun.kaituo.kitbattle.KitBattle.SOUND_VOLUME;

@SuppressWarnings("unused")
public class Tracer extends PlayerData {
    private final double skillDistance;

    public Tracer(Player p) {
        super(p);
        skillDistance = getConfigDouble("distance");
    }

    @Override
    public boolean castSkill(Player p) {
        Player target = KitBattle.inst().getNearestEnemy(p, 9999);
        if (target == null) {
            return false;
        }
        Location targetLoc = target.getLocation();
        Location casterLoc = p.getLocation();
        Vector diff = targetLoc.toVector().subtract(casterLoc.toVector());
        if (diff.length() < skillDistance) {
            p.teleport(targetLoc);
        } else {
            Vector movementVec = diff.normalize().multiply(skillDistance);
            p.teleport(casterLoc.add(movementVec));
        }
        World world = p.getWorld();
        world.playSound(casterLoc, Sound.ENTITY_ENDERMAN_TELEPORT , SoundCategory.PLAYERS, SOUND_VOLUME, 1);
        world.spawnParticle(Particle.DRAGON_BREATH, p.getLocation(), PARTICLE_COUNT, 3, 3, 3);
        return true;
    }
}
