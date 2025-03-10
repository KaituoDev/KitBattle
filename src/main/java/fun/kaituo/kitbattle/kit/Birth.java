package fun.kaituo.kitbattle.kit;

import fun.kaituo.kitbattle.KitBattle;
import fun.kaituo.kitbattle.util.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SuspiciousStewMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Birth extends PlayerData implements Listener {

    public Birth(Player p) {
        super(p);
    }

    // 监听玩家复活事件，确保图腾触发后处理物品替换和效果
    @EventHandler
    public void onEntityResurrect(EntityResurrectEvent event) {
        if (!event.getEntity().getUniqueId().equals(playerId)) {
            return;
        }

        Player p = (Player) event.getEntity();

        onTotemTrigger();  // 执行替换装备和物品的逻辑
        Bukkit.getScheduler().runTaskLater(KitBattle.inst(), () -> {
            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, -1, 0));
            p.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, -1, 4));
        }, 1L); // 1 tick 后执行

    }

    // Method to handle when the Totem effect is triggered
    public void onTotemTrigger() {

        // Replace the player's armor with new items (you can specify the items here)
        p.getInventory().setItem(39,KitBattle.inst().getInv("Rebirth").getHead() );
        p.getInventory().setItem(38,KitBattle.inst().getInv("Rebirth").getChest() );
        p.getInventory().setItem(37,KitBattle.inst().getInv("Rebirth").getLegs() );
        p.getInventory().setItem(36,KitBattle.inst().getInv("Rebirth").getFeet() );


        ItemStack[] inventory = p.getInventory().getContents();
        for (int i = 0; i < inventory.length; i++) {
            ItemStack currentItem = inventory[i];

            if (currentItem != null && currentItem.getType() == Material.STONE_AXE) {
                p.getInventory().setItem(i, KitBattle.inst().getInv("Rebirth").getHotbar(0) );
            }
            if (currentItem != null && currentItem.getType() == Material.SPIDER_EYE) {
                p.getInventory().setItem(i, KitBattle.inst().getInv("Rebirth").getHotbar(1) );
            }
        }


    }
    @EventHandler
    public void onPlayerConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack consumedItem = event.getItem();

        // 确保玩家食用了迷之炖菜
        if (consumedItem.getType() == Material.SUSPICIOUS_STEW) {
            // 判断是否含有夜视效果
            if (consumedItem.getItemMeta() != null && consumedItem.getItemMeta() instanceof SuspiciousStewMeta) {
                SuspiciousStewMeta stewMeta = (SuspiciousStewMeta) consumedItem.getItemMeta();
                if (stewMeta.hasCustomEffect(PotionEffectType.NIGHT_VISION)) {
                    int slot = player.getInventory().first(consumedItem); // 获取物品原来的位置

                    // 延迟 1 tick 等待碗进入背包后再补充迷之炖菜
                    Bukkit.getScheduler().runTaskLater(KitBattle.inst(), () -> {
                        ItemStack newStew = new ItemStack(Material.SUSPICIOUS_STEW);
                        SuspiciousStewMeta newMeta = (SuspiciousStewMeta) newStew.getItemMeta();
                        newMeta.addCustomEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 20 * 8, 0), true); // 8秒夜视
                        newStew.setItemMeta(newMeta);

                        player.getInventory().setItem(slot, newStew); // 在原位置放置新炖菜
                    }, 1L); // 1 tick 后执行
                }
            }
        }
    }


}
