package fun.kaituo.kitbattle.listener;

import fun.kaituo.gameutils.util.AbstractSignListener;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class HitIntervalSign extends AbstractSignListener {
    public static final String ON_TEXT = "§a§l已开启";
    public static final String OFF_TEXT = "§c§l已关闭";

    public HitIntervalSign(JavaPlugin plugin, Location location) {
        super(plugin, location);
        lines.set(1, "§e§l受击无敌");
        lines.set(2, HitInterval ? ON_TEXT : OFF_TEXT);
    }

    private boolean HitInterval = true;

    public boolean HitInterval() {
        return HitInterval;
    }

    public void setHitInterval(boolean infiniteFirepower) {
        HitInterval = infiniteFirepower;
        lines.set(2, HitInterval ? ON_TEXT : OFF_TEXT);
        update();
    }

    @Override
    public void onRightClick(PlayerInteractEvent playerInteractEvent) {
        setHitInterval(!HitInterval);
    }

    @Override
    public void onSneakingRightClick(PlayerInteractEvent playerInteractEvent) {
        setHitInterval(!HitInterval);
    }
}
