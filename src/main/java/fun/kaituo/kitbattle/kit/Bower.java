package fun.kaituo.kitbattle.kit;

import fun.kaituo.kitbattle.util.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@SuppressWarnings("unused")
public class Bower extends PlayerData {
    public Bower(Player p) {
        super(p);
    }

    @Override
    public void applyPotionEffects(Player p) {
        super.applyPotionEffects(p);
        p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, -1, 2, false, false));
    }
}
