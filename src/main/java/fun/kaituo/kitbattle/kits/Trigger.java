package fun.kaituo.kitbattle.kits;

import fun.kaituo.kitbattle.util.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@SuppressWarnings("unused")
public class Trigger extends PlayerData {
    public Trigger(Player p) {
        super(p);
    }

    @Override
    public void applyPotionEffects(Player p) {
        super.applyPotionEffects(p);
        p.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST,-1,9, false, false));
    }
}