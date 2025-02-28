package fun.kaituo.kitbattle.kits;

import fun.kaituo.kitbattle.util.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@SuppressWarnings("unused")
public class CaveMan extends PlayerData {
    public CaveMan(Player p) {
        super(p);
    }
    @Override
    public void applyPotionEffects(Player p) {
        super.applyPotionEffects(p);
        p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, -1, 1, false, false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, -1, 0, false, false));
    }
}
