package net.amoebaman.ffamaster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.amoebaman.kitmaster.controllers.ItemController;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CustomItems {
	
	private static final Map<String, ItemStack> customItems = new HashMap<String, ItemStack>();
	
	public static void loadCustomItems(ConfigurationSection section){
		for(String key : section.getKeys(false)){
			try{
				ConfigurationSection sec = section.getConfigurationSection(key);
				ItemStack stack = ItemController.parseItem(sec.getString("item"));
				ItemMeta meta = stack.getItemMeta();
				meta.setDisplayName(ChatColor.RESET + ChatColor.translateAlternateColorCodes('&', sec.getString("name")));
				List<String> list = sec.getStringList("lore");
				List<String> lore = new ArrayList<String>();
				for(String str : list)
					lore.add(ChatColor.YELLOW + ChatColor.translateAlternateColorCodes('&', str));
				meta.setLore(lore);
				stack.setItemMeta(meta);
				
				customItems.put(key.toLowerCase().replace(' ', '_'), stack);
			}
			catch(Exception e){
				FFAMaster.logger().severe("Unable to load custom item: " + key);
				e.printStackTrace();
			}
		}
	}

	public static ItemStack get(String name){
		return customItems.get(name.toLowerCase().replace(' ', '_'));
	}
	
	/**
	 * Gets the storage name (lowercase, underscored) for this object.
	 * For the friendly name, use {@link TradeMaster#getName(ItemStack)}.
	 * @param item
	 * @return
	 */
	public static String getName(ItemStack item){
		for(String name : customItems.keySet())
			if(FFAMaster.sameItem(item, customItems.get(name)))
				return name;
		return null;
	}
	
	public static boolean isCustomItem(ItemStack item){
		return getName(item) != null;
	}
	
	public static boolean isCustomItem(String name){
		return customItems.containsKey(name.toLowerCase().replace(' ', '_'));
	}
	
}
