package fun.kaituo.kitbattle.kits;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@SuppressWarnings("unused")
public class BladeMaster implements Kit {
    @Override
    public void castSkill(Player p) {
        Bukkit.broadcastMessage("§a" + p.getName() + "使用了技能：剑刃风暴！");
    }

    @Override
    public long getCooldownTicks() {
        return 200;
    }
}
