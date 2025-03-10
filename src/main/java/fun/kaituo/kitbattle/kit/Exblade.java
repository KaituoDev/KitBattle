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
    private GameInventory inventory;
    private ItemStack BREACH;
    private ItemStack SHARPNESS;
    private ItemStack FIRE;
    private ItemStack POISON;

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
        try {
            GameInventory inv = KitBattle.inst().getInv("Blades");
            SWORDS = new ItemStack[]{
                    inv.getHotbar(0),
                    inv.getHotbar(1),
                    inv.getHotbar(2),
                    inv.getHotbar(3)
            };
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize SWORDS array", e);
        }
    }

    public Exblade(Player p) {
        super(p);
        try {
            this.inventory = KitBattle.inst().getInv("Blades");
            this.BREACH = inventory.getHotbar(0);
            this.SHARPNESS = inventory.getHotbar(1);
            this.FIRE = inventory.getHotbar(2);
            this.POISON = inventory.getHotbar(3);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize inventory", e);
        }
    }


    

    @Override
    public void castSkill(Player p) {
        ItemStack[] contents = p.getInventory().getContents();
        boolean hasSword = false;

        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null && isSword(contents[i].getType())) {
                contents[i] = getRandomSword();
                hasSword = true;
            }
        }

        if (hasSword) {
            p.getInventory().setContents(contents);
        }
    }

    private boolean isSword(Material material) {
        return SWORD_MATERIALS.contains(material);
    }

    private ItemStack getRandomSword() {
        return SWORDS[ThreadLocalRandom.current().nextInt(SWORDS.length)].clone();
    }
}
