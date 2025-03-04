package fun.kaituo.kitbattle.kit;

import fun.kaituo.kitbattle.util.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@SuppressWarnings("unused")
public class Arno extends PlayerData {
    public Arno(Player p) {
        super(p);
    }

    public void applyPotionEffects() {
        super.applyPotionEffects();
        p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, -1, 0));
        p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, -1, 1));
        p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, -1, 0));
        p.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, -1, 0));
        p.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, -1, 0));
        p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, -1, 0));
        p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, -1, 0));
        p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, -1, 0));
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, -1, 0));
        p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, -1, 1));
    }
}