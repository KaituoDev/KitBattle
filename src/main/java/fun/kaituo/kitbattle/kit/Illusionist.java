package fun.kaituo.kitbattle.kit;

import fun.kaituo.kitbattle.KitBattle;
import fun.kaituo.kitbattle.util.PlayerData;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

@SuppressWarnings("unused")
public class Illusionist extends PlayerData {
    private final int distance;

    public Illusionist(Player p) {
        super(p);
        distance = getConfigInt("distance");
    }

    @Override
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
