package fun.kaituo.kitbattle.kit;

import fun.kaituo.kitbattle.util.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@SuppressWarnings("unused")
public class Ninja extends PlayerData {
    public Ninja(Player p) {
        super(p);
    }

    @Override
    public void applyPotionEffects() {
        super.applyPotionEffects();
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,-1,1, false, false));
    }
}
