package me.nuka.offerings.events;

import me.nuka.offerings.Main;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import static me.nuka.offerings.Utils.getWorldEditMinMax;

public class EventListener implements Listener {
    private Main plugin;

    public EventListener(Main plugin){
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event){
        if(event == null || !event.hasBlock() || event.getClickedBlock() == null) return;

        if(event.getClickedBlock().getType() != Material.LECTERN) return;
        if(event.getPlayer().getInventory().getItemInMainHand().getType() != Material.PAPER) return;

        // TODO: check item lore
        // TODO: abstract that method, use in onPlayerDropItem as well

        String templeName = plugin.droppedInOffering(event.getClickedBlock().getLocation());
        if(templeName == null || templeName.equals("")) return;

        if(plugin.getCurrentlyInUse(templeName)) return;

        ItemStack handItem = event.getPlayer().getInventory().getItemInMainHand();
        handItem.setAmount(handItem.getAmount() - 1);

        plugin.setCurrentlyInUse(templeName, true);
        plugin.performOffering(event.getPlayer(), handItem, templeName, 1.1, 2, 0.012);
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event){
        if(event == null) return;

        Player player = event.getPlayer();
        Item itemDropped = event.getItemDrop();

        if(itemDropped.getItemStack().getItemMeta().getLore().isEmpty()) return;

        String lore = ChatColor.stripColor(itemDropped.getItemStack().getItemMeta().getLore().get(1));

        if(!lore.contains("Temple of")) return;
        itemDropped.setPickupDelay(40);

        new BukkitRunnable() {
            @Override
            public void run() {
                String templeName = plugin.droppedInOffering(itemDropped.getLocation());
                if(templeName == null || templeName.equals("")) return;

                if(plugin.getCurrentlyInUse(templeName)){
                    player.getInventory().addItem(itemDropped.getItemStack());
                    itemDropped.remove();
                    player.sendMessage("Someone is already making an offering. Please be patient and try again when it is finished.");
                    return;
                }

                // TODO if itemStack.getAmount > 1, reimburse the other ones
                if(itemDropped.getItemStack().getAmount() > 1) {
                    int amount = itemDropped.getItemStack().getAmount()-1;
                    itemDropped.getItemStack().setAmount(amount);
                    player.getInventory().addItem(itemDropped.getItemStack());
                    player.sendMessage("You've dropped too many offerings. It is nice of you but we only accept them one at a time.\nYou got back your "+ amount + " offerings");
                }

                itemDropped.remove();
                plugin.setCurrentlyInUse(templeName, true);
                plugin.performOffering(player, itemDropped.getItemStack(), templeName,1 , 3, 0.02);
            }
        }.runTaskLater(plugin, 30);
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event){
        if(event == null) return;
        if(!event.getLine(0).equalsIgnoreCase("[Offering]")) return;

        if(!event.getPlayer().hasPermission("offerings.create")) {
            event.getPlayer().sendMessage("You do not have the permission to create Offerings.");
            return;
        }

        if(event.getLine(1).equals("")) {
            event.getPlayer().sendMessage("You need to specify the temple name on line 2.");
            return;
        }

        plugin.getTemples().put(event.getLine(1), getWorldEditMinMax(event.getPlayer()));
        event.getPlayer().sendMessage("Offering successfully created.");
    }

}
