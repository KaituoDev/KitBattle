package fun.kaituo.kitbattle.kit;

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
public class Hysteria extends PlayerData {
    private final int speedDuration;
    private final int speedAmplifier;
    private final int strengthDuration;
    private final int strengthAmplifier;

    public Hysteria(Player p) {
        super(p);
        speedDuration = getConfigInt("speed-duration");
        speedAmplifier = getConfigInt("speed-amplifier");
        strengthDuration = getConfigInt("strength-duration");
        strengthAmplifier = getConfigInt("strength-amplifier");
    }

    @Override
    public boolean castSkill() {
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, speedDuration, speedAmplifier));
        p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, strengthDuration, strengthAmplifier));
        World world = p.getWorld();
        world.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL , SoundCategory.PLAYERS, SOUND_VOLUME, 2);
        world.spawnParticle(Particle.ANGRY_VILLAGER, p.getLocation(), PARTICLE_COUNT, 3, 3, 3);
        return true;
    }
}
