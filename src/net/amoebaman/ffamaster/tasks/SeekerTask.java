package net.amoebaman.ffamaster.tasks;

import java.util.*;

import net.amoebaman.ffamaster.CustomItems;
import net.amoebaman.ffamaster.FFAMaster;
import net.amoebaman.ffamaster.LegendaryHandler;
import net.amoebaman.ffamaster.ShardHandler;
import net.amoebaman.ffamaster.TradeHandler;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SeekerTask implements Runnable{
	
	public static final Map<Player, Player> LAST_TARGET = new HashMap<Player, Player>();
	public static final Map<Player, Mode> MODES = new HashMap<Player, Mode>();
	
	public void run() {
		for(Player player : Bukkit.getOnlinePlayers()){
			
			if(!MODES.containsKey(player))
				MODES.put(player, Mode.EQUAL);
			Mode mode = MODES.get(player);
			
			if(TradeHandler.getAmountInInventory(CustomItems.get("seeker"), player.getInventory()) > 0){
				
				double range = 250 + (25 * TradeHandler.getAmountInInventory(CustomItems.get("clairvoyance_charm"), player.getInventory()));
				Set<Player> matches = new HashSet<Player>();
				for(Player each : Bukkit.getOnlinePlayers())
					if(each.getLocation().distance(player.getLocation()) < range){
						boolean conditions = false;
						switch(mode){
							case BOSSES:
								conditions = TradeHandler.getAmountInInventory(CustomItems.get("boss_charm"), each.getInventory()) > 0;
								break;
							case EQUAL:
								conditions = Math.abs(FFAMaster.getPtValue(player.getInventory()) - FFAMaster.getPtValue(each.getInventory())) <= 5;
								break;
							case LEGENDS:
								for(ItemStack item : each.getInventory().getContents())
									if(LegendaryHandler.isLegend(item))
										conditions = true;
								break;
							case SHARDS:
								for(ItemStack item : each.getInventory().getContents())
									if(ShardHandler.isShard(item) || ShardHandler.isCharm(item))
										conditions = true;
								break;
							case STRONGER:
								conditions = FFAMaster.getPtValue(each.getInventory()) - FFAMaster.getPtValue(player.getInventory()) > 5;
								break;
						}
						if(conditions)
							matches.add(each);
					}
				matches.remove(player);
				if(matches.isEmpty()){
					if(LAST_TARGET.get(player) != null)
						player.sendMessage(ChatColor.DARK_PURPLE + " Seeker has lost its target");
					player.setCompassTarget(player.getWorld().getSpawnLocation());
					LAST_TARGET.put(player, null);
					continue;
				}
				
				Player closest = null;
				for(Player each : matches)
					if(each.getLocation().distance(player.getLocation()) < range){
						range = each.getLocation().distance(player.getLocation());
						closest = each;
					}
				
				player.setCompassTarget(closest.getEyeLocation());
				
				if(LAST_TARGET.get(player) == null)
					player.sendMessage(ChatColor.DARK_PURPLE + " Seeker has acquired a target");
				else if(!closest.equals(LAST_TARGET.get(player)))
					player.sendMessage(ChatColor.DARK_PURPLE + " Seeker has switched to a new target");
				
				LAST_TARGET.put(player, closest);
			}
		}
	}
	
	public enum Mode{
		EQUAL,
		STRONGER,
		SHARDS,
		BOSSES,
		LEGENDS,
	}
	
}
