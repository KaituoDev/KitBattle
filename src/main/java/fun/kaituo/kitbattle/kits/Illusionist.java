package fun.kaituo.kitbattle.kits;

import fun.kaituo.kitbattle.KitBattle;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

@SuppressWarnings("unused")
public class Illusionist implements Kit{
    private final int distance;

    public Illusionist() {
        distance = getConfigInt("distance");
    }

    public boolean castSkill(Player p) {
        RayTraceResult result = p.getWorld().rayTraceEntities(
                p.getEyeLocation(),
                p.getEyeLocation().getDirection(),
                distance,
                e -> e != p && e instanceof Player && KitBattle.inst().isInArena((Player) e)
        );
        if (result == null) {
            return false;
        }
        Player target = (Player) result.getHitEntity();
        if (target == null) {
            return false;
        }
        Location targetLoc = target.getLocation();
        Location selfLoc = p.getLocation();
        target.teleport(selfLoc);
        p.teleport(targetLoc);
        return true;
    }
}
