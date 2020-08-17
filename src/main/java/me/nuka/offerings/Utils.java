package me.nuka.offerings;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Map;

public class Utils {
    public static Location[] getWorldEditMinMax(Player player){
        BukkitPlayer bPlayer = BukkitAdapter.adapt(player);
        Region selection = null;

        try {
            selection = WorldEdit.getInstance().getSessionManager().get(bPlayer).getSelection(bPlayer.getWorld());
        } catch (IncompleteRegionException e) {
            e.printStackTrace();
            return null;
        }

        Location[] selectionAsLocations = new Location[2];
        selectionAsLocations[0] = new Location(player.getWorld(), selection.getMinimumPoint().getX(), selection.getMinimumPoint().getY(), selection.getMinimumPoint().getZ());
        selectionAsLocations[1] = new Location(player.getWorld(), selection.getMaximumPoint().getX(), selection.getMaximumPoint().getY(), selection.getMaximumPoint().getZ());

        return selectionAsLocations;
    }

    public static void giveBlessing(Player player, Material mat, ArrayList<String> lore, int qty, boolean enchant) {
        ItemStack offeringItem = new ItemStack(mat);
        offeringItem.setAmount(qty);

        ItemMeta meta = offeringItem.getItemMeta();

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&7&nBlessing"));
        meta.setLore(lore);

        offeringItem.setItemMeta(meta);

        if(enchant) enchant(offeringItem);

        player.getInventory().addItem(offeringItem);
    }

    public static ItemStack enchant(ItemStack item) {
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta != null) {
            itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            itemMeta.addEnchant(Enchantment.DURABILITY, 1, true);
            item.setItemMeta(itemMeta);
        }
        return item;
    }
}
