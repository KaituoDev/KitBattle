package fun.kaituo.kitbattle.kit;

import fun.kaituo.kitbattle.util.PlayerData;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import static fun.kaituo.kitbattle.KitBattle.PARTICLE_COUNT;
import static fun.kaituo.kitbattle.KitBattle.SOUND_VOLUME;

@SuppressWarnings("unused")
public class Elf extends PlayerData {
    private final int poisonDuration;
    private final int poisonAmplifier;

    public Elf(Player p) {
        super(p);
        poisonDuration = getConfigInt("poison-duration");
        poisonAmplifier = getConfigInt("poison-amplifier");
    }

    public boolean castSkill(Player p) {
        if (p.getInventory().contains(Material.TIPPED_ARROW)) {
            return false;
        }
        ItemStack arrow = new ItemStack(Material.TIPPED_ARROW);
        PotionMeta meta = (PotionMeta)arrow.getItemMeta();
        assert meta != null;
        meta.setBasePotionType(PotionType.POISON);
        meta.addCustomEffect(new PotionEffect(PotionEffectType.POISON, poisonDuration, poisonAmplifier), true);
        arrow.setItemMeta(meta);
        p.getInventory().addItem(arrow);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, SOUND_VOLUME, 1);
        p.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR , p.getLocation(), PARTICLE_COUNT, 3, 3, 3);
        return true;
    }
}
