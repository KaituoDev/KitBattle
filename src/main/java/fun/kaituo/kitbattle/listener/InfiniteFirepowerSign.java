package fun.kaituo.kitbattle.listener;

import fun.kaituo.gameutils.util.AbstractSignListener;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class InfiniteFirepowerSign extends AbstractSignListener {
    public static final String ON_TEXT = "§a§l已开启";
    public static final String OFF_TEXT = "§c§l已关闭";

    public InfiniteFirepowerSign(JavaPlugin plugin, Location location) {
        super(plugin, location);
        lines.set(1, "§e§l无限火力");
        lines.set(2, isInfiniteFirepower ? ON_TEXT : OFF_TEXT);
    }

    private boolean isInfiniteFirepower = true;

    public boolean isInfiniteFirepower() {
        return isInfiniteFirepower;
    }

    public void setInfiniteFirepower(boolean infiniteFirepower) {
        isInfiniteFirepower = infiniteFirepower;
        lines.set(2, isInfiniteFirepower ? ON_TEXT : OFF_TEXT);
        update();
    }

    @Override
    public void onRightClick(PlayerInteractEvent playerInteractEvent) {
        setInfiniteFirepower(!isInfiniteFirepower);
    }

    @Override
    public void onSneakingRightClick(PlayerInteractEvent playerInteractEvent) {
        setInfiniteFirepower(!isInfiniteFirepower);
    }
}
