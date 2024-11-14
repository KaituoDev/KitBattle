package fun.kaituo.kitbattle;

import fun.kaituo.gameutils.GameUtils;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Random;

public class KitBattleCommandExecutor implements CommandExecutor {
    private GameUtils gameUtils;
    Location[] spawnLocations;
    List<Player> players;
    Random random = new Random();
    public KitBattleCommandExecutor(GameUtils gameUtils, Location[] locations, List<Player> players) {
        this.gameUtils = gameUtils;
        this.spawnLocations = locations;
        this.players = players;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (command.getName().equalsIgnoreCase("kbspawn")) {
            if (!(commandSender instanceof Player)) {
                commandSender.sendMessage("&c只有玩家才能使用这个命令");
                return true;
            }
            if (gameUtils.getPlayerGame((Player) commandSender) != KitBattle.getGameInstance()) {
                commandSender.sendMessage("你不在职业战争游戏中");
                return false;
            }
            ((Player)commandSender).teleport(spawnLocations[random.nextInt(7)]);
            ((Player)commandSender).playSound(((Player) commandSender).getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP,  SoundCategory.PLAYERS, 1, 1);
            return true;
        }
        return false;
    }
}
