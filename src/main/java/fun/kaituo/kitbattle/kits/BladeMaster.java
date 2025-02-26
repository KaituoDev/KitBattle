package fun.kaituo.kitbattle.kits;

import fun.kaituo.kitbattle.KitBattle;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Set;

import static fun.kaituo.kitbattle.KitBattle.PARTICLE_COUNT;
import static fun.kaituo.kitbattle.KitBattle.SOUND_VOLUME;

@SuppressWarnings("unused")
public class BladeMaster implements Kit {
    private final double skillRadius;
    private final double skillDamage;
    public BladeMaster() {
        FileConfiguration config = KitBattle.inst().getConfig();
        skillRadius = config.getDouble(getConfigPrefix() + "radius");
        skillDamage = config.getDouble(getConfigPrefix() + "damage");
    }

    @Override
    public boolean castSkill(Player p) {
        Set<Player> victims = KitBattle.inst().getNearbyEnemies(p,skillRadius);
        if (victims.isEmpty()) {
            return false;
        }
        for (Player v : victims) {
            v.damage(skillDamage, p);
            World world = p.getWorld();
            world.playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP , SoundCategory.PLAYERS, SOUND_VOLUME, 1);
            world.spawnParticle(Particle.SWEEP_ATTACK, p.getLocation(), PARTICLE_COUNT, 3, 3, 3);
        }
        return true;
    }
}
