package fun.kaituo.kitbattle.kits;

import fun.kaituo.kitbattle.KitBattle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Set;

@SuppressWarnings("unused")
public class GravityMage implements Kit{
    private final int slownessDuration;
    private final int slownessAmplifier;
    private final int radius;

    public GravityMage() {
        FileConfiguration config = KitBattle.inst().getConfig();
        slownessDuration = getConfigInt("slowness-duration");
        slownessAmplifier = getConfigInt("slowness-amplifier");
        radius = getConfigInt("radius");
    }

    public boolean castSkill(Player p) {
        Set<Player> enemies =  KitBattle.inst().getNearbyEnemies(p, radius);
        if (enemies.isEmpty()) {
            return false;
        }
        for (Player victim: enemies) {
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slownessDuration, slownessAmplifier));
        }
        return true;
    }
}
