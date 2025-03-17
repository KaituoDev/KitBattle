package fun.kaituo.kitbattle.kit;

import fun.kaituo.kitbattle.KitBattle;
import fun.kaituo.kitbattle.util.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Set;

import static fun.kaituo.kitbattle.KitBattle.PARTICLE_COUNT;

public class Blueblood extends PlayerData {
    private final double HEAL_PERCENTAGE;
    private final double SKILL_HEAL_PERCENTAGE;
    private final double RADIUS;
    private final double MAX_HEALTH = 40.0;
    private static final double PARTICLE_SPEED = 0.5;

    public Blueblood(Player p) {
        super(p);
        HEAL_PERCENTAGE = getConfigDouble("heal-percentage");
        SKILL_HEAL_PERCENTAGE = getConfigDouble("skill-heal-percentage");
        RADIUS = getConfigDouble("radius");
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!event.getDamager().getType().equals(EntityType.PLAYER) || !(event.getDamager() instanceof Player)) {
            return;
        }
        Player damager = (Player) event.getDamager();
        if (!damager.equals(p.getPlayer())) {
            return;
        }
        double healAmount = event.getDamage() * HEAL_PERCENTAGE;
        double newHealth = Math.min(damager.getHealth() + healAmount, MAX_HEALTH);
        damager.setHealth(newHealth);
    }

    @Override
    public boolean castSkill() {
        Set<Player> enemies = KitBattle.inst().getNearbyEnemies(p, RADIUS);
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
        double lostHealth = MAX_HEALTH - p.getHealth();
        double healAmount = SKILL_HEAL_PERCENTAGE * enemyCount * lostHealth;
        double newHealth = Math.min(p.getHealth() + healAmount, MAX_HEALTH);
        p.setHealth(newHealth);
        // 为玩家提供生命回复效果
        p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 0, false, false));
        return true;
    }

    private void spawnBloodEffect(Player target, Player caster) {
        new BukkitRunnable() {
            Vector particleLocation = target.getLocation().toVector();
            int steps = 0;

            @Override
            public void run() {
                if (steps >= PARTICLE_COUNT) {
                    cancel();
                    return;
                }

                // 获取施法者实时位置
                Vector casterLocation = caster.getLocation().toVector();

                // 计算新的方向（朝向施法者的方向）
                Vector direction = casterLocation.subtract(particleLocation).normalize();

                // 让粒子沿着施法者实时位置飞行
                particleLocation.add(direction.multiply(PARTICLE_SPEED));

                // 生成红石粒子
                target.getWorld().spawnParticle(
                        Particle.DUST,
                        particleLocation.toLocation(target.getWorld()),
                        1,
                        new Particle.DustOptions(org.bukkit.Color.RED, 1)
                );

                steps++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("KitBattle"), 0, 1);
    }
}
