package fun.kaituo.kitbattle.kit;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import fun.kaituo.kitbattle.KitBattle;
import fun.kaituo.kitbattle.util.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@SuppressWarnings("unused")
public class StormKnight extends PlayerData {
    private final double firstDamage;
    private final double secondDamage;
    private final double thirdDamage;
    private boolean isCastingSkill = false;

    public StormKnight(Player p) {
        super(p);
        firstDamage = getConfigDouble("first-damage");
        secondDamage = getConfigDouble("second-damage");
        thirdDamage = getConfigDouble("third-damage");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isCastingSkill = false;
    }

    @Override
    public void onQuit() {
        super.onQuit();
        isCastingSkill = false;
    }

    @EventHandler
    public void preventJump(PlayerJumpEvent e) {
        if (!e.getPlayer().getUniqueId().equals(playerId)) {
            return;
        }
        if (isCastingSkill) {
            e.setCancelled(true);
        }
    }

    @Override
    public boolean castSkill() {
        isCastingSkill = true;
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 2, false, false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 2, false, false));
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 10, 0);

        taskIds.add(Bukkit.getScheduler().runTaskLater(KitBattle.inst(), () -> {
            for (Player v : KitBattle.inst().getNearbyEnemies(p, 3)) {
                v.damage(firstDamage, p);
            }
            p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation(), 50, 2, 2, 2);
        }, 0).getTaskId());

        taskIds.add(Bukkit.getScheduler().runTaskLater(KitBattle.inst(), () -> {
            for (Player v : KitBattle.inst().getNearbyEnemies(p, 3)) {
                v.damage(secondDamage, p);
            }
            p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation(), 50, 2, 2, 2);
        }, 20).getTaskId());

        taskIds.add(Bukkit.getScheduler().runTaskLater(KitBattle.inst(), () -> {
            for (Player v : KitBattle.inst().getNearbyEnemies(p, 3)) {
                v.damage(thirdDamage, p);
            }
            p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation(), 50, 2, 2, 2);
        }, 40).getTaskId());

        taskIds.add(Bukkit.getScheduler().runTaskLater(KitBattle.inst(), () -> isCastingSkill = false, 60).getTaskId());

        return true;
    }
}
