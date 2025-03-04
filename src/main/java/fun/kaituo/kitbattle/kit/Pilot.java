package fun.kaituo.kitbattle.kit;

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
    public void onDestroy() {
        p.setAllowFlight(false);
        super.onDestroy();
    }

    @Override
    public void onQuit() {
        p.setAllowFlight(false);
        super.onQuit();
    }

    @Override
    public void onRejoin() {
        super.onRejoin();
        if (remainingFlyDuration > 0) {
            p.setAllowFlight(true);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (remainingFlyDuration > 0) {
            remainingFlyDuration -= 1;
        }
        if (remainingFlyDuration == 0) {
            p.setAllowFlight(false);
        }
    }

    @Override
    public boolean castSkill() {
        World world = p.getWorld();
        world.playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH , SoundCategory.PLAYERS, SOUND_VOLUME, 1);
        world.spawnParticle(Particle.END_ROD, p.getLocation(), PARTICLE_COUNT, 3.0, 3.0, 3.0);
        p.setAllowFlight(true);
        remainingFlyDuration = flyDuration;
        return true;
    }
}
