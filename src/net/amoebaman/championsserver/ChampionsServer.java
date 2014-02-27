package net.amoebaman.championsserver;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Logger;

import net.amoebaman.championsserver.tasks.*;
import net.amoebaman.championsserver.utils.*;
import net.amoebaman.utils.CommandController;

import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.*;

import com.google.common.collect.Lists;
import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;

@SuppressWarnings("deprecation")
public class ChampionsServer extends JavaPlugin{
	
	public static String mainDir;
	public static YamlConfiguration config, values;
	public static File configFile, valuesFile;	
	
	/** A list of all task IDs being run by this plugin */
	private static final List<Integer> tasks = new ArrayList<Integer>();
	
	/** The plugin's SQL handler */
	public static SQLHandler sql;
	
	/** A set of all block IDs considered transparent, automatically generated */
	public static final HashSet<Byte> transparent = new HashSet<Byte>();
	
	/** A set of all block IDs considered naturally spawning */
	public static final HashSet<Material> natural = new HashSet<Material>();
	
	/** A map of defined PT values for specific items */
	public static final HashMap<ItemStack, Double> ptValues = new HashMap<ItemStack, Double>();
	
	/** A map of defined drop chances, in decimal probabilities, for specific items */
	public static final HashMap<ItemStack, Double> dropChances = new HashMap<ItemStack, Double>();
	
	/** A map of defined purchase prices, in levels, for specific items */
	public static final HashMap<ItemStack, Integer> buyPrices = new HashMap<ItemStack, Integer>();
	
	/** A list of predefined enchantments that players are allowed to get */
	public static final List<Enchantment> enchants = Lists.newArrayList(
			Enchantment.ARROW_DAMAGE,
			Enchantment.ARROW_KNOCKBACK,
			Enchantment.ARROW_FIRE,
			Enchantment.ARROW_INFINITE,
			Enchantment.DAMAGE_ALL,
			Enchantment.FIRE_ASPECT,
			Enchantment.KNOCKBACK,
			Enchantment.OXYGEN,
			Enchantment.PROTECTION_ENVIRONMENTAL,
			Enchantment.PROTECTION_FALL,
			Enchantment.PROTECTION_FIRE,
			Enchantment.PROTECTION_PROJECTILE,
			Enchantment.THORNS
			);
	
	/** A list of all physical item drops representing shards */
	public static final List<Item> shardItems = new ArrayList<Item>();
	
	/** A list of all physical item drops representing legendary items */
	public static final Map<String, Item> legendItems = new HashMap<String, Item>();
	
    public void onEnable(){
		
		Bukkit.getPluginManager().registerEvents(new EventListener(), this);
		Bukkit.getPluginManager().registerEvents(new TradeHandler(), this);
		Bukkit.getPluginManager().registerEvents(new ShardHandler(), this);
		Bukkit.getPluginManager().registerEvents(new LegendaryHandler(), this);
		Bukkit.getPluginManager().registerEvents(new TalismanEffectTask(), this);
		CommandController.registerCommands(new CommandListener());
		
		getDataFolder().mkdirs();
		mainDir = getDataFolder().getPath();
		configFile = getConfigFile("config");
		valuesFile = getConfigFile("values");
		
		try{
			config = YamlConfiguration.loadConfiguration(configFile);
			CustomItems.loadCustomItems(config.getConfigurationSection("custom-items"));
			
			values = YamlConfiguration.loadConfiguration(valuesFile);
			ptValues.clear();
			for(String key : values.getConfigurationSection("pt-values").getKeys(false))
				ptValues.put(TradeHandler.getItem(key), values.getDouble("pt-values." + key, 0.0));
			dropChances.clear();
			for(String key : values.getConfigurationSection("drop-chances").getKeys(false))
				dropChances.put(TradeHandler.getItem(key), values.getDouble("drop-chances." + key, 0.0));
			buyPrices.clear();
			for(String key : values.getConfigurationSection("prices.buy").getKeys(false))
				buyPrices.put(TradeHandler.getItem(key), values.getInt("prices.buy." + key, -1));
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		sql = new SQLHandler(config.getString("database.url", "localhost"), config.getString("database.username", "root"), config.getString("database.password", "none"));
		
		for(int i = 0; i < sql.getNumShards(); i++)
			shardItems.add(null);
		
		tasks.add(Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new TalismanEffectTask(), 0L, 20L));
		tasks.add(Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new SeekerTask(), 0L, 20L));
		tasks.add(Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new SpawnItemsTask(), 0L, 100L));
		tasks.add(Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new ShardHandler(), 0L, 100L));
		tasks.add(Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new LegendaryHandler(), 0L, 100L));
		
		for(World world : Bukkit.getWorlds())
			world.setGameRuleValue("keepInventory", "true");
		
		transparent.clear();
		natural.clear();
		for(Material mat : Material.values()){
			if(mat.isBlock() && (!mat.isSolid() || !mat.isOccluding() || mat.isTransparent()))
				transparent.add((byte) mat.getId());
			switch(mat){
				case AIR: case GRASS: case DIRT: case COBBLESTONE: case STONE: case SAND: case SNOW: case SNOW_BLOCK: case GRAVEL:
				case LONG_GRASS: case YELLOW_FLOWER: case RED_ROSE: case DOUBLE_PLANT: case LOG: case LOG_2: case FENCE: case LEAVES: case LEAVES_2:
				case WATER: case STATIONARY_WATER: case LAVA: case STATIONARY_LAVA: case RED_MUSHROOM: case BROWN_MUSHROOM: case VINE:
				case WATER_LILY: case CACTUS: case DEAD_BUSH: case COCOA: case ICE: case PACKED_ICE:
					natural.add(mat);
				default:
			}
		}
		
		
	}
	
	public void onDisable(){
		logger().info("Despawning " + shardItems.size() + " dropped shards");
		for(Item i : ChampionsServer.shardItems)
			if(i != null)
				i.remove();
		logger().info("Despawning " + legendItems.size() + " dropped legends");
		for(Item i : ChampionsServer.legendItems.values())
			if(i != null)
				i.remove();
		logger().info("Saving player inventories to SQL database");
		for(Player player : Bukkit.getOnlinePlayers())
			ChampionsServer.sql.saveInventory(player);
		logger().info("Cancelling repeating tasks");
		for(int id : tasks)
			Bukkit.getScheduler().cancelTask(id);
		Bukkit.getScheduler().cancelTasks(this);
	}
	
	/**
	 * Statically gets a reference to this plugin from Bukkit's plugin manager
	 * @return this
	 */
	public static ChampionsServer plugin(){ return (ChampionsServer) Bukkit.getPluginManager().getPlugin("ChampionsServer"); }
	
	/**
	 * Statically gets a reference to this plugin's console logger from Bukkit's plugin manager
	 * @return this plugin's logger
	 */
	public static Logger logger(){ return plugin().getLogger(); }
	
	/**
	 * Gets a configuration file by its name (without the .yml extension).  If the file is not present and this plugin
	 * contains a default file, the default will be copied to the plugin folder and loaded.
	 * @param name the name of a configuration file
	 * @return the file reference
	 */
	public static File getConfigFile(String name){
		try{
			File file = new File(plugin().getDataFolder().getPath() + File.separator + name + ".yml");
			if(!file.exists()){
				plugin().getLogger().info("Loading pre-defined contents of " + name + ".yml");
				file.createNewFile();
				file.setWritable(true);
				InputStream preset = ChampionsServer.class.getResourceAsStream("/" + name + ".yml");
				if(preset != null){
					BufferedReader reader = new BufferedReader(new InputStreamReader(preset));
					BufferedWriter writer = new BufferedWriter(new FileWriter(file));
					while(reader.ready()){
						writer.write(reader.readLine());
						writer.newLine();
					}
					reader.close();
					writer.close();
				}
			}
			return file;
		}
		catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Checks for weapons or armor via a straight switch/case of materials matching.  Axes are considered weapons.
	 * @param mat a material
	 * @return true if mat is a weapon or piece of armor
	 */
	public static boolean isWeaponOrArmor(Material mat){
		switch(mat){
			case BOW:
			case WOOD_SWORD:
			case STONE_SWORD:
			case GOLD_SWORD:
			case IRON_SWORD:
			case DIAMOND_SWORD:
			case WOOD_AXE:
			case STONE_AXE:
			case GOLD_AXE:
			case IRON_AXE:
			case DIAMOND_AXE:
			case LEATHER_HELMET:
			case LEATHER_CHESTPLATE:
			case LEATHER_LEGGINGS:
			case LEATHER_BOOTS:
			case GOLD_HELMET:
			case GOLD_CHESTPLATE:
			case GOLD_LEGGINGS:
			case GOLD_BOOTS:
			case CHAINMAIL_HELMET:
			case CHAINMAIL_CHESTPLATE:
			case CHAINMAIL_LEGGINGS:
			case CHAINMAIL_BOOTS:
			case IRON_HELMET:
			case IRON_CHESTPLATE:
			case IRON_LEGGINGS:
			case IRON_BOOTS:
			case DIAMOND_HELMET:
			case DIAMOND_CHESTPLATE:
			case DIAMOND_LEGGINGS:
			case DIAMOND_BOOTS:
				return true;
			default:
				return false;
		}
	}
	
	/**
	 * Checks two itemstacks for similarity (NOT congruency).  Conditions for being the "same item" are same material,
	 * same data (unless that data is just durability, as in tools/weapons/armor), and same custom name if present.
	 * @param i1 an item
	 * @param i2 another item
	 * @return true if the items meet the criteria for similarity
	 */
	public static boolean sameItem(ItemStack i1, ItemStack i2){
		if(i1 == null || i2 == null)
			return false;
		if(i1.getType() == i2.getType() && (i1.getDurability() == i2.getDurability() || isWeaponOrArmor(i1.getType()))){
			if(!i1.hasItemMeta())
				i1.setItemMeta(Bukkit.getItemFactory().getItemMeta(i1.getType()));
			if(!i2.hasItemMeta())
				i2.setItemMeta(Bukkit.getItemFactory().getItemMeta(i2.getType()));
			
			if(i1.getItemMeta().hasDisplayName() && i2.getItemMeta().hasDisplayName())
				if(i1.getItemMeta().getDisplayName().equals(i2.getItemMeta().getDisplayName()))
					return true;
				else
					return false;
			else if(i1.getItemMeta().hasDisplayName() || i2.getItemMeta().hasDisplayName())
				return false;
			else
				return true;
		}
		else
			return false;
	}
	
	/**
	 * Checks if an item is of any interest whatsoever to our operations.  Interesting items are either
	 * worth PT, custom items defined in configuration, or the Wellspring items.
	 * @param stack
	 * @return
	 */
	public static boolean isInteresting(ItemStack stack){
		return getPtValue(stack) > 0 || CustomItems.isCustomItem(stack) || ShardHandler.isShard(stack) || ShardHandler.isCharm(stack);
	}
	
	/**
	 * Gets the PT score of an individual item, a composite score based on the value of its material, the
	 * amount of it, and the enchantments it has on it.
	 * @param item an item
	 * @return the PT score of the item
	 */
	public static int getPtValue(ItemStack item){
		if(item == null)
			return 0;
		double value = 0.0;
		for(ItemStack each : ptValues.keySet())
			if(sameItem(item, each) && ptValues.containsKey(each)){
				value = ptValues.get(each);
				break;
			}
		if(!CustomItems.isCustomItem(item)){
			for(Entry<Enchantment, Integer> entry : item.getEnchantments().entrySet()){
				switch(entry.getKey().getId()){
					case 1: //Fire Protection
					case 4: //Projectile Protection
					case 5: //Respiration
						value += (0.5 * entry.getValue());
						break;
					case 0: //Protection
					case 7: //Thorns
						value += (1 * entry.getValue());
						break;
					case 2: //Feather Falling
					case 19: //Knockback
					case 49: //Punch
						value += (2 * entry.getValue());
						break;
					case 16: //Sharpness
						value += (3 * entry.getValue());
						break;
					case 20: //Fire Aspect
					case 50: //Flame
						value += (5 * entry.getValue());
						break;
					case 48: //Power
						value += (5 + (3 * (entry.getValue() - 1)));
						break;
					case 51: //Infinity
						value += (10 * entry.getValue());
						break;
				}
			}
		}
		if(item.getAmount() > 1)
			value *= item.getAmount();
		return (int) Math.round(value);
	}
	
	/**
	 * Checks WorldGuard regions to see if PvP is permitted at a location.
	 * @param loc a location
	 * @return true if PvP is permitted at this location
	 */
	public static boolean isPvPZone(Location loc){
		ApplicableRegionSet regions = WGBukkit.getRegionManager(loc.getWorld()).getApplicableRegions(loc);
		return regions.allows(DefaultFlag.PVP);
	}
	
	/**
	 * Gets the sum of effects that should be granted based on all the talismans present in this inventory,
	 * one level per talisman for its respective effect.
	 * @param inv an inventory
	 * @return all the potion effects granted by talisman in the inventory
	 */
	public static Map<PotionEffectType, Integer> getTalismanEffects(Inventory inv){
		Map<PotionEffectType, Integer> effects = new HashMap<PotionEffectType, Integer>();
		for(PotionEffectType type : PotionEffectType.values()){
			int amount = TradeHandler.getAmountInInventory(CustomItems.get(Utils.getPotionEffectName(type).toLowerCase().replace(' ', '_') + "_talisman"), inv);
			if(amount > 0)
				effects.put(type, amount - 1);
		}
		return effects;
	}
	
	/**
	 * Gets the PT score of an inventory as a whole, a composite of the PT values of all its
	 * individual items, as well as all effects granted by talismans therein.
	 * @param inv an inventory
	 * @return the total PT score of the inventory
	 */
	public static int getPtValue(Inventory inv){
		List<ItemStack> contents = Lists.newArrayList(inv.getContents());
		if(inv instanceof PlayerInventory)
			for(ItemStack stack : ((PlayerInventory) inv).getArmorContents())
				contents.add(stack);
		
		double total = 0;
		for(ItemStack stack : contents)
			total +=getPtValue(stack);
		
		Map<PotionEffectType, Integer> effects = getTalismanEffects(inv);
		for(PotionEffectType type : effects.keySet()){
			switch(type.getId()){
				case 12: //Fire Resistance
				case 16: //Night Vision
				case 13: //Water Breathing
					total += 2;
					break;
				case 14: //Invisibility
					total += 25;
					break;
				case 8: //Jump Boost
					total += 2 * (effects.get(type) + 1);
					break;
				case 1: //Swiftness
					total += 5 * (effects.get(type) + 1);
					break;
				case 11: //Resistance
					total += 7 * (effects.get(type) + 1);
					break;
				case 21: //Health Boost
				case 22: //Absorption
					total += 5 * (effects.get(type) + 1);
					break;
				case 10: //Regeneration
				case 5: //Strength
					total += 4 * Math.pow(2, effects.get(type));
					break;
				case 9: //Nausea
					total -= 10;
					break;
				case 2: //Slowness
					total -= 3 * (effects.get(type) + 1);
					break;
				case 18: //Weakness
					total -= 2 * Math.pow(2, effects.get(type));
					break;
			}
		}
		
		return (int) total;
	}
	
	/**
	 * Gets the maximum PT score that a player may bring to bear in battle, a base amount added to by
	 * any boss charms in the player's inventory.
	 * @param player a player
	 * @return the player's PT score cap
	 */
	public static int getPtCap(Player player){
		return 35 + (TradeHandler.getAmountInInventory(CustomItems.get("boss_charm"), player.getInventory()) * 5);
	}
	
}
