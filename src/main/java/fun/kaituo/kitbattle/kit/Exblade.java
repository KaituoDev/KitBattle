package fun.kaituo.kitbattle.kit;

import fun.kaituo.gameutils.util.GameInventory;
import fun.kaituo.kitbattle.KitBattle;
import fun.kaituo.kitbattle.util.PlayerData;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class Exblade extends PlayerData {
    private static final Set<Material> SWORD_MATERIALS = Set.of(
            Material.WOODEN_SWORD,
            Material.STONE_SWORD,
            Material.IRON_SWORD,
            Material.GOLDEN_SWORD,
            Material.DIAMOND_SWORD,
            Material.NETHERITE_SWORD
    );

    private static final ItemStack[] SWORDS;

    static {
        GameInventory inv = KitBattle.inst().getInv("Blades");
        if (inv == null) {
            throw new RuntimeException("Failed to load inventory: Blades is null!");
        }

        SWORDS = new ItemStack[]{
                inv.getHotbar(0),
                inv.getHotbar(1),
                inv.getHotbar(2),
                inv.getHotbar(3)
        };

        for (int i = 0; i < SWORDS.length; i++) {
            if (SWORDS[i] == null) {
                throw new RuntimeException("Failed to load sword at index " + i);
            }
        }
    }

    public Exblade(Player p) {
        super(p);
    }

    public boolean castSkill(Player p) {
        boolean hasSword = false;
        for (int i = 0; i < p.getInventory().getSize(); i++) {
            ItemStack item = p.getInventory().getItem(i);
            if (item != null && isSword(item.getType())) {
                p.getInventory().setItem(i, getRandomSword());
                hasSword = true;
            }
        }
        return hasSword;
    }

    private boolean isSword(Material material) {
        return SWORD_MATERIALS.contains(material);
    }

    private ItemStack getRandomSword() {
        return SWORDS[ThreadLocalRandom.current().nextInt(SWORDS.length)].clone();
    }
}
