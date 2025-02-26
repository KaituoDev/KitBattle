package fun.kaituo.kitbattle.listener;

import fun.kaituo.gameutils.util.AbstractSignListener;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class RecoverOnKillSign extends AbstractSignListener {
    public static final String ON_TEXT = "§a§l已开启";
    public static final String OFF_TEXT = "§c§l已关闭";
    public RecoverOnKillSign(JavaPlugin plugin, Location location) {
        super(plugin, location);
        lines.set(1, "§e§l击杀回满");
        lines.set(2, shouldRecoverOnKill ? ON_TEXT : OFF_TEXT);
    }

    private boolean shouldRecoverOnKill = true;

    public boolean shouldRecoverOnKill() {
        return shouldRecoverOnKill;
    }

    public void setShouldRecoverOnKill(boolean shouldRecoverOnKill) {
        this.shouldRecoverOnKill = shouldRecoverOnKill;
        lines.set(2, shouldRecoverOnKill ? ON_TEXT : OFF_TEXT);
        update();
    }

    @Override
    public void onRightClick(PlayerInteractEvent playerInteractEvent) {
        setShouldRecoverOnKill(!shouldRecoverOnKill);
    }

    @Override
    public void onSneakingRightClick(PlayerInteractEvent playerInteractEvent) {
        setShouldRecoverOnKill(!shouldRecoverOnKill);
    }
}
