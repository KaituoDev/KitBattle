package fun.kaituo.kitbattle.kit;

import fun.kaituo.kitbattle.KitBattle;
import fun.kaituo.kitbattle.util.PlayerData;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Set;

import static fun.kaituo.kitbattle.KitBattle.PARTICLE_COUNT;
import static fun.kaituo.kitbattle.KitBattle.SOUND_VOLUME;

@SuppressWarnings("unused")
public class BladeMaster extends PlayerData {
    private final double skillRadius;
    private final double skillDamage;
    public BladeMaster(Player p) {
        super(p);
        skillRadius = getConfigDouble("radius");
        skillDamage = getConfigDouble("damage");
    }

    @Override
    public boolean castSkill() {
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
