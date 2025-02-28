package fun.kaituo.kitbattle.command;

import fun.kaituo.gameutils.GameUtils;
import fun.kaituo.kitbattle.KitBattle;
import fun.kaituo.kitbattle.util.PlayerData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class KitBattleGo implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@Nonnull CommandSender sender, Command cmd, @Nonnull String label, @Nonnull String[] args) {
        if (!cmd.getName().equalsIgnoreCase("kitbattlego")) {
            return false;
        }
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§c此指令必须由玩家执行！");
            return true;
        }
        if (GameUtils.inst().getGame(p) != KitBattle.inst()) {
            sender.sendMessage("§c你不在职业战争中！");
            return true;
        }
        if (args.length != 1) {
            sender.sendMessage("§c指令参数错误！使用方法为/kitbattlego <职业名称>");
            return true;
        }
        Class<? extends PlayerData> kitClass = KitBattle.inst().getKitClass(args[0]);
        if (kitClass == null) {
            sender.sendMessage("§c未找到名称为" + args[0] + "的职业！");
            return true;
        }
        KitBattle.inst().toArena(p, kitClass);
        return true;
    }

    public List<String> onTabComplete(@Nonnull CommandSender sender, Command command, @Nonnull String alias, @Nonnull String[] args) {
        if (!command.getName().equalsIgnoreCase("kitbattlego")) {
            return new ArrayList<>();
        }
        if (args.length != 1) {
            return new ArrayList<>();
        }
        return KitBattle.inst().getKitNames().stream().filter(
                name -> name.toLowerCase().startsWith(args[0].toLowerCase())
        ).toList();
    }
}
