package fun.kaituo.kitbattle.kits;

import fun.kaituo.kitbattle.KitBattle;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.EvokerFangs;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@SuppressWarnings("unused")
public class Jizo implements Kit{
    private final int slownessDuration;
    private final int slownessAmplifier;
    private final int blindnessDuration;
    private final int radius;

    public Jizo() {
        FileConfiguration config = KitBattle.inst().getConfig();
        slownessDuration = getConfigInt("slowness-duration");
        slownessAmplifier = getConfigInt("slowness-amplifier");
        blindnessDuration = getConfigInt("blindness-duration");
        radius = getConfigInt("radius");
    }

    public boolean castSkill(Player p) {
        Player victim = KitBattle.inst().getNearestEnemy(p, radius);
        if (victim == null) {
            return false;
        }
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slownessDuration, slownessAmplifier));
        victim.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, blindnessDuration, 0));
        Location l = victim.getLocation();
        Location l2 = l.clone(); l2.setX(l2.getX() - 1);
        Location l3 = l.clone(); l3.setX(l3.getX() + 1);
        Location l4 = l.clone(); l4.setZ(l4.getZ() - 1);
        Location l5 = l.clone(); l5.setZ(l5.getZ() + 1);
        EvokerFangs[] fangs = new EvokerFangs[5];
        fangs[0] = (EvokerFangs) p.getWorld().spawnEntity(l, EntityType.EVOKER_FANGS);
        fangs[1] = (EvokerFangs) p.getWorld().spawnEntity(l2, EntityType.EVOKER_FANGS);
        fangs[2] = (EvokerFangs) p.getWorld().spawnEntity(l3, EntityType.EVOKER_FANGS);
        fangs[3] = (EvokerFangs) p.getWorld().spawnEntity(l4, EntityType.EVOKER_FANGS);
        fangs[4] = (EvokerFangs) p.getWorld().spawnEntity(l5, EntityType.EVOKER_FANGS);
        for (EvokerFangs fang : fangs) {
            fang.setOwner(p);
        }
        return true;
    }
}
