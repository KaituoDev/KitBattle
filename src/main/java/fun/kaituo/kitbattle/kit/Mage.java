package fun.kaituo.kitbattle.kit;

import fun.kaituo.kitbattle.util.PlayerData;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ExplosionPrimeEvent;

import java.util.HashSet;
import java.util.Set;

import static fun.kaituo.kitbattle.KitBattle.PARTICLE_COUNT;
import static fun.kaituo.kitbattle.KitBattle.SOUND_VOLUME;

@SuppressWarnings("unused")
public class Mage extends PlayerData implements Listener {
    private final Set<Fireball> flyingFireballs = new HashSet<>();
    private final float explosionPower;

    public Mage(Player p) {
        super(p);
        explosionPower = (float) getConfigDouble("explosion-power");
    }

    private void clearFireballs() {
        for (Fireball fireball : flyingFireballs) {
            fireball.remove();
        }
        flyingFireballs.clear();
    }

    @Override
    public void onDestroy() {
        clearFireballs();
        super.onDestroy();
    }

    @Override
    public void onQuit() {
        clearFireballs();
        super.onQuit();
    }

    @Override
    public void onRejoin() {
        super.onRejoin();
    }

    @Override
    public boolean castSkill() {
        World world = p.getWorld();
        Location startLoc = p.getEyeLocation().clone().add(p.getEyeLocation().getDirection());
        Fireball fireball = (Fireball) world.spawnEntity(startLoc, EntityType.FIREBALL, false);
        fireball.setShooter(p);
        flyingFireballs.add(fireball);
        world.playSound(p.getLocation(), Sound.ENTITY_GHAST_SHOOT, SoundCategory.PLAYERS, SOUND_VOLUME, 1);
        world.spawnParticle(Particle.WITCH, p.getLocation(), PARTICLE_COUNT, 3, 3, 3);
        return true;
    }

    @EventHandler
    public void onFireballExplode(ExplosionPrimeEvent e) {
        for (Fireball fireball : flyingFireballs) {
            if (fireball.equals(e.getEntity())) {
                e.setCancelled(true);
                fireball.getWorld().createExplosion(
                        fireball.getLocation(),
                        explosionPower,
                        false,
                        false,
                        (Entity) fireball.getShooter());
                fireball.remove();
                flyingFireballs.remove(fireball);
                return;
            }
        }
    }
}
