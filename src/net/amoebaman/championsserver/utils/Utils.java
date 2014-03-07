package net.amoebaman.championsserver.utils;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;
import java.util.Map.Entry;

import org.bukkit.*;
import org.bukkit.craftbukkit.libs.com.google.gson.stream.JsonReader;
import org.bukkit.craftbukkit.libs.com.google.gson.stream.JsonWriter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.potion.PotionEffectType;

import net.amoebaman.championsserver.ChampionsServer;
import net.minecraft.util.com.google.gson.stream.JsonToken;

public class Utils {

	public static LivingEntity getCulprit(EntityDamageByEntityEvent event){
		if(event.getDamager() instanceof LivingEntity)
			return (LivingEntity) event.getDamager();
		else if(event.getDamager() instanceof Projectile && ((Projectile) event.getDamager()).getShooter() instanceof LivingEntity)
			return (LivingEntity) ((Projectile) event.getDamager()).getShooter();
		else
			return null;
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

	public static String itemToString(ItemStack stack){
		if(stack == null || stack.getType() == Material.AIR)
			return "null";

		try{
			StringWriter string = new StringWriter();
			JsonWriter json = new JsonWriter(string);

			json.beginObject();
			json.name("type").value(stack.getType().name());
			json.name("data").value(stack.getDurability());
			json.name("amount").value(stack.getAmount());

			json.name("enchants").beginObject();
			for(Enchantment enc : stack.getEnchantments().keySet())
				json.name(enc.getName()).value(stack.getEnchantmentLevel(enc));
			if(stack.hasItemMeta() && stack.getItemMeta() instanceof EnchantmentStorageMeta){
				EnchantmentStorageMeta meta = (EnchantmentStorageMeta) stack.getItemMeta();
				for(Enchantment enc : meta.getEnchants().keySet())
					json.name(enc.getName()).value(meta.getEnchantLevel(enc));
			}
			json.endObject();

			if(stack.hasItemMeta()){
				ItemMeta meta = stack.getItemMeta();
				json.name("meta").beginObject();

				if(meta.hasDisplayName())
					json.name("name").value(meta.getDisplayName());
				if(meta.hasLore()){
					json.name("lore").beginArray();
					for(String line : meta.getLore())
						json.value(line);
					json.endArray();
				}
				if(meta instanceof LeatherArmorMeta)
					json.name("color").value(((LeatherArmorMeta) meta).getColor().asRGB());
				if(meta instanceof SkullMeta)
					json.name("skull").value(((SkullMeta) meta).getOwner());
				if(meta instanceof MapMeta)
					json.name("map").value(((MapMeta) meta).isScaling());

				json.endObject();
			}
			json.endObject();
			json.close();
			
			return string.toString();
		}
		catch(Exception e){
			e.printStackTrace();
			return "null";
		}
	}

	public static ItemStack itemFromString(String str){
		if(str == null || str.equals("null"))
			return null;

		ItemStack item = new ItemStack(Material.AIR);
		ItemMeta meta = Bukkit.getItemFactory().getItemMeta(item.getType());
		JsonReader json = new JsonReader(new StringReader(str));

		try{
			json.beginObject();

			while(json.hasNext() && !json.peek().equals(JsonToken.END_OBJECT)){
				String name = json.nextName();
				if(name.equals("type")){
					item.setType(Material.getMaterial(json.nextString()));
					meta = Bukkit.getItemFactory().getItemMeta(item.getType());
				}
				if(name.equals("data"))
					item.setDurability((short) json.nextInt());
				if(name.equals("amount"))
					item.setAmount(json.nextInt());
				if(name.equals("enchants")){
					json.beginObject();
					while(!json.peek().equals(JsonToken.END_OBJECT))
						if(meta instanceof EnchantmentStorageMeta)
							((EnchantmentStorageMeta) meta).addEnchant(Enchantment.getByName(json.nextName()), json.nextInt(), true);
						else
							meta.addEnchant(Enchantment.getByName(json.nextName()), json.nextInt(), true);
					json.endObject();
				}

				if(name.equals("meta")){
					json.beginObject();
					while(!json.peek().equals(JsonToken.END_OBJECT)){
						name = json.nextName();
						if(name.equals("name"))
							meta.setDisplayName(json.nextString());
						if(name.equals("lore")){
							json.beginArray();
							List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<String>();
							while(!json.peek().equals(JsonToken.END_ARRAY))
								lore.add(json.nextString());
							meta.setLore(lore);
							json.endArray();
						}
						if(name.equals("color"))
							try{ ((LeatherArmorMeta) meta).setColor(Color.fromRGB(json.nextInt())); } catch(Exception e){}
						if(name.equals("skull"))
							try{ ((SkullMeta) meta).setOwner(json.nextString()); } catch(Exception e){}
						if(name.equals("map"))
							try{ ((MapMeta) meta).setScaling(json.nextBoolean()); } catch(Exception e){}
					}
					json.endObject();
				}
			}

			json.endObject();
			json.close();
		}
		catch(Exception e){ e.printStackTrace(); }

		item.setItemMeta(meta);
		return item;
	}
	
	public static String jsonSave(Map<String, String> map){
		StringWriter string = new StringWriter();
		
		try{
			JsonWriter json = new JsonWriter(string);
			json.beginObject();
			for(Entry<String, String> entry : map.entrySet())
				json.name(entry.getKey()).value(entry.getValue());
			json.endObject();
			json.close();
		}
		catch(Exception e){}
		
		return string.toString();
	}
	
	public static Map<String, String> jsonLoad(String str){
		Map<String, String> map = new HashMap<String, String>();
		
		try{
			JsonReader json = new JsonReader(new StringReader(str));
			json.beginObject();
			while(!json.peek().equals(JsonToken.END_OBJECT))
				map.put(json.nextName(), json.nextString());
			json.endObject();
			json.close();
		}
		catch(Exception e){}
		
		return map;
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
