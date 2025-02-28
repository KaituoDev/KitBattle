package fun.kaituo.kitbattle.kits;

import fun.kaituo.kitbattle.util.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

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
    public boolean castSkill(Player p) {
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, speedDuration, speedAmplifier));
        p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, strengthDuration, strengthAmplifier));
        return true;
    }
}
