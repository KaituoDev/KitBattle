package fun.kaituo.kitbattle.kit;

import fun.kaituo.kitbattle.util.PlayerData;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class Sounds extends PlayerData{
    private final Random random = new Random();
    // 定义每种药水效果的持续时间和概率
    private final Map<PotionEffectType, Integer> effectDurations = new HashMap<>();
    private final Map<PotionEffectType, Integer> effectProbabilities = new HashMap<>();
    private final List<PotionEffectType> availableEffects;

    public Sounds(Player p) {
        super(p);

        // 设置药水效果的持续时间（单位：tick，20 ticks = 1秒）
        effectDurations.put(PotionEffectType.SLOWNESS, 40);
        effectDurations.put(PotionEffectType.GLOWING, 100);
        effectDurations.put(PotionEffectType.POISON, 120);
        effectDurations.put(PotionEffectType.INSTANT_DAMAGE, 0);
        effectDurations.put(PotionEffectType.DARKNESS, 40);
        effectDurations.put(PotionEffectType.WEAKNESS, 40);
        effectDurations.put(PotionEffectType.INFESTED, 200);
        effectDurations.put(PotionEffectType.WITHER, 100);
        effectDurations.put(PotionEffectType.LEVITATION, 60);
        effectDurations.put(PotionEffectType.REGENERATION, 100);
        effectDurations.put(PotionEffectType.RESISTANCE, 100);
        effectDurations.put(PotionEffectType.HASTE, 100);

        // 设置药水效果的出现概率（权重，数字越大，概率越高）
        effectProbabilities.put(PotionEffectType.SLOWNESS, 10);
        effectProbabilities.put(PotionEffectType.GLOWING, 10);
        effectProbabilities.put(PotionEffectType.POISON, 10);
        effectProbabilities.put(PotionEffectType.INSTANT_DAMAGE, 20);
        effectProbabilities.put(PotionEffectType.DARKNESS, 10);
        effectProbabilities.put(PotionEffectType.WEAKNESS, 10);
        effectProbabilities.put(PotionEffectType.INFESTED, 5);
        effectProbabilities.put(PotionEffectType.WITHER, 5);
        effectProbabilities.put(PotionEffectType.LEVITATION, 5);
        effectProbabilities.put(PotionEffectType.REGENERATION, 10);
        effectProbabilities.put(PotionEffectType.RESISTANCE, 5);
        effectProbabilities.put(PotionEffectType.HASTE, 5);

        // 初始化可用的药水效果列表
        availableEffects = Arrays.asList(
                PotionEffectType.SLOWNESS,
                PotionEffectType.GLOWING,
                PotionEffectType.POISON,
                PotionEffectType.INSTANT_DAMAGE,
                PotionEffectType.DARKNESS,
                PotionEffectType.WEAKNESS,
                PotionEffectType.INFESTED,
                PotionEffectType.WITHER,
                PotionEffectType.LEVITATION,
                PotionEffectType.REGENERATION,
                PotionEffectType.RESISTANCE,
                PotionEffectType.HASTE
        );
    }

    @EventHandler
    public void onPlayerShootArrow(EntityShootBowEvent event) {
        if (!event.getEntity().getUniqueId().equals(playerId)) {
            return;
        }
        // 确保玩家持有弓，并射出了箭
        if (event.getBow() != null && event.getBow().getType() == Material.BOW) {
            Arrow arrow = (Arrow) event.getProjectile();
            applyRandomPotionEffect(arrow);
        }
    }

    // 随机从可用的效果中选择一个，并根据概率设置持续时间
    private void applyRandomPotionEffect(Arrow arrow) {
        // 根据概率选择一个药水效果
        PotionEffectType selectedEffect = getRandomEffectBasedOnProbability();

        // 获取该效果的持续时间
        int duration = effectDurations.get(selectedEffect);
        int amplifier = 1; // 药水等级 (例如1)

        // 将选中的药水效果添加到箭上
        arrow.addCustomEffect(new PotionEffect(selectedEffect, duration, amplifier, false, false), true);
    }

    // 根据每个效果的概率权重来选择效果
    private PotionEffectType getRandomEffectBasedOnProbability() {
        // 计算总概率
        int totalProbability = effectProbabilities.values().stream().mapToInt(Integer::intValue).sum();

        // 生成一个随机数
        int randomNum = random.nextInt(totalProbability);

        // 根据随机数来确定选择的效果
        int cumulativeProbability = 0;
        for (PotionEffectType effect : availableEffects) {
            cumulativeProbability += effectProbabilities.get(effect);
            if (randomNum < cumulativeProbability) {
                return effect;
            }
        }
        // 默认返回第一个效果（应该永远不会到达这里）
        return availableEffects.getFirst();
    }
}
