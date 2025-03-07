package fun.kaituo.kitbattle.kit;

import fun.kaituo.kitbattle.KitBattle;
import fun.kaituo.kitbattle.util.PlayerData;
import org.bukkit.*;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;

public class Sounds extends PlayerData {
    private final Random random = new Random();
    private final Map<Arrow, PotionEffectType> arrowEffects = new HashMap<>();
    private final Map<PotionEffectType, Integer> effectDurations = new HashMap<>();
    private final Map<PotionEffectType, Integer> effectProbabilities = new HashMap<>();
    private final Map<PotionEffectType, Color> effectColors = new HashMap<>();
    private final List<PotionEffectType> availableEffects;

    public Sounds(Player p) {
        super(p);

        effectDurations.put(PotionEffectType.SLOWNESS, 40);
        effectDurations.put(PotionEffectType.GLOWING, 100);
        effectDurations.put(PotionEffectType.POISON, 120);
        effectDurations.put(PotionEffectType.INSTANT_DAMAGE, 0);
        effectDurations.put(PotionEffectType.DARKNESS, 40);
        effectDurations.put(PotionEffectType.WEAKNESS, 40);
        effectDurations.put(PotionEffectType.WITHER, 100);
        effectDurations.put(PotionEffectType.LEVITATION, 60);
        effectDurations.put(PotionEffectType.REGENERATION, 100);
        effectDurations.put(PotionEffectType.RESISTANCE, 100);
        effectDurations.put(PotionEffectType.HASTE, 100);

        effectProbabilities.put(PotionEffectType.SLOWNESS, 10);
        effectProbabilities.put(PotionEffectType.GLOWING, 10);
        effectProbabilities.put(PotionEffectType.POISON, 10);
        effectProbabilities.put(PotionEffectType.INSTANT_DAMAGE, 20);
        effectProbabilities.put(PotionEffectType.DARKNESS, 10);
        effectProbabilities.put(PotionEffectType.WEAKNESS, 10);
        effectProbabilities.put(PotionEffectType.WITHER, 5);
        effectProbabilities.put(PotionEffectType.LEVITATION, 5);
        effectProbabilities.put(PotionEffectType.REGENERATION, 10);
        effectProbabilities.put(PotionEffectType.RESISTANCE, 5);
        effectProbabilities.put(PotionEffectType.HASTE, 5);

        //effectColors.put(PotionEffectType.SLOWNESS, Color.fromRGB(112, 128, 144));
        //effectColors.put(PotionEffectType.GLOWING, Color.fromRGB(255, 255, 0));
        //effectColors.put(PotionEffectType.POISON, Color.fromRGB(50, 205, 50));
        //effectColors.put(PotionEffectType.INSTANT_DAMAGE, Color.fromRGB(178, 34, 34));
        //effectColors.put(PotionEffectType.DARKNESS, Color.fromRGB(79, 79, 79));
        //effectColors.put(PotionEffectType.WEAKNESS, Color.fromRGB(156, 156, 156));
        //effectColors.put(PotionEffectType.WITHER, Color.fromRGB(0, 0, 0));
        //effectColors.put(PotionEffectType.LEVITATION, Color.fromRGB(240, 255, 255));
        //effectColors.put(PotionEffectType.REGENERATION, Color.fromRGB(255, 20, 147));
        //effectColors.put(PotionEffectType.RESISTANCE, Color.fromRGB(138, 43, 226));
        //effectColors.put(PotionEffectType.HASTE, Color.fromRGB(205, 205, 0));



        availableEffects = new ArrayList<>(effectDurations.keySet());
    }

    @EventHandler
    public void onPlayerShootArrow(EntityShootBowEvent event) {
        if (!event.getEntity().getUniqueId().equals(playerId)) {
            return;
        }

        if (event.getBow() != null && event.getBow().getType() == Material.BOW) {
            Arrow arrow = (Arrow) event.getProjectile();

            PotionEffectType selectedEffect = applyRandomPotionEffect(arrow);
            arrowEffects.put(arrow, selectedEffect);
            startArrowParticleTask(arrow, effectColors.get(selectedEffect));
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Arrow) {
            Arrow arrow = (Arrow) event.getEntity();
            arrowEffects.remove(arrow);
        }
    }

    private PotionEffectType applyRandomPotionEffect(Arrow arrow) {
        PotionEffectType selectedEffect = getRandomEffectBasedOnProbability();
        int duration = effectDurations.get(selectedEffect);
        int amplifier = 1;

        // **使用 PotionMeta 设置箭矢的 PotionType**
        ItemStack potionItem = new ItemStack(Material.TIPPED_ARROW);
        PotionMeta potionMeta = (PotionMeta) potionItem.getItemMeta();
        if (potionMeta != null) {
            potionMeta.addCustomEffect(new PotionEffect(selectedEffect, duration, amplifier, false, false), true);
            potionItem.setItemMeta(potionMeta);
        }

        // **应用药水数据到箭矢**
        arrow.setItem(potionItem);
        arrow.addCustomEffect(new PotionEffect(selectedEffect, duration, amplifier, false, true), true);

        return selectedEffect;
    }

    private PotionEffectType getRandomEffectBasedOnProbability() {
        int totalProbability = effectProbabilities.values().stream().mapToInt(Integer::intValue).sum();
        int randomNum = random.nextInt(totalProbability);
        int cumulativeProbability = 0;

        for (PotionEffectType effect : availableEffects) {
            cumulativeProbability += effectProbabilities.get(effect);
            if (randomNum < cumulativeProbability) {
                return effect;
            }
        }
        return availableEffects.get(0);
    }

    private void startArrowParticleTask(Arrow arrow, Color color) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (arrow.isDead() || arrow.isOnGround()) {
                    this.cancel();
                    return;
                }

                World world = arrow.getWorld();
                Location loc = arrow.getLocation();

                // **生成自定义颜色粒子**
                //world.spawnParticle(Particle.DUST, loc, 10, new Particle.DustOptions(color, 1));
            }
        }.runTaskTimer(KitBattle.inst(), 0L, 2L);
    }
}
