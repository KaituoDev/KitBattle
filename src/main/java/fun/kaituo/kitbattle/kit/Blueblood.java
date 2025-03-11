package fun.kaituo.kitbattle.kit;

import fun.kaituo.kitbattle.KitBattle;
import fun.kaituo.kitbattle.util.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

import static fun.kaituo.kitbattle.KitBattle.PARTICLE_COUNT;

public class Blueblood extends PlayerData {
    private final double HEAL_PERCENTAGE;
    private final double SKILL_HEAL_PERCENTAGE;
    private final double RADIUS;

    public Blueblood(Player p) {
        super(p);
        HEAL_PERCENTAGE = getConfigDouble("heal-percentage");
        SKILL_HEAL_PERCENTAGE = getConfigDouble("skill-heal-percentage");
        RADIUS = getConfigDouble("radius");
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        Player damager = (Player) event.getDamager();
        if (!damager.equals(p.getPlayer())) {
            return;
        }
        double healAmount = event.getDamage() * HEAL_PERCENTAGE;
        double newHealth = Math.min(damager.getHealth() + healAmount, damager.getMaxHealth());
        damager.setHealth(newHealth);
    }

    public boolean castSkill(Player p) {
        Set<Player> enemies = KitBattle.inst().getNearbyEnemies(p, RADIUS);
        double totalHealAmount = 0;
        int enemyCount = 0;
        if (enemies.isEmpty()) {
            return false;
        }
        // 查找技能范围内的敌人
        for (Player target : enemies) {
                    // 造成伤害
                    target.damage(4.0); // 可以调整伤害数值
                    enemyCount++;
                    spawnBloodEffect(target, p);

        }
        double lostHealth = p.getMaxHealth() - p.getHealth();
        double healAmount = SKILL_HEAL_PERCENTAGE * enemyCount * lostHealth;
        double newHealth = Math.min(p.getHealth() + healAmount, p.getMaxHealth());
        p.setHealth(newHealth);
        // 为玩家提供生命回复效果
        p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 0, false, false));
        return  true;
    }
    private void spawnBloodEffect(Player target, Player caster) {
        Vector direction = caster.getLocation().toVector().subtract(target.getLocation().toVector()).normalize();
        new BukkitRunnable() {
            int steps = 0;
            Vector particleLocation = target.getLocation().toVector();
            double totalDistance = particleLocation.distance(caster.getLocation().toVector()); // 修复：使用 Vector 计算距离
            double stepDistance = totalDistance / PARTICLE_COUNT;

            @Override
            public void run() {
                if (steps >= PARTICLE_COUNT) {
                    cancel();
                    return;
                }

                // 计算当前粒子位置
                Vector currentPosition = particleLocation.clone().add(direction.clone().multiply(stepDistance * steps));

                // 生成红石粒子
                target.getWorld().spawnParticle(Particle.DUST, currentPosition.toLocation(target.getWorld()), 1,
                        new Particle.DustOptions(org.bukkit.Color.RED, 1));

                steps++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("KitBattle"), 0, 1);
    }
}
