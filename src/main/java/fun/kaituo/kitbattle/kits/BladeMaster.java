package fun.kaituo.kitbattle.kits;

import fun.kaituo.kitbattle.KitBattle;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Set;

import static fun.kaituo.kitbattle.KitBattle.PARTICLE_COUNT;
import static fun.kaituo.kitbattle.KitBattle.SOUND_VOLUME;

@SuppressWarnings("unused")
public class BladeMaster implements Kit {
    private final double skillRadius;
    private final double skillArrowDownwardSpeed;
    public BladeMaster() {
        skillRadius = KitBattle.inst().getConfig().getDouble("blade-master.radius");
        skillArrowDownwardSpeed = KitBattle.inst().getConfig().getDouble("blade-master.arrow-downward-speed");
    }

    @Override
    public long getCooldownTicks() {
        return KitBattle.inst().getConfig().getLong("blade-master.cd");
    }

    @Override
    public boolean castSkill(Player p) {
        Set<Player> victims = KitBattle.inst().getNearbyEnemies(p,skillRadius);
        if (victims.isEmpty()) {
            return false;
        }
        for (Player v : victims) {
            Arrow arrow = p.launchProjectile(Arrow.class, new Vector(0, skillArrowDownwardSpeed, 0));
            Location loc = v.getLocation().clone();
            loc.setY(loc.getY() + 2.5 );
            arrow.teleport(loc);
            KitBattle.inst().fakeEntityDestroy(arrow);
            World world = p.getWorld();
            world.playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP , SoundCategory.PLAYERS, SOUND_VOLUME, 1);
            world.spawnParticle(Particle.SWEEP_ATTACK, p.getLocation(), PARTICLE_COUNT, 3, 3, 3);
        }
        return true;
    }
}
