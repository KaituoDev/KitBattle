package fun.kaituo.kitbattle.kits;

import fun.kaituo.kitbattle.util.PlayerData;
import org.bukkit.entity.Player;

@SuppressWarnings("unused")
public class WhiteWolf extends PlayerData {
    private final float explosionMinPower;
    private final float explosionMaxPower;
    private final double healthCost;
    public WhiteWolf(Player p) {
        super(p);
        explosionMinPower = (float) getConfigDouble("explosion-min-power");
        explosionMaxPower = (float) getConfigDouble("explosion-max-power");
        healthCost = getConfigDouble("health-cost");
    }

    @Override
    public boolean castSkill(Player p) {
        float healthPercentage = (float) (p.getHealth() / 40);
        p.setHealth(p.getHealth() * (1 - healthCost));
        float power = explosionMinPower + (explosionMaxPower - explosionMinPower) * (1 - healthPercentage);
        p.getWorld().createExplosion(p.getLocation(), power, false, false, p);
        p.damage(0.1);
        return true;
    }
}
