package net.amoebaman.championsserver.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import net.amoebaman.championsserver.ChampionsServer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffectType;

import com.google.common.collect.Lists;

public class Utils {
	
	public static <E> E getRandomElement(Collection<E> set){
		E element = null;
		Iterator<E> it = set.iterator();
		for(int i = 0; i < Math.random() * set.size() && it.hasNext(); i++)
			element = it.next();
		return element;
	}
	
	public static String friendlyItemString(ItemStack stack){
		String str = stack.getItemMeta().hasDisplayName() ? stack.getItemMeta().getDisplayName() : stack.getType().name().toLowerCase().replace("_", " ") + (stack.getAmount() > 1 ? "s" : "");
		str = stack.getAmount() + " " + str;
		str = ChatColor.stripColor(str);
		str = Utils.capitalize(str);
		if(!stack.getEnchantments().isEmpty())
			str += " with ";
		boolean first = true;
		for(Enchantment enc : stack.getEnchantments().keySet()){
			str += capitalize((first ? " " : ", ") + getEnchantmentName(enc)) + " " + romanNumerals(stack.getEnchantmentLevel(enc));
			first = false;
		}
		return str;
	}
	
	
	private static final String[] romanNumerals = new String[]{"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
	private static final int[] ints = new int[]{1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
	public static String romanNumerals(int number){
		if(number <= 0 || number > 4000)
			return String.valueOf(number);
		String roman = "";
		for(int i = 0; i < romanNumerals.length; i++)
			while(number >= ints[i]){
				number -= ints[i];
				roman += romanNumerals[i];
			}
		return roman;
	}
	
	public static String capitalize(String str){
		String[] words = str.split(" ");
		String result = "";
		for(String word : words)
			if(!word.isEmpty()){
				result += Character.toUpperCase(word.charAt(0));
				if(word.length() > 1)
					result += word.substring(1).toLowerCase();
				result += " ";
			}
		return result.trim();
	}
	
	private static final String keySeparator = "~k~";
	private static final String listSeparator = "~l~";
	
	public static String itemToString(ItemStack stack){
		if(stack == null || stack.getType() == Material.AIR)
			return "null";
		
		String itemString = "type=" + stack.getType().name() + keySeparator;
		itemString += "data=" + stack.getDurability() + keySeparator;
		itemString += "amount=" + stack.getAmount() + keySeparator;
		
		for(Enchantment enc : stack.getEnchantments().keySet())
			itemString += enc.getName() + "=" + stack.getEnchantmentLevel(enc) + keySeparator;
		
		if(stack.hasItemMeta()){
			ItemMeta meta = stack.getItemMeta();
			if(meta.hasDisplayName())
				itemString += "displayname=" + meta.getDisplayName() + keySeparator;
			if(meta.hasLore()){
				String loreString = "";
				boolean first = true;
				for(String str : meta.getLore()){
					if(!first)
						loreString += listSeparator;
					loreString += str;
					first = false;
				}
				itemString += "lore=" + loreString + keySeparator;
			}
			if(meta instanceof LeatherArmorMeta)
				itemString += "leathercolor=" + ((LeatherArmorMeta) meta).getColor().asRGB() + keySeparator;
			if(meta instanceof SkullMeta)
				itemString += "skullowner=" + ((SkullMeta) meta).getOwner() + keySeparator;
			if(meta instanceof EnchantmentStorageMeta)
				for(Enchantment enc : ((EnchantmentStorageMeta) meta).getEnchants().keySet())
					itemString += enc.getName() + "=" + ((EnchantmentStorageMeta) meta).getEnchantLevel(enc) + keySeparator;
		}
		
		return itemString.trim();
	}
	
	public static ItemStack itemFromString(String str){
		if(str == null || str.equals("null"))
			return null;
		
		ItemStack stack = new ItemStack(Material.getMaterial(valueFromKeyInString(str, "type")));
		stack.setDurability(Short.parseShort(valueFromKeyInString(str, "data")));
		stack.setAmount(Integer.parseInt(valueFromKeyInString(str, "amount")));
		
		ItemMeta meta = Bukkit.getItemFactory().getItemMeta(stack.getType());
		
		for(Enchantment enc : Enchantment.values())
			if(stringContainsKey(str, enc.getName())){
				if(meta instanceof EnchantmentStorageMeta)
					((EnchantmentStorageMeta) meta).addEnchant(enc, Integer.parseInt(valueFromKeyInString(str, enc.getName())), true);
				else
					meta.addEnchant(enc, Integer.parseInt(valueFromKeyInString(str, enc.getName())), true);
			}
		
		if(stringContainsKey(str, "displayname"))
			meta.setDisplayName(valueFromKeyInString(str, "displayname"));
		if(stringContainsKey(str, "lore"))
			meta.setLore(Lists.newArrayList(valueFromKeyInString(str, "lore").split(listSeparator)));
		if(stringContainsKey(str, "leathercolor") && meta instanceof LeatherArmorMeta)
			((LeatherArmorMeta) meta).setColor(Color.fromRGB(Integer.parseInt(valueFromKeyInString(str, "leathercolor"))));
		if(stringContainsKey(str, "skullowner") && meta instanceof SkullMeta)
			((SkullMeta) meta).setOwner(valueFromKeyInString(str, "skullowner"));
		
		stack.setItemMeta(meta);
		
		return stack;
	}
	
	public static boolean stringContainsKey(String str, String key){
		return str.contains(keySeparator + key);
	}
	
	public static String valueFromKeyInString(String str, String key){
		if(!str.contains(key))
			return null;
		return str.substring(str.indexOf(key) + key.length() + 1, str.indexOf(keySeparator, str.indexOf(key)));
	}
	
	public static String generateDataString(Map<String, String> data){
		String str = "";
		for(Entry<String, String> entry : data.entrySet())
			str += keySeparator + entry.getKey() + "=" + entry.getValue();
		str += keySeparator;
		return str;
	}
	
	public static Map<String, String> parseDataString(String str){
		Map<String, String> data = new HashMap<String, String>();
		if(!str.contains(keySeparator))
			return data;
		for(String sub : str.split(keySeparator))
			if(sub.contains("="))
				data.put(sub.substring(0, sub.indexOf('=')), sub.substring(sub.indexOf('=') + 1));
		return data;
	}
	
	public static double centerAngle(double angle, double center){
		while(angle >= center + 180)
			angle -= 360;
		while(angle < center - 180)
			angle += 360;
		return angle;
	}
	
	public static String getPotionEffectName(PotionEffectType effect){
		if(effect == null)
			return "null";
		if(effect.equals(PotionEffectType.BLINDNESS)) return "blindness";
		if(effect.equals(PotionEffectType.CONFUSION)) return "nausea";
		if(effect.equals(PotionEffectType.DAMAGE_RESISTANCE)) return "resistance";
		if(effect.equals(PotionEffectType.FAST_DIGGING)) return "haste";
		if(effect.equals(PotionEffectType.FIRE_RESISTANCE)) return "fire resistance";
		if(effect.equals(PotionEffectType.HARM)) return "harming";
		if(effect.equals(PotionEffectType.HEAL)) return "instant health";
		if(effect.equals(PotionEffectType.HUNGER)) return "hunger";
		if(effect.equals(PotionEffectType.INCREASE_DAMAGE)) return "strength";
		if(effect.equals(PotionEffectType.INVISIBILITY)) return "invisibility";
		if(effect.equals(PotionEffectType.JUMP)) return "jump boost";
		if(effect.equals(PotionEffectType.NIGHT_VISION)) return "night vision";
		if(effect.equals(PotionEffectType.POISON)) return "poison";
		if(effect.equals(PotionEffectType.REGENERATION)) return "regeneration";
		if(effect.equals(PotionEffectType.SLOW)) return "slowness";
		if(effect.equals(PotionEffectType.SLOW_DIGGING)) return "mining fatigue";
		if(effect.equals(PotionEffectType.SPEED)) return "swiftness";
		if(effect.equals(PotionEffectType.WATER_BREATHING)) return "water breathing";
		if(effect.equals(PotionEffectType.WEAKNESS)) return "weakness";
		if(effect.equals(PotionEffectType.WITHER)) return "wither";
		if(effect.equals(PotionEffectType.ABSORPTION)) return "absorption";
		if(effect.equals(PotionEffectType.HEALTH_BOOST)) return "health boost";
		if(effect.equals(PotionEffectType.SATURATION)) return "saturation";
		return "unknown";
	}
	
	public static String getEnchantmentName(Enchantment enc){
		if(enc.equals(Enchantment.ARROW_DAMAGE)) return "power";
		if(enc.equals(Enchantment.ARROW_FIRE)) return "flame";
		if(enc.equals(Enchantment.ARROW_INFINITE)) return "infinity";
		if(enc.equals(Enchantment.ARROW_KNOCKBACK)) return "punch";
		if(enc.equals(Enchantment.DAMAGE_ALL)) return "sharpness";
		if(enc.equals(Enchantment.DAMAGE_ARTHROPODS)) return "bane of arthropods";
		if(enc.equals(Enchantment.DAMAGE_UNDEAD)) return "smite";
		if(enc.equals(Enchantment.DIG_SPEED)) return "efficiency";
		if(enc.equals(Enchantment.DURABILITY)) return "unbreaking";
		if(enc.equals(Enchantment.FIRE_ASPECT)) return "fire aspect";
		if(enc.equals(Enchantment.KNOCKBACK)) return "knockback";
		if(enc.equals(Enchantment.LOOT_BONUS_BLOCKS)) return "fortune";
		if(enc.equals(Enchantment.LOOT_BONUS_MOBS)) return "looting";
		if(enc.equals(Enchantment.OXYGEN)) return "respiration";
		if(enc.equals(Enchantment.PROTECTION_ENVIRONMENTAL)) return "protection";
		if(enc.equals(Enchantment.PROTECTION_EXPLOSIONS)) return "blast protection";
		if(enc.equals(Enchantment.PROTECTION_FALL)) return "feather falling";
		if(enc.equals(Enchantment.PROTECTION_FIRE)) return "fire protection";
		if(enc.equals(Enchantment.PROTECTION_PROJECTILE)) return "projectile protection";
		if(enc.equals(Enchantment.SILK_TOUCH)) return "silk touch";
		if(enc.equals(Enchantment.THORNS)) return "thorns";
		if(enc.equals(Enchantment.WATER_WORKER)) return "aqua affinity";
		return null;
	}
	
	public static Inventory entityGearToInv(EntityEquipment gear){
		Inventory inv = Bukkit.createInventory(null, 9);
		inv.addItem(gear.getItemInHand(), gear.getHelmet(), gear.getChestplate(), gear.getLeggings(), gear.getBoots());
		return inv;
	}
	
	public static long timeSinceLastPlay(OfflinePlayer player){
		return System.currentTimeMillis() - player.getLastPlayed();
	}
	
	public static int hoursToMillis(int hours){
		return 1000 * 60 * 60 * hours;
	}
	
	public static boolean shouldRespawnItem(Item item){
		return item == null || !item.isValid() || item.getLocation().getY() < 0 || item.getLocation().getBlock().isLiquid();
	}
	
	public static boolean checkAndKillExtras(OfflinePlayer player, ItemStack stack, boolean wipeAll){
		Inventory inv = null;
		boolean found = false;
		/*
		 * Check the player's inventory
		 */
		inv = player.isOnline() ? player.getPlayer().getInventory() : ChampionsServer.sql.getInventory(player);
		for(int i = 0; i < inv.getSize(); i++)
			if(ChampionsServer.sameItem(inv.getItem(i), stack)){
				if(found || wipeAll)
					inv.setItem(i, null);
				else
					inv.getItem(i).setAmount(1);
				found = true;
			}
		/*
		 * Check the player's ender chest
		 */
		inv = player.isOnline() ? player.getPlayer().getEnderChest() : ChampionsServer.sql.loadChest(player);
		for(int i = 0; i < inv.getSize(); i++)
			if(ChampionsServer.sameItem(inv.getItem(i), stack)){
				if(found || wipeAll)
					inv.setItem(i, null);
				else
					inv.getItem(i).setAmount(1);
				found = true;
			}
		/*
		 * Check the item on the player's cursor
		 */
		if(player.isOnline() && ChampionsServer.sameItem(player.getPlayer().getItemOnCursor(), stack)){
			if(found || wipeAll)
				player.getPlayer().setItemOnCursor(null);
			else
				player.getPlayer().getItemOnCursor().setAmount(1);
			found = true;
		}
		
		return found;
	}
	
}
