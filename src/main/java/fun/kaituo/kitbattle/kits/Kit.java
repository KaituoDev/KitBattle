package fun.kaituo.kitbattle.kits;

import fun.kaituo.gameutils.util.GameInventory;
import fun.kaituo.kitbattle.KitBattle;
import org.bukkit.entity.Player;

public interface Kit {

    default long getCooldownTicks() {
        return -1;
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
