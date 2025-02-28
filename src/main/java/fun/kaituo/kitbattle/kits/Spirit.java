package fun.kaituo.kitbattle.kits;

import fun.kaituo.kitbattle.util.PlayerData;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import static fun.kaituo.kitbattle.KitBattle.PARTICLE_COUNT;
import static fun.kaituo.kitbattle.KitBattle.SOUND_VOLUME;

@SuppressWarnings("unused")
public class Spirit extends PlayerData {
    public Spirit(Player p) {
        super(p);
    }

    @Override
    public boolean castSkill(Player p) {
        World world = p.getWorld();
        p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, -1, 1, false, false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, -1, 1));
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, -1, 1));
        world.playSound(p.getLocation(), Sound.ENTITY_WITHER_AMBIENT , SoundCategory.PLAYERS, SOUND_VOLUME, 1);
        world.spawnParticle(Particle.ANGRY_VILLAGER, p.getLocation(), PARTICLE_COUNT, 3, 3, 3);
        return true;
    }

}
