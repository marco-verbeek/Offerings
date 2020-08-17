package me.nuka.offerings;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

import static me.nuka.offerings.Utils.giveBlessing;

public class Main extends JavaPlugin implements Listener {
    private final HashMap<String, Location[]> temples = new HashMap<>();
    private final HashMap<String, RandomCollection<ItemStack>> rewards = new HashMap<>();
    private final HashMap<String, Boolean> currentlyInUse = new HashMap<>();

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(new EventListener(this), this);

        if (!Bukkit.getPluginManager().isPluginEnabled("HolographicDisplays")) {
            getLogger().severe("*** HolographicDisplays is not installed or not enabled. ***");

            this.setEnabled(false);
            return;
        }

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

            // TODO: create new items, and clean this horrible implementation

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

    public String droppedInOffering(Location dropLocation){
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

    public void performOffering(Player player, Item itemDropped, String templeName) {
        player.sendMessage("You successfully dropped an Offering in the Temple of " + templeName);

        Location[] templeLoc = this.temples.get(templeName);
        Location holoLoc = templeLoc[0].toVector().getMidpoint(templeLoc[1].toVector()).toLocation(player.getWorld()).add(0.5, 1, 0.5);

        Hologram hologram = HologramsAPI.createHologram(this, holoLoc);
        hologram.appendItemLine(itemDropped.getItemStack());

        ArrayList<ItemStack> rewards = getTempleRewards(templeName);
        ItemStack finalReward = this.rewards.get(templeName).next();

        new BukkitRunnable(){
            final double maximumY = holoLoc.getY() + 3;
            double time = 1;

            @Override
            public void run() {
                if(holoLoc.getY() >= maximumY){
                    concludeOffering(player, finalReward, hologram, templeName);

                    cancel();
                    return;
                }

                if(time % 4 == 0){
                    ItemStack item = getItemFromRewards(rewards);
                    item.setAmount(1);
                    hologram.clearLines();
                    hologram.appendItemLine(item);
                }

                holoLoc.setY(holoLoc.getY() + 0.02);
                hologram.teleport(holoLoc);

                time += 1;
            }
        }.runTaskTimer(this, 0, 1);
    }

    private void concludeOffering(Player player, ItemStack itemStack, Hologram hologram, String templeName) {
        player.sendMessage("Finished Offering with item " + itemStack.getType().toString());

        hologram.clearLines();
        hologram.appendItemLine(itemStack);

        new BukkitRunnable(){
            @Override
            public void run() {
                hologram.delete();
                player.getInventory().addItem(itemStack);
                currentlyInUse.put(templeName, false);
            }
        }.runTaskLater(this, 3*20);
    }


    // TODO: separate these into rewards.yml and temples.yml
    private void loadConfig(){
        // Temples
        Objects.requireNonNull(this.getConfig().getConfigurationSection("temples")).getKeys(false).forEach(key -> {
            ArrayList<Location> temples = (ArrayList<Location>) getConfig().get("temples." + key);
            Location[] templeLocations = new Location[2];

            assert temples != null;

            templeLocations[0] = temples.get(0);
            templeLocations[1] = temples.get(1);

            this.temples.put(key, templeLocations);
            this.currentlyInUse.put(key, false);
        });

        // Temple-specific Rewards
        Objects.requireNonNull(this.getConfig().getConfigurationSection("rewards")).getKeys(false).forEach(key -> {
            ArrayList<String> items = new ArrayList<>(this.getConfig().getStringList("rewards." + key));
            RandomCollection<ItemStack> randomCollection = new RandomCollection<>();

           for(String item : items){
               String[] itemValues = item.split(" ");

               try {
                   Material material = Material.valueOf(itemValues[0]);
                   randomCollection.add(Double.parseDouble(itemValues[2]), new ItemStack(material, Integer.parseInt(itemValues[1])));
               } catch(IllegalArgumentException ex){
                   getLogger().severe("[Offerings] Item '" + itemValues[0] + "' does not exist!");
               }
           }

            this.rewards.put(key, randomCollection);
        });
    }

    private void saveToConfig(){
        for(Map.Entry<String, Location[]> entry : this.temples.entrySet())
            this.getConfig().set("temples." + entry.getKey(), entry.getValue());

        this.saveConfig();
    }


    public HashMap<String, Location[]> getTemples() {
        return this.temples;
    }
    private ArrayList<ItemStack> getTempleRewards(String templeName){
        return this.rewards.get(templeName).collectionAsArray();
    }
    private ItemStack getItemFromRewards(ArrayList<ItemStack> rewards) {
        Collections.shuffle(rewards);
        return rewards.get(0);
    }
    public boolean getCurrentlyInUse(String templeName) {
        return this.currentlyInUse.get(templeName);
    }
    public void setCurrentlyInUse(String templeName, boolean inUse){
        this.currentlyInUse.put(templeName, inUse);
    }
}
