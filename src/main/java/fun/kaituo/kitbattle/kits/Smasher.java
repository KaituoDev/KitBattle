package fun.kaituo.kitbattle.kits;

import fun.kaituo.kitbattle.KitBattle;
import fun.kaituo.kitbattle.util.PlayerData;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

@SuppressWarnings("unused")
public class Smasher extends PlayerData {
    private final float explosionPower;
    private int remainingFlyTime = -1;
    private boolean hasLanded = true;

    public Smasher(Player p) {
        super(p);
        explosionPower = (float) getConfigDouble("explosion-power");
    }

    private boolean isOnGround(Player p) {
        Block block1 = p.getLocation().subtract(-0.3, 1, -0.3).getBlock();
        Block block2 = p.getLocation().subtract(0.3, 1, -0.3).getBlock();
        Block block3 = p.getLocation().subtract(-0.3, 1, 0.3).getBlock();
        Block block4 = p.getLocation().subtract(0.3, 1, 0.3).getBlock();
        Block block5 = p.getLocation().subtract(-0.3, 0, -0.3).getBlock();
        Block block6 = p.getLocation().subtract(0.3, 0, -0.3).getBlock();
        Block block7 = p.getLocation().subtract(-0.3, 0, 0.3).getBlock();
        Block block8 = p.getLocation().subtract(0.3, 0, 0.3).getBlock();
        return block1.getType().isSolid() || block2.getType().isSolid() || block3.getType().isSolid() || block4.getType().isSolid() ||
            block5.getType().isSolid() || block6.getType().isSolid() || block7.getType().isSolid() || block8.getType().isSolid();
    }

    private boolean tryExplode(Player p) {
        if (isOnGround(p) && !hasLanded) {
            hasLanded = true;
            remainingFlyTime = 0;
            p.getWorld().createExplosion(p.getLocation(), explosionPower, false, false, p);
            if (KitBattle.inst().isInfiniteFirepower()) {
                maxCoolDownTicks = (long) (getConfigLong("cd") * (1 - KitBattle.inst().getCooldownReductionMultiplier()));
            } else {
                maxCoolDownTicks = getConfigLong("cd");
            }
            coolDownTicks = maxCoolDownTicks;
            return true;
        }
        return false;
    }

    @Override
    public void quit(Player p) {
        super.quit(p);
        p.setAllowFlight(false);
        p.setFlySpeed(0.1f);
    }

    @Override
    public void destroy(Player p) {
        super.destroy(p);
        p.setAllowFlight(false);
        p.setFlySpeed(0.1f);
    }

    @Override
    public void tick(Player p) {
        super.tick(p);
        if (remainingFlyTime > 40) {
            Vector v = p.getVelocity().clone();
            v.setY(5000);
            p.setVelocity(v);
            remainingFlyTime -= 1;
            p.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 2, 99));
            p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 2, 9));
        } else if (remainingFlyTime > 0) {
            if (tryExplode(p)) {
                return;
            }
            remainingFlyTime -= 1;
            p.setAllowFlight(true);
            p.setFlying(true);
            p.setFlySpeed(0.25f);
            p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 2, 9));
        } else if (remainingFlyTime == 0) {
            remainingFlyTime -= 1;
            p.setAllowFlight(false);
            p.setFlySpeed(0.1f);
            Vector v = p.getVelocity().clone();
            v.setY(-20000);
            p.setVelocity(v);
            p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 30, 4, false, false));
        } else {
            tryExplode(p);
        }
    }

    @Override
    public void tryCastSkill(Player p) {
        if (maxCoolDownTicks == 0) {
            p.sendMessage("§c你没有技能！");
            return;
        }
        if (coolDownTicks > 0) {
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§c§l技能冷却中！"));
            return;
        }
        if (castSkill(p)) {
            maxCoolDownTicks = 100000;
            coolDownTicks = maxCoolDownTicks;
        }
    }

    @Override
    public boolean castSkill(Player p) {
        if (!hasLanded) {
            return false;
        }
        remainingFlyTime = 60;
        hasLanded = false;
        Location loc = p.getLocation();
        loc.setY(105);
        p.teleport(loc);
        return true;
    }

}
