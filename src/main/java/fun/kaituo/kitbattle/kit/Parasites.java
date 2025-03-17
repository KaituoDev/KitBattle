package fun.kaituo.kitbattle.kit;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Silverfish;
import org.bukkit.entity.Endermite;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import fun.kaituo.kitbattle.util.PlayerData;
import fun.kaituo.kitbattle.KitBattle;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.bukkit.Bukkit.getPlayer;

public class Parasites extends PlayerData implements Listener {
    private final UUID ownerUUID;
    private final List<Entity> summonedEntities = new ArrayList<>();
    private BukkitRunnable cleanupTask;

    public Parasites(Player p) {
        super(p);
        this.ownerUUID = p.getUniqueId();
        Bukkit.getPluginManager().registerEvents(this, KitBattle.inst());
    }

    @Override
    public void onDestroy() {
        clearSummonedEntities();
    }

    public boolean castSkill() {
        Location location = p.getLocation();

        // 先清除上一波召唤物，并取消旧的清除任务
        clearSummonedEntities();

        int numSummons = 3 + (int) (Math.random() * 3);  // 生成3到5个召唤物
        for (int i = 0; i < numSummons; i++) {
            if (i % 2 == 0) {
                // 召唤蠹虫
                Silverfish bug = p.getWorld().spawn(location, Silverfish.class);
                bug.setCustomNameVisible(false);
                bug.setSilent(true);
                bug.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 1));
                bug.setPersistent(false);
                bug.setMetadata("ownerUUID", new FixedMetadataValue(KitBattle.inst(), ownerUUID.toString()));
                summonedEntities.add(bug);
            } else {
                // 召唤末影螨
                Endermite endermite = p.getWorld().spawn(location, Endermite.class);
                endermite.setCustomNameVisible(false);
                endermite.setSilent(true);
                endermite.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1));
                endermite.setPersistent(false);
                endermite.setMetadata("ownerUUID", new FixedMetadataValue(KitBattle.inst(), ownerUUID.toString()));
                summonedEntities.add(endermite);
            }
        }

        // 取消旧的清除任务，防止上一波的定时器仍在运行
        if (cleanupTask != null && !cleanupTask.isCancelled()) {
            cleanupTask.cancel();
        }

        // 启动新的清除任务
        cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                clearSummonedEntities();
            }
        };
        cleanupTask.runTaskLater(KitBattle.inst(), 200L);  // 200L = 10秒
        return true;
    }

    private void clearSummonedEntities() {
        // 先检查并取消清除任务
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }

        // 清除召唤物
        for (Entity entity : summonedEntities) {
            if (entity != null && !entity.isDead()) {
                entity.remove();
            }
        }
        summonedEntities.clear();
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Silverfish || event.getDamager() instanceof Endermite) {
            Entity damager = event.getDamager();
            Player victim = event.getEntity() instanceof Player ? (Player) event.getEntity() : null;

            if (victim == null) return;

            // 检查召唤物的所有者
            if (damager.hasMetadata("ownerUUID")) {
                String ownerUUID = damager.getMetadata("ownerUUID").get(0).asString();
                if (victim.getUniqueId().toString().equals(ownerUUID)) {
                    event.setCancelled(true); // 阻止召唤物攻击它的主人
                    return;
                }
            }

            // 施加负面效果
            if (damager instanceof Silverfish) {
                victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 1)); // 5秒缓慢
            } else if (damager instanceof Endermite) {
                victim.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 1)); // 5秒黑暗
            }
        }
    }
}
