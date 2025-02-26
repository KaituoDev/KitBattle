package fun.kaituo.kitbattle.util;

import fun.kaituo.gameutils.util.GameInventory;
import fun.kaituo.kitbattle.KitBattle;
import fun.kaituo.kitbattle.kits.Kit;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import java.util.Collection;

public class PlayerData {
    private final Kit kit;

    private Location location;
    private Collection<PotionEffect> effects;
    private double health;
    private int foodLevel;
    private float saturation;
    private GameInventory inventory;
    private long maxCooldownTicks;
    private long cooldownTicks;

    public PlayerData(Kit kit) {
        this.kit = kit;
        maxCooldownTicks = kit.getCooldownTicks();
        cooldownTicks = 0;
    }

    public Kit getKit() {
        return kit;
    }

    public void tick(Player p) {
        if (cooldownTicks > 0) {
            cooldownTicks -= 1;
        }
        p.setLevel((int) Math.ceil ((double) cooldownTicks / 20));
        p.setExp((1f - (float) cooldownTicks / maxCooldownTicks));
    }

    public void tryCastSkill(Player p) {
        if (cooldownTicks < 0) {
            p.sendMessage("§c你没有技能！");
            return;
        }
        if (cooldownTicks > 0) {
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§4§l技能冷却中！"));
            return;
        }
        if (kit.castSkill(p)) {
            if (KitBattle.inst().isInfiniteFirepower()) {
                maxCooldownTicks = (long) (kit.getCooldownTicks() * (1 - KitBattle.inst().getCooldownReductionMultiplier()));
            } else {
                maxCooldownTicks = kit.getCooldownTicks();
            }
            cooldownTicks = maxCooldownTicks;
        }
    }

    public void save(Player p) {
        location = p.getLocation();
        effects = p.getActivePotionEffects();
        health = p.getHealth();
        foodLevel = p.getFoodLevel();
        saturation = p.getSaturation();
        inventory = new GameInventory(p);
    }

    public void load(Player p) {
        p.teleport(location);
        p.addPotionEffects(effects);
        p.setHealth(health);
        p.setFoodLevel(foodLevel);
        p.setSaturation(saturation);
        inventory.apply(p);
    }
}
