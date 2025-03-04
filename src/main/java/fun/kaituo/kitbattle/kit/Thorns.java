package fun.kaituo.kitbattle.kit;

import fun.kaituo.kitbattle.util.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@SuppressWarnings("unused")
public class Thorns extends PlayerData {
    public Thorns(Player p) {
        super(p);
    }

    @Override
    public void applyPotionEffects() {
        super.applyPotionEffects();
        p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, -1, 0, false, false));
    }
}
