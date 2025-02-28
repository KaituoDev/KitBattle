package fun.kaituo.kitbattle.kits;

import com.comphenix.protocol.PacketType;
import fun.kaituo.kitbattle.util.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@SuppressWarnings("unused")
public class Striker extends PlayerData {
    public Striker(Player p) {
        super(p);
    }
        @Override
        public void applyPotionEffects(Player p){
            super.applyPotionEffects(p);
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, -1, 0, false, false));
        }
    }
