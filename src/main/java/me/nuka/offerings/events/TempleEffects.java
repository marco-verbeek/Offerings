package me.nuka.offerings.events;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import me.nuka.offerings.Main;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class TempleEffects {
    Main plugin;

    public TempleEffects(Main plugin){
        this.plugin = plugin;
    }

    public boolean play(String templeName, Location minLoc, Location maxLoc, Location centerLoc){
        // TODO: temple effects for War, NuKa, Bees, ...

        if(templeName.equalsIgnoreCase("NuKa")){

        } else if(templeName.equalsIgnoreCase("Knowledge")){
            knowledgeLectern(centerLoc);
            return true;
        } else if(templeName.equalsIgnoreCase("War")){

        }

        return false;
    }

    public void knowledgeLectern(Location centerLoc){
        Location finalCenterLoc = centerLoc.clone();

        Location front = centerLoc.clone().add(1, 0, 0);
        Location back = centerLoc.clone().subtract(1, 0, 0);

        Hologram frontHolo = HologramsAPI.createHologram(plugin, front);
        Hologram backHolo = HologramsAPI.createHologram(plugin, back);

        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK, 1);
        frontHolo.appendItemLine(book);
        backHolo.appendItemLine(book);

        new BukkitRunnable(){
            int frontDegree = 0;
            int backDegree = 180;

            double multiplier = 1;
            int rounds = 0;

            @Override
            public void run() {
                if(rounds == 3){
                    frontHolo.delete();
                    backHolo.delete();

                    cancel();
                    return;
                }

                double radiansF = Math.toRadians(frontDegree);
                double xF = Math.cos(radiansF) * multiplier;
                double zF = Math.sin(radiansF) * multiplier;

                double radiansB = Math.toRadians(backDegree);
                double xB = Math.cos(radiansB) * multiplier;
                double zB = Math.sin(radiansB) * multiplier;

                //plugin.getLogger().severe(String.format("[Offerings] RADIANS TICK: degree: %d, x: %f, z: %f", degree, x, z));

                Location tempFront = front.add(0, 0.012, 0).clone().add(xF-1,0,zF);
                Location tempBack = back.add(0, 0.012, 0).clone().add(xB+1,0,zB);

                frontHolo.teleport(tempFront);
                backHolo.teleport(tempBack);

                // TODO: please fix this code bcs it really is ugly
                if(frontDegree == 360){
                    //finalCenterLoc.getWorld().(Particle.ENCHANTMENT_TABLE, finalCenterLoc, 10);
                    finalCenterLoc.add(0, 1, 0);

                    frontDegree = 0;
                    rounds++;
                } else {
                    if(frontDegree % 90 == 0)
                        multiplier -= 0.05;
                    frontDegree += 6;
                }

                if(backDegree == 360){
                    backDegree = 0;
                } else {
                    backDegree += 6;
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}
