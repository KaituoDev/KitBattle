package fun.kaituo.kitbattle.kits;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Bower implements Kit{
    @Override
    public void applyPotionEffects(Player p) {
        p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, -1, 2, false, false));
    }
}
