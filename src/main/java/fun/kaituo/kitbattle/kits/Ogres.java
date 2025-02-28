package fun.kaituo.kitbattle.kits;

import fun.kaituo.kitbattle.util.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@SuppressWarnings("unused")
public class Ogres extends PlayerData {
    public Ogres(Player p) {
        super(p);
        p.setHealth(80);
    }

    @Override
    public void applyPotionEffects(Player p) {
        super.applyPotionEffects(p);
        p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,-1,1, false, false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST,-1,14, false, false));
    }
}
