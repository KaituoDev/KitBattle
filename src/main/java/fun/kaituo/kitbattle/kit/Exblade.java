package fun.kaituo.kitbattle.kit;

import fun.kaituo.gameutils.util.GameInventory;
import fun.kaituo.kitbattle.KitBattle;
import fun.kaituo.kitbattle.util.PlayerData;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static org.bukkit.Bukkit.getPlayer;
import static org.bukkit.Material.WOODEN_SWORD;

public class Exblade extends PlayerData {
    private static final Set<Material> SWORD_MATERIALS = Set.of(
            WOODEN_SWORD,
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

    @Override
    public boolean castSkill() {
        ItemStack[] contents = p.getInventory().getContents();  // 获取玩家背包中的所有物品
        boolean hasSword = false;

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            // 如果该物品是剑，进行替换
            if (item != null && isSword(item.getType())) {
                contents[i] = getRandomSword(contents[i]);  // 替换为随机的剑
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

    private ItemStack getRandomSword(ItemStack currentSword) {
        ItemStack newSword;
        do {
            int index = ThreadLocalRandom.current().nextInt(SWORDS.length);
            newSword = SWORDS[index].clone();
        } while (currentSword != null && newSword.isSimilar(currentSword));
        return newSword;
    }
    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;

        Player player = (Player) event.getDamager();

        // 确保玩家是此职业
        if (!player.equals(p)) return;

        // 确保玩家手中拿的是指定的剑
        if (!isUsingSpecificSword(player)) return;

        // 确保被攻击的目标是生物
        if (event.getEntity() instanceof LivingEntity) {
            LivingEntity victim = (LivingEntity) event.getEntity();

            // 施加中毒效果（5秒，等级1）
            victim.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 2));
        }
    }
    private boolean isUsingSpecificSword(Player player) {
        ItemStack weapon = player.getInventory().getItemInMainHand();
            if (weapon != null && weapon.getType().equals(WOODEN_SWORD)) {
                return true;
            }
        return false;
    }
}
