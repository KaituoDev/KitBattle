package fun.kaituo.kitbattle.util;

import fun.kaituo.gameutils.util.GameInventory;
import fun.kaituo.kitbattle.KitBattle;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public abstract class PlayerData implements Listener  {
    protected final UUID playerId;
    protected Player p;

    protected Location location;
    protected final Collection<PotionEffect> potionEffects = new ArrayList<>();
    protected double health;
    protected int foodLevel;
    protected float saturation;
    protected GameInventory inventory;
    protected long maxCoolDownTicks;
    protected long coolDownTicks;

    protected final Set<Integer> taskIds = new HashSet<>();

    public PlayerData(Player p) {
        playerId = p.getUniqueId();
        this.p = p;
        maxCoolDownTicks = getConfigLong("cd");
        coolDownTicks = 0;
        applyInventory();
        applyPotionEffects();
        p.setHealth(40);
        Bukkit.getPluginManager().registerEvents(this, KitBattle.inst());
        taskIds.add(Bukkit.getScheduler().scheduleSyncRepeatingTask(KitBattle.inst(), this::tick, 1, 1));
    }

    public void resetPlayer() {
        p.getInventory().clear();
        for (PotionEffect effect : p.getActivePotionEffects()) {
            p.removePotionEffect(effect.getType());
        }
        p.setHealth(20);
        p.setFoodLevel(20);
        p.setSaturation(5);
        p.setExp(0);
        p.setLevel(0);
    }

    public void onDestroy() {
        resetPlayer();
        HandlerList.unregisterAll(this);
        for (int i : taskIds) {
            Bukkit.getScheduler().cancelTask(i);
        }
        taskIds.clear();
    }

    public void applyPotionEffects() {
        p.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, -1, 4, false, false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 60, 0, false, false));
    }

    public void applyInventory() {
        GameInventory inv = KitBattle.inst().getInv(this.getClass().getSimpleName());
        if (inv != null) {
            inv.apply(p);
        } else {
            p.sendMessage("§cKit " + this.getClass().getSimpleName() + " not found!");
        }
    }

    public void tick() {
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

    public void onQuit() {
        location = p.getLocation();
        potionEffects.clear();
        potionEffects.addAll(p.getActivePotionEffects());
        health = p.getHealth();
        foodLevel = p.getFoodLevel();
        saturation = p.getSaturation();
        inventory = new GameInventory(p);
        resetPlayer();
        HandlerList.unregisterAll(this);
        for (int i : taskIds) {
            Bukkit.getScheduler().cancelTask(i);
        }
        taskIds.clear();
        p = null;
    }

    public void onRejoin() {
        Player p = Bukkit.getPlayer(playerId);
        assert p != null;
        this.p = p;
        p.teleport(location);
        p.addPotionEffects(potionEffects);
        p.setHealth(health);
        p.setFoodLevel(foodLevel);
        p.setSaturation(saturation);
        inventory.apply(p);
        Bukkit.getPluginManager().registerEvents(this, KitBattle.inst());
        taskIds.add(Bukkit.getScheduler().scheduleSyncRepeatingTask(KitBattle.inst(), this::tick, 1, 1));
    }

    @EventHandler
    public void onPlayerTryCastSkill(PlayerInteractEvent e) {
        if (!e.getPlayer().getUniqueId().equals(playerId)) {
            return;
        }
        if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK) && !e.getAction().equals(Action.RIGHT_CLICK_AIR)) {
            return;
        }
        ItemStack item = e.getItem();
        if (item == null) {
            return;
        }
        if (item.getItemMeta() == null) {
            return;
        }
        // We use fortune enchantment to identify skill items
        if (!item.getItemMeta().hasEnchant(Enchantment.FORTUNE)) {
            return;
        }
        tryCastSkill();
    }

    public void tryCastSkill() {
        if (maxCoolDownTicks == 0) {
            p.sendMessage("§c你没有技能！");
            return;
        }
        if (coolDownTicks > 0) {
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§c§l技能冷却中！"));
            return;
        }
        if (castSkill()) {
            if (KitBattle.inst().isInfiniteFirepower()) {
                maxCoolDownTicks = (long) (getConfigLong("cd") * (1 - KitBattle.inst().getCooldownReductionMultiplier()));
            } else {
                maxCoolDownTicks = getConfigLong("cd");
            }
            coolDownTicks = maxCoolDownTicks;
        }
    }

    public boolean castSkill() {
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

