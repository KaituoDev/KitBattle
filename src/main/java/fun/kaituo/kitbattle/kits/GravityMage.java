package fun.kaituo.kitbattle.kits;

import fun.kaituo.kitbattle.KitBattle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Set;

public class GravityMage implements Kit{
    private final int slowDuration;
    private final int slowAmplifier;
    private final int radius;

    public GravityMage() {
        FileConfiguration config = KitBattle.inst().getConfig();
        slowDuration = config.getInt(getConfigPrefix() + "slowness-duration");
        slowAmplifier = config.getInt(getConfigPrefix() + "slowness-amplifier");
        radius = config.getInt(getConfigPrefix() + "radius");
    }

    @Override
    public boolean castSkill(Player p) {
        Set<Player> enemies =  KitBattle.inst().getNearbyEnemies(p, radius);
        if (enemies.isEmpty()) {
            return false;
        }
        for (Player victim: enemies) {
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slowDuration, slowAmplifier));
        }
        return true;
    }
}
