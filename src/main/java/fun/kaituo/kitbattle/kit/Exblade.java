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
        SWORDS = new ItemStack[]{
                inv.getHotbar(0),
                inv.getHotbar(1),
                inv.getHotbar(2),
                inv.getHotbar(3)
        };
    }

    public Exblade(Player p) {
        super(p);
    }

    public boolean castSkill(Player p) {
        ItemStack[] contents = p.getInventory().getContents();  // 获取玩家背包中的所有物品
        boolean hasSword = false;

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            // 如果该物品是剑，进行替换
            if (item != null && isSword(item.getType())) {
                contents[i] = getRandomSword();  // 替换为随机的剑
                hasSword = true;
            }
        }

        if (hasSword) {
            p.getInventory().setContents(contents);  // 更新玩家背包
        }

        return hasSword;  // 返回是否成功替换了至少一把剑
    }

    private boolean isSword(Material material) {
        return SWORD_MATERIALS.contains(material);
    }

    private ItemStack getRandomSword() {
        // 从四把指定的剑中随机选择一把
        return SWORDS[ThreadLocalRandom.current().nextInt(SWORDS.length)].clone();
    }
}
