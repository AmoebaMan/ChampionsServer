package net.amoebaman.championsserver.tasks;

import java.util.Map.Entry;

import net.amoebaman.championsserver.ChampionsServer;
import net.amoebaman.championsserver.CustomItems;
import net.amoebaman.championsserver.utils.Utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

public class SpawnItemsTask implements Runnable {
	
	private double attemptChance = 1;
	private double itemAttempts = 100;
	private double enchantChance = 0.3;
	
	private Block block;
	private Location loc;
	private ItemStack drop;
	private Enchantment enc;
	private Entry<ItemStack, Double> entry;
	
	public void run() {
		for(World world : Bukkit.getWorlds()){
			if(world.getEnvironment() == Environment.NORMAL && Math.random() < attemptChance){
				try{
					block = world.getBlockAt((int) ((Math.random() - 0.5) * 2000), 0, (int) ((Math.random() - 0.5) * 2000));
					if(block.getLocation().distance(block.getWorld().getSpawnLocation()) < 100 || block.getLocation().length() > 950)
						continue;
					while(block.getType().isSolid())
						block = block.getRelative(BlockFace.UP);
					if(block.isLiquid() || block.getRelative(BlockFace.UP).getType().isSolid())
						continue;
					
					boolean found = false;
					top: for(int x = -10; x <= 10; x++)
						for(int y = -10; y <= 10; y++)
							for(int z = -10; z <= 10; z++)
								if(!ChampionsServer.natural.contains(block.getRelative(x, y, z).getType())){
									found = true;
									break top;
								}
					if(!found)
						continue;
					
					loc = block.getLocation();
					
					drop = null;
					for(int k = 0; k < itemAttempts; k++){
						entry = Utils.getRandomElement(ChampionsServer.dropChances.entrySet());
						if(Math.random() > entry.getValue())
							drop = null;
						else
							drop = entry.getKey().clone();
					}
					
					if(drop != null && !CustomItems.isCustomItem(drop) && Math.random() < enchantChance){
						enc = null;
						int tries = 0;
						do{
							enc = Utils.getRandomElement(ChampionsServer.enchants);
							tries++;
						} while(!enc.canEnchantItem(drop) && tries < itemAttempts);
						if(enc.canEnchantItem(drop))
							drop.addEnchantment(enc, (int) (Math.random() * enc.getMaxLevel()) + 1);
					}
					
					if(drop != null){
						world.dropItem(loc, drop);
						world.unloadChunkRequest(block.getX(), block.getZ(), true);
						ChampionsServer.logger().info("Dropped " + drop + " at (" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")");
						if(ChampionsServer.getPtValue(drop) >= 10 || CustomItems.isCustomItem(drop))
							Bukkit.broadcastMessage(ChatColor.GREEN + "A rare or powerful item has dropped at (" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")!");
					}
				}
				catch(Exception e){ e.printStackTrace(); }
			}
		}
	}
	
}
