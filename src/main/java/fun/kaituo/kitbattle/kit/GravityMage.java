package fun.kaituo.kitbattle.kit;

import fun.kaituo.kitbattle.KitBattle;
import fun.kaituo.kitbattle.util.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Set;

@SuppressWarnings("unused")
public class GravityMage extends PlayerData {
    private final int slownessDuration;
    private final int slownessAmplifier;
    private final int radius;

    public GravityMage(Player p) {
        super(p);
        slownessDuration = getConfigInt("slowness-duration");
        slownessAmplifier = getConfigInt("slowness-amplifier");
        radius = getConfigInt("radius");
    }

    @Override
    public boolean castSkill() {
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
