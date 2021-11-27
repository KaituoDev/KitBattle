package tech.yfshadaow;


import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class KitBattle extends JavaPlugin{
    World world;
    List<Player> players;
    public void onEnable() {
        this.world = Bukkit.getWorld("world");
        this.players = new ArrayList<>();
        Game game = new Game(this);
        game.runTask(this);
    }

    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
        HandlerList.unregisterAll((Plugin)this);
        if (players.size() > 0) {
            for (Player p : players) {
                p.teleport(new Location(world, 0.5,89.0,0.5));
                Bukkit.getPluginManager().callEvent(new PlayerChangeGameEvent(p));
            }
        }
    }
}