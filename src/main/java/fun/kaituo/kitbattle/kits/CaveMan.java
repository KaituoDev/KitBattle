package fun.kaituo.kitbattle.kits;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@SuppressWarnings("unused")
public class CaveMan implements Kit{
    public void applyPotionEffects(Player p) {
        p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, -1, 1, false, false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, -1, 0, false, false));
    }
}
