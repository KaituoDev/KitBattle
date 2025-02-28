package fun.kaituo.kitbattle.util;

import fun.kaituo.gameutils.util.GameInventory;
import fun.kaituo.kitbattle.KitBattle;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collection;

public abstract class PlayerData {
    protected Location location;
    protected final Collection<PotionEffect> potionEffects = new ArrayList<>();
    protected double health;
    protected int foodLevel;
    protected float saturation;
    protected GameInventory inventory;
    protected final long maxCoolDownTicks;
    protected long coolDownTicks;

    public PlayerData(Player p) {
        maxCoolDownTicks = getConfigLong("cd");
        coolDownTicks = 0;
        applyInventory(p);
        applyPotionEffects(p);
    }

    public void destroy(Player p) {}

    public void applyPotionEffects(Player p) {
        p.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, -1, 4, false, false));
        // Give player glowing and regeneration for 3 seconds
        p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 60, 0, false, false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 9, false, false));
    }

    public void applyInventory(Player p) {
        GameInventory inv = KitBattle.inst().getInv(this.getClass().getSimpleName());
        if (inv != null) {
            inv.apply(p);
        } else {
            p.sendMessage("§cKit " + this.getClass().getSimpleName() + " not found!");
        }
    }

    public void tick(Player p) {
        if (maxCoolDownTicks == 0) {
            p.setLevel(0);
            p.setExp(0);
            return;
        }
        if (coolDownTicks > 0) {
            coolDownTicks -= 1;
        }
        p.setLevel((int) Math.ceil ((double) coolDownTicks / 20));
        p.setExp((1f - (float) coolDownTicks / maxCoolDownTicks));
    }

    public void quit(Player p) {
        location = p.getLocation();
        potionEffects.clear();
        potionEffects.addAll(p.getActivePotionEffects());
        health = p.getHealth();
        foodLevel = p.getFoodLevel();
        saturation = p.getSaturation();
        inventory = new GameInventory(p);
    }

    public void rejoin(Player p) {
        p.teleport(location);
        p.addPotionEffects(potionEffects);
        p.setHealth(health);
        p.setFoodLevel(foodLevel);
        p.setSaturation(saturation);
        inventory.apply(p);
    }

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
            if (KitBattle.inst().isInfiniteFirepower()) {
                coolDownTicks = (long) (maxCoolDownTicks * (1 - KitBattle.inst().getCooldownReductionMultiplier()));
            } else {
                coolDownTicks = maxCoolDownTicks;
            }
        }
    }

    public boolean castSkill(Player p) {
        return false;
    }

    public String getConfigPrefix() {
        return "kits-config." + this.getClass().getSimpleName() + ".";
    }

    @SuppressWarnings("unused")
    public String getConfigString(String key) {
        return KitBattle.inst().getConfig().getString(getConfigPrefix() + key);
    }

    @SuppressWarnings("unused")
    public int getConfigInt(String key) {
        return KitBattle.inst().getConfig().getInt(getConfigPrefix() + key);
    }

    @SuppressWarnings("unused")
    public long getConfigLong(String key) {
        return KitBattle.inst().getConfig().getLong(getConfigPrefix() + key);
    }

    @SuppressWarnings("unused")
    public double getConfigDouble(String key) {
        return KitBattle.inst().getConfig().getDouble(getConfigPrefix() + key);
    }
}
