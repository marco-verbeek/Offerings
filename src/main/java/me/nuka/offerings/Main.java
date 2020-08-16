package me.nuka.offerings;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class Main extends JavaPlugin implements Listener {
    private HashMap<String, Location[]> temples = new HashMap<>();
    private HashMap<String, RandomCollection<ItemStack>> rewards = new HashMap<>();

    // TODO: fill this.rewards with ItemStacks & their probability FROM CONFIG
    // TODO: cant do more than one offering at a time

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);

        if (!Bukkit.getPluginManager().isPluginEnabled("HolographicDisplays")) {
            getLogger().severe("*** HolographicDisplays is not installed or not enabled. ***");
            getLogger().severe("*** This plugin will be disabled. ***");

            this.setEnabled(false);
            return;
        }

        RandomCollection<ItemStack> randomCollection = new RandomCollection<>();
        randomCollection.add(5, new ItemStack(Material.WITHER_ROSE));
        randomCollection.add(30, new ItemStack(Material.DIAMOND_AXE));
        randomCollection.add(1, new ItemStack(Material.NETHER_STAR));
        randomCollection.add(50, new ItemStack(Material.STONE_HOE));
        randomCollection.add(20, new ItemStack(Material.DIAMOND_HELMET));
        randomCollection.add(10, new ItemStack(Material.NETHERITE_CHESTPLATE));
        randomCollection.add(10, new ItemStack(Material.NETHERITE_SWORD));
        randomCollection.add(20, new ItemStack(Material.NETHERITE_SCRAP));
        randomCollection.add(20, new ItemStack(Material.GOLDEN_APPLE));
        randomCollection.add(10, new ItemStack(Material.EMERALD_BLOCK));
        randomCollection.add(40, new ItemStack(Material.STICKY_PISTON));

        this.rewards.put("NuKa", randomCollection);

        this.saveDefaultConfig();
        if(this.getConfig().contains("temples"))
            loadConfig();
    }

    @Override
    public void onDisable() {
        for(Hologram hologram : HologramsAPI.getHolograms(this))
            hologram.delete();

        if(!temples.isEmpty())
            saveToConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(sender instanceof Player && label.equalsIgnoreCase("of")){

            Material mat = Material.WITHER_ROSE;
            int qty = (args.length > 1) ? Integer.parseInt(args[1]) : 1;

            ArrayList<String> lore = new ArrayList<>();
            lore.add("");

            if(args.length != 0) {
                String itemName = args[0];

                if (itemName.equalsIgnoreCase("War")) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', "&7&oTemple of &c&lWar"));
                } else if (itemName.equalsIgnoreCase("Knowledge") || itemName.equalsIgnoreCase("K")) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', "&7&oTemple of &a&lKnowledge"));
                } else if (itemName.equalsIgnoreCase("Diplomacy")) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', "&7&oTemple of &d&lDiplomacy"));
                } else if (itemName.equalsIgnoreCase("Bees")) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', "&7&oTemple of &e&lBees"));
                } else if (itemName.equalsIgnoreCase("Alpacas")) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', "&7&oTemple of &6&lAlpacas"));
                }
            } else {
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7&oTemple of &6&lAlpacas"));
            }

            giveBlessing((Player) sender, mat, lore, qty, true);
        }

        return true;
    }

    private void giveBlessing(Player player, Material mat, ArrayList<String> lore, int qty, boolean enchant) {
        ItemStack offeringItem = new ItemStack(mat);
        offeringItem.setAmount(qty);

        ItemMeta meta = offeringItem.getItemMeta();

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&7&nBlessing"));
        meta.setLore(lore);

        offeringItem.setItemMeta(meta);

        if(enchant) enchant(offeringItem);

        player.getInventory().addItem(offeringItem);
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event){
        if(event == null) return;

        Player player = event.getPlayer();
        Item itemDropped = event.getItemDrop();

        if(itemDropped.getItemStack().getItemMeta().getLore().isEmpty()) return;

        String lore = ChatColor.stripColor(itemDropped.getItemStack().getItemMeta().getLore().get(1));

        if(!lore.contains("Offering for the")) return;
        itemDropped.setPickupDelay(20);

        new BukkitRunnable() {
            @Override
            public void run() {
                String templeName = droppedInOffering(itemDropped.getLocation());
                if(templeName == null || templeName.equals("")) return;

                // TODO if itemStack.getAmount > 1, reimburse the other ones

                itemDropped.remove();
                performOffering(player, itemDropped, templeName);
            }
        }.runTaskLater(this, 30);
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

        this.temples.put(event.getLine(1), getWorldEditMinMax(event.getPlayer()));
        event.getPlayer().sendMessage("Offering successfully created.");
    }

    private Location[] getWorldEditMinMax(Player player){
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

    private String droppedInOffering(Location dropLocation){
        for(Map.Entry<String, Location[]> entry : this.temples.entrySet()){
            Location[] locations = entry.getValue();

            int minX = Math.min(locations[0].getBlockX(), locations[1].getBlockX());
            int minY = Math.min(locations[0].getBlockY(), locations[1].getBlockY());
            int minZ = Math.min(locations[0].getBlockZ(), locations[1].getBlockZ());

            int maxX = Math.max(locations[0].getBlockX(), locations[1].getBlockX());
            int maxY = Math.max(locations[0].getBlockY(), locations[1].getBlockY());
            int maxZ = Math.max(locations[0].getBlockZ(), locations[1].getBlockZ());

            if(dropLocation.getBlockX() <= maxX && dropLocation.getBlockX() >= minX)
                if(dropLocation.getBlockY() <= maxY && dropLocation.getBlockY() >= minY)
                    if(dropLocation.getBlockZ() <= maxZ && dropLocation.getBlockZ() >= minZ)
                        return entry.getKey();
        }

        return null;
    }

    private void performOffering(Player player, Item itemDropped, String templeName) {
        player.sendMessage("You successfully dropped an Offering in the Temple of " + templeName);

        Location[] templeLoc = this.temples.get(templeName);
        Location holoLoc = templeLoc[0].toVector().getMidpoint(templeLoc[1].toVector()).toLocation(player.getWorld()).add(0.5, 1, 0.5);

        Hologram hologram = HologramsAPI.createHologram(this, holoLoc);
        hologram.appendItemLine(itemDropped.getItemStack());

        ArrayList<ItemStack> rewards = getTempleRewards(templeName);
        ItemStack finalReward = this.rewards.get(templeName).next();

        new BukkitRunnable(){
            double maximumY = holoLoc.getY() + 3;
            double time = 1;

            @Override
            public void run() {
                if(holoLoc.getY() >= maximumY){
                    concludeOffering(player, finalReward, hologram);

                    cancel();
                    return;
                }

                if(time % 4 == 0){
                    ItemStack item = getItemFromRewards(rewards);
                    hologram.clearLines();
                    hologram.appendItemLine(item);
                }

                holoLoc.setY(holoLoc.getY() + 0.02);
                hologram.teleport(holoLoc);

                time += 1;
            }
        }.runTaskTimer(this, 0, 1);
    }

    private ItemStack getItemFromRewards(ArrayList<ItemStack> rewards) {
        Collections.shuffle(rewards);
        return rewards.get(0);
    }

    private void concludeOffering(Player player, ItemStack itemStack, Hologram hologram) {
        player.sendMessage("Finished Offering with item " + itemStack.getItemMeta().getDisplayName());

        hologram.clearLines();
        hologram.appendItemLine(itemStack);

        new BukkitRunnable(){
            @Override
            public void run() {
                hologram.delete();
                player.getInventory().addItem(itemStack);
            }
        }.runTaskLater(this, 3*20);
    }

    private ArrayList<ItemStack> getTempleRewards(String templeName){
        return this.rewards.get(templeName).collectionAsArray();
    }

    private ItemStack enchant(ItemStack item) {
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta != null) {
            itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            itemMeta.addEnchant(Enchantment.DURABILITY, 1, true);
            item.setItemMeta(itemMeta);
        }
        return item;
    }

    private void loadConfig(){
        this.getConfig().getConfigurationSection("temples").getKeys(false).forEach(key -> {
            ArrayList<Location> temples = (ArrayList<Location>) getConfig().get("temples."+key);

            Location[] templeLocations = new Location[2];
            templeLocations[0] = temples.get(0);
            templeLocations[1] = temples.get(1);

            this.temples.put(key, templeLocations);
        });
    }

    private void saveToConfig(){
        for(Map.Entry<String, Location[]> entry : this.temples.entrySet())
            this.getConfig().set("temples." + entry.getKey(), entry.getValue());

        this.saveConfig();
    }
}
