package fun.kaituo;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Random;

public class KitBattleCommandExecutor implements CommandExecutor {
    Location[] spawnLocations;
    List<Player> players;
    Random random = new Random();
    public KitBattleCommandExecutor(Location[] locations, List<Player> players) {
        this.spawnLocations = locations;
        this.players = players;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (command.getName().equalsIgnoreCase("kbspawn")) {
            if (commandSender.isOp() && commandSender instanceof Player) {
                ((Player)commandSender).teleport(spawnLocations[random.nextInt(7)]);
                ((Player)commandSender).playSound(((Player) commandSender).getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP,  SoundCategory.PLAYERS, 1, 1);
                return true;
            }
        }
        return false;
    }
}
