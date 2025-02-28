package fun.kaituo.kitbattle.kits;

import fun.kaituo.kitbattle.util.PlayerData;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import static fun.kaituo.kitbattle.KitBattle.PARTICLE_COUNT;
import static fun.kaituo.kitbattle.KitBattle.SOUND_VOLUME;

@SuppressWarnings("unused")
public class Uncrowned extends PlayerData {
    private final int glowingDuration;
    private final int resistanceDuration;
    private final int resistanceAmplifier;

    public Uncrowned(Player p) {
        super(p);
        glowingDuration = getConfigInt("glowing-duration");
        resistanceDuration = getConfigInt("resistance-duration");
        resistanceAmplifier = getConfigInt("resistance-amplifier");
    }

    @Override
    public boolean castSkill(Player p) {
        World world = p.getWorld();
        p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, glowingDuration, 0));
        p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, resistanceDuration, resistanceAmplifier));
        world.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE , SoundCategory.PLAYERS, SOUND_VOLUME, 2);
        world.spawnParticle(Particle.INSTANT_EFFECT, p.getLocation(), PARTICLE_COUNT, 3, 3, 3);
        return true;
    }
}
