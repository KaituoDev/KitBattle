package fun.kaituo.kitbattle.listener;

import fun.kaituo.gameutils.GameUtils;
import fun.kaituo.gameutils.util.AbstractSignListener;
import fun.kaituo.kitbattle.KitBattle;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class ChooseKitSign extends AbstractSignListener {
    public ChooseKitSign(JavaPlugin plugin, Location location) {
        super(plugin, location);
        lines.set(0, "§a§l职业战争");
        lines.set(2, "§6§l[选择职业]");
    }

    @Override
    public void onRightClick(PlayerInteractEvent playerInteractEvent) {
        Player p = playerInteractEvent.getPlayer();
        if (GameUtils.inst().getGame(p) != KitBattle.inst()) {
            return;
        }
        Bukkit.dispatchCommand(p, "zmenu open kitbattle");
    }

    @Override
    public void onSneakingRightClick(PlayerInteractEvent playerInteractEvent) {
        onRightClick(playerInteractEvent);
    }
}
