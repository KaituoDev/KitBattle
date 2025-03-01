package fun.kaituo.kitbattle.kits;

import fun.kaituo.kitbattle.util.PlayerData;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;

import static fun.kaituo.kitbattle.KitBattle.PARTICLE_COUNT;
import static fun.kaituo.kitbattle.KitBattle.SOUND_VOLUME;

@SuppressWarnings("unused")
public class Pilot extends PlayerData {
    private final int flyDuration;
    private int remainingFlyDuration;

    public Pilot(Player p) {
        super(p);
        flyDuration = getConfigInt("fly-duration");
        remainingFlyDuration = 0;
    }

    @Override
    public void destroy(Player p) {
        super.destroy(p);
        p.setAllowFlight(false);
    }

    @Override
    public void quit(Player p) {
        super.quit(p);
        p.setAllowFlight(false);
    }

    @Override
    public void rejoin(Player p) {
        super.rejoin(p);
        if (remainingFlyDuration > 0) {
            p.setAllowFlight(true);
        }
    }

    @Override
    public void tick(Player p) {
        super.tick(p);
        if (remainingFlyDuration > 0) {
            remainingFlyDuration -= 1;
        }
        if (remainingFlyDuration == 0) {
            p.setAllowFlight(false);
        }
    }

    @Override
    public boolean castSkill(Player p) {
        World world = p.getWorld();
        world.playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH , SoundCategory.PLAYERS, SOUND_VOLUME, 1);
        world.spawnParticle(Particle.END_ROD, p.getLocation(), PARTICLE_COUNT, 3.0, 3.0, 3.0);
        p.setAllowFlight(true);
        remainingFlyDuration = flyDuration;
        return true;
    }
}
