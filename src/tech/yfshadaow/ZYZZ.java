package tech.yfshadaow;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Switch;
import org.bukkit.craftbukkit.libs.jline.internal.Nullable;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class ZYZZ extends JavaPlugin implements Listener {
    Scoreboard zyzz;
    World world;
    Scoreboard scoreboard;
    HashMap<Player, Long> cd1;
    HashMap<Player, Long> cd2;

    @EventHandler
    public void cancelSpawn(CreatureSpawnEvent cse) { //防止鸡蛋生成鸡
        if (cse.getSpawnReason().equals(CreatureSpawnEvent.SpawnReason.EGG)) {
                cse.setCancelled(true);
        }
    }

    @EventHandler public void clearCoolDown(PlayerDeathEvent pde) {clearCoolDown(pde.getEntity());}
    @EventHandler public void clearCoolDown(PlayerTeleportEvent pte) {clearCoolDown(pte.getPlayer());} //重置玩家冷却

    @EventHandler
    public void clearKills(PlayerInteractEvent pie) {
        if (pie.getClickedBlock() == null) {
            return;
        }
        Location location = pie.getClickedBlock().getLocation();
        long x = location.getBlockX(); long y = location.getBlockY(); long z = location.getBlockZ();
        if (x == 0 && y == 203 && z == 997) {
            zyzz.getObjective("zyzzKills").unregister();
            zyzz.registerNewObjective("zyzzKills","dummy","职业战争击败榜");
            zyzz.getObjective("zyzzKills").setDisplaySlot(DisplaySlot.SIDEBAR);
        }
    }


    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent pde) {
        Player player = pde.getEntity();
        Player killer = pde.getEntity().getKiller();

        if (!(killer instanceof Player)) { return; } //不是玩家
        //if (!player.getScoreboardTags().contains("zyzz")) { return; }//死亡者没有tag
        if (player.equals(killer)) { return; } //自杀判定
        if (killer.getScoreboardTags().contains("zyzz")) {
            Score score = zyzz.getObjective("zyzzKills").getScore(killer);
            score.setScore(score.getScore() + 1);//加分
        } else if (killer.getScoreboardTags().contains("zytz")){
            //继续就行了
        } else {
            return;//不处于任何一个游戏中
        }


        if (killer.getInventory().getChestplate() == null) { return; }//没有胸甲
        if (killer.getInventory().getChestplate().getItemMeta() == null) { return; }//没有meta

        //这里添加内容

        switch (killer.getInventory().getChestplate().getItemMeta().getDisplayName()) {
            case "战士胸甲":
            case "坦克胸甲":
            case "太空人胸甲":
            case "海盗胸甲":
            case "地狱战士胸甲":
            case "忍者胸甲":
            case "审判者胸甲":
            case "咏春胸甲":
                double maxHealth = killer.getMaxHealth();
                if (killer.getMaxHealth() - killer.getHealth() <= 4) {
                    killer.setHealth(killer.getMaxHealth());
                } else {
                    killer.setHealth(killer.getHealth() + 4);
                }
                break;
            case "弓箭手胸甲":
            case "神箭游侠胸甲":
                killer.getInventory().addItem(new ItemStack(Material.ARROW, 8));
                break;
            case "熊孩子胸甲":
                killer.getInventory().addItem(new ItemStack(Material.EGG, 4));
                killer.getInventory().addItem(new ItemStack(Material.SNOWBALL, 8));
                break;
        }


    }




    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent pie) {
        if (!pie.getAction().equals(Action.RIGHT_CLICK_AIR) && !pie.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {return;}// 不是右键
        if (pie.getItem() == null) {return;}//没有物品
        if (pie.getItem().getItemMeta() == null) {return;}//没有meta
        Player executor = pie.getPlayer();
        if (!executor.getScoreboardTags().contains("zyzz") && !executor.getScoreboardTags().contains("zytz")) {return;}//不在zyzz里
        if (executor.getLocation().getY() > 128) {return;}//在高空
        //这里开始添加内容

        switch (pie.getItem().getItemMeta().getDisplayName()) {

            case "§c烈焰攻击" :
                if (!checkCoolDown(executor,cd1,800)) {
                    return;
                }
                world.spawnParticle(Particle.FLAME, pie.getPlayer().getLocation(),25,5, 5, 5);
                world.playSound(pie.getPlayer().getLocation(), Sound.BLOCK_LAVA_EXTINGUISH,1, 1);
                Collection<Entity> entities = world.getNearbyEntities(executor.getLocation(), 5, 5, 5);
                for (Entity e: entities) {
                    if (e instanceof Player) {
                        if (checkSameTeam((Player)e,executor)) {
                            continue;
                        }
                        ((Player) e).damage(1, executor);
                        if (e.getFireTicks() < 80) {
                            e.setFireTicks(80);
                        }
                    }
                }
                break;

            case "§b坚定信念-抗性" :
                if (!checkCoolDown(executor,cd1,400)) {
                    return;
                }
                world.spawnParticle(Particle.FIREWORKS_SPARK, pie.getPlayer().getLocation(),10,1, 1, 1);
                world.playSound(pie.getPlayer().getLocation(), Sound.BLOCK_ANVIL_PLACE,1, 1);
                executor.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE,100,0));
                break;
        }
    }

    @EventHandler
    public void preventDropping(PlayerDropItemEvent pdie) {
        if (pdie.getPlayer().getScoreboardTags().contains("zyzz") || pdie.getPlayer().getScoreboardTags().contains("zytz")) {
            pdie.setCancelled(true);
        }
    }

    @EventHandler
    public void cancelTeamDamage(EntityDamageByEntityEvent edbee) {
        if (!(edbee.getDamager() instanceof Player) || !(edbee.getEntity() instanceof Player)) {
            return;
        }
        if (checkSameTeam((Player)edbee.getDamager(),(Player)edbee.getEntity())) {
            edbee.setCancelled(true);
        }
    }
    @EventHandler
    public void cancelProjectileDamage(EntityDamageByEntityEvent edbee) {
        if (!(edbee.getEntity() instanceof Player)) {
            return;
        }
        if (!(edbee.getDamager() instanceof Projectile)) {
            return;
        }
        if (((Projectile)edbee.getDamager()).getShooter() == null) {
            return;
        }
        if (!(((Projectile)edbee.getDamager()).getShooter() instanceof Player)) {
            return;
        }
        if (checkSameTeam((Player)((Projectile) edbee.getDamager()).getShooter(),(Player)edbee.getEntity())) {
            edbee.setCancelled(true);
        }
    }


    @EventHandler
    public void onProjectileHit(ProjectileHitEvent phe) {
        if (!((phe.getEntity().getShooter()) instanceof Player)) { return; }//不是玩家
        Player shooter = (Player)phe.getEntity().getShooter();
        if (!shooter.getScoreboardTags().contains("zyzz") && !shooter.getScoreboardTags().contains("zytz")) { return; }//不在职业战争里
        if (shooter.getInventory().getChestplate() == null) { return; }//没有胸甲
        if (shooter.getInventory().getChestplate().getItemMeta() == null) { return; }//没有meta
        Location l;
        if (phe.getHitBlock() != null) {
            l = phe.getHitBlock().getLocation();
            switch (phe.getHitBlockFace()) {
                case UP: l.setX(l.getX()+0.5); l.setY(l.getY()+1.5); l.setZ(l.getZ()+0.5); break;
                case DOWN: l.setX(l.getX()+0.5); l.setY(l.getY()-0.5); l.setZ(l.getZ()+0.5); break;
                case NORTH: l.setX(l.getX()+0.5); l.setY(l.getY()+0.5); l.setZ(l.getZ()-0.5); break;
                case SOUTH: l.setX(l.getX()+0.5); l.setY(l.getY()+0.5); l.setZ(l.getZ()+1.5); break;
                case EAST: l.setX(l.getX()+1.5); l.setY(l.getY()+0.5); l.setZ(l.getZ()+0.5); break;
                case WEST: l.setX(l.getX()-0.5); l.setY(l.getY()+0.5); l.setZ(l.getZ()+0.5); break;
                default: l.setX(l.getX()+0.5); l.setY(l.getY()+0.5); l.setZ(l.getZ()+0.5); break;
            }
        } else {
            l = phe.getHitEntity().getLocation();
            l.setY(l.getY() + 1.8);
        }
        if (l.getY() > 128) { return; } //大厅里面


        //这里开始 添加具体内容

        switch (shooter.getInventory().getChestplate().getItemMeta().getDisplayName()) {
            case "熊孩子胸甲":
                if (phe.getEntity().getType().equals(EntityType.SNOWBALL)) {//雪球
                    world.strikeLightningEffect(l);
                    Collection<Entity> entities = world.getNearbyEntities(l, 3, 3, 3);
                    for (Entity e: entities) {
                        if (e instanceof Player) {
                            ((Player) e).damage(3, shooter);
                        }
                    }
                } else if (phe.getEntity().getType().equals(EntityType.EGG)) {//鸡蛋
                    world.createExplosion(l, 1.6f, false, false, shooter);
                    world.spawnParticle(Particle.EXPLOSION_LARGE,l,1);
                }
                break;
        }
    }





    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        world = Bukkit.getWorld("world");
        scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        cd1 = new HashMap<Player, Long>();
        cd2 = new HashMap<Player, Long>();
        zyzz = Bukkit.getScoreboardManager().getMainScoreboard();
    }

    public void onDisable() {

    }

    public boolean checkSameTeam(Player a, Player b) {

        if (a.equals(b)) {
            return true;
        } else if (scoreboard.getPlayerTeam(a)== null || scoreboard.getPlayerTeam(b) == null) {
            return false;
        } else if (scoreboard.getPlayerTeam(a).equals(scoreboard.getPlayerTeam(b))) {
            return true;
        } else {
            return false;
        }
    }

    public long getTime(World world) {
        return ((CraftWorld)world).getHandle().worldData.getTime();
    }

    public void clearCoolDown(Player p) {
        if (cd1.get(p) != null) {
            cd1.remove(p);
        }
        if (cd2.get(p) != null) {
            cd2.remove(p);
        }
    }

    public boolean checkCoolDown(Player p, HashMap<Player, Long> coolDownMap, long coolDown) {
        if (coolDownMap.get(p) == null) {
            coolDownMap.put(p,getTime(world));
            return true;
        } else {
            long timeLapsed = getTime(world) - coolDownMap.get(p);
            if (timeLapsed < coolDown) {
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§4§l技能冷却中！ 还剩 §3§l" + (int)((coolDown - timeLapsed) / 20) + " §4§l秒"));
                return false;
            } else {
                coolDownMap.put(p,getTime(world));
                return true;
            }
        }
    }

}
