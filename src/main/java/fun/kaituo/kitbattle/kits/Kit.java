package fun.kaituo.kitbattle.kits;

import fun.kaituo.gameutils.util.GameInventory;
import fun.kaituo.kitbattle.KitBattle;
import org.bukkit.entity.Player;

public interface Kit {
    default String getConfigPrefix() {
        return "kits-config." + this.getClass().getSimpleName() + ".";
    }
    default long getCooldownTicks() {
        long coolDownTicks = KitBattle.inst().getConfig().getLong(getConfigPrefix() + "cd");
        // If the entry does not exist, getLong() actually returns 0
        if (coolDownTicks == 0) {
            return -1;
        }
        return coolDownTicks;
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
