package fun.kaituo.kitbattle.kits;

import fun.kaituo.gameutils.util.GameInventory;
import fun.kaituo.kitbattle.KitBattle;
import org.bukkit.entity.Player;

public interface Kit {
    default String getConfigPrefix() {
        return "kits-config." + this.getClass().getSimpleName() + ".";
    }

    @SuppressWarnings("unused")
    default String getConfigString(String key) {
        return KitBattle.inst().getConfig().getString(getConfigPrefix() + key);
    }

    @SuppressWarnings("unused")
    default int getConfigInt(String key) {
        return KitBattle.inst().getConfig().getInt(getConfigPrefix() + key);
    }

    @SuppressWarnings("unused")
    default long getConfigLong(String key) {
        return KitBattle.inst().getConfig().getLong(getConfigPrefix() + key);
    }

    @SuppressWarnings("unused")
    default double getConfigDouble(String key) {
        return KitBattle.inst().getConfig().getDouble(getConfigPrefix() + key);
    }

    default long getCooldownTicks() {
        return getConfigLong("cd");
    }

    default void applyInventory(Player p) {
        GameInventory inv = KitBattle.inst().getInv(this.getClass().getSimpleName());
        if (inv != null) {
            inv.apply(p);
        } else {
            p.sendMessage("§cKit " + this.getClass().getSimpleName() + " not found!");
        }
    }

    default void applyPotionEffects(Player p) {}

    default boolean castSkill(Player p) {
        return false;
    }
}
