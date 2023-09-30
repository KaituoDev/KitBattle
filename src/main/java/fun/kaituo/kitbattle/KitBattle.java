package fun.kaituo.kitbattle;



import fun.kaituo.gameutils.Game;
import fun.kaituo.gameutils.GameUtils;
import fun.kaituo.gameutils.event.PlayerChangeGameEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;



public class KitBattle extends JavaPlugin {
    private GameUtils gameUtils;
    List<Player> players = new ArrayList<>();

    public static KitBattleGame getGameInstance() {
        return KitBattleGame.getInstance();
    }

    public void onEnable() {
        gameUtils = (GameUtils) Bukkit.getPluginManager().getPlugin("GameUtils");
        saveDefaultConfig();
        gameUtils.registerGame(getGameInstance());
        getCommand("kbspawn").setExecutor(new KitBattleCommandExecutor(getGameInstance().spawnLocations, players));
    }

    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
        HandlerList.unregisterAll(this);
        for (Player p: Bukkit.getOnlinePlayers()) {
            if (gameUtils.getPlayerGame(p) == getGameInstance()) {
                Bukkit.dispatchCommand(p, "join Lobby");
            }
        }
        gameUtils.unregisterGame(getGameInstance());
    }
}