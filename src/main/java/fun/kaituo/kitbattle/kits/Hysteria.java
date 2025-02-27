package fun.kaituo.kitbattle.kits;

import fun.kaituo.kitbattle.KitBattle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@SuppressWarnings("unused")
public class Hysteria implements Kit{
    private final int speedDuration;
    private final int speedAmplifier;
    private final int strengthDuration;
    private final int strengthAmplifier;

    public Hysteria() {
        FileConfiguration config = KitBattle.inst().getConfig();
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
