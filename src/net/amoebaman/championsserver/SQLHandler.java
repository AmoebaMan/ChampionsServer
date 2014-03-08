package net.amoebaman.championsserver;

import java.sql.*;
import java.util.*;

import net.amoebaman.utils.JsonReader;
import net.amoebaman.utils.JsonWriter;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.*;

@SuppressWarnings("resource")
public class SQLHandler {
	
	Connection conn = null;
	
	final String db_name = "ffamaster";
	
	final String inventory_table = "inventories";
	final String player_column = "player";
	final String inventory_column = "inventory";
	final String armor_column = "armor";
	final String chest_column = "chest";
	
	final String misc_table = "misc";
	final String datastring_column = "datastring";
	
	final String shards_table = "shards";
	final String number_column = "number";
	final String spawn_loc_column = "spawn";
	
	final String legends_table = "legends";
	final String name_column = "name";
	final String holder_column = "holder";
	
	final String player_macro = "%player%";
	final String name_macro = "%name%";
	final String number_macro = "%number%";
	final String value_macro = "%value%";
	
	final String create_database = "CREATE DATABASE IF NOT EXISTS " + db_name;
	final String use_database = "USE " + db_name;
	
	final String create_inventory_table =
			"CREATE TABLE IF NOT EXISTS " + inventory_table + "( " +
					player_column + " VARCHAR(16) NOT NULL, " +
					inventory_column + " TEXT, " +
					armor_column + " TEXT, " +
					chest_column + " TEXT, " +
					"PRIMARY KEY (" + player_column + ") " +
					")";
	
	final String create_misc_table = 
			"CREATE TABLE IF NOT EXISTS " + misc_table + "( " +
					player_column + " VARCHAR(16) NOT NULL, " +
					datastring_column + " TEXT, " +
					"PRIMARY KEY (" + player_column + ") " +
					")";
	
	final String create_shards_table = 
			"CREATE TABLE IF NOT EXISTS " + shards_table + "( " +
					number_column + " INT NOT NULL, " +
					holder_column + " VARCHAR(16) DEFAULT 'none', " +
					spawn_loc_column + " TEXT, " +
					"PRIMARY KEY (" + number_column + ") " +
					")";
	
	final String create_legends_table =
			"CREATE TABLE IF NOT EXISTS " + legends_table + "( " +
					name_column + " VARCHAR(50) NOT NULL, " +
					holder_column + " VARCHAR(16) DEFAULT 'none', " +
					spawn_loc_column + " TEXT, " +
					"PRIMARY KEY (" + name_column + ") " +
					")";
	
	final String add_player_inventory =
			"INSERT INTO " + inventory_table + "(" + player_column + ", " + inventory_column + ", " + armor_column + ", " + chest_column + ") " +
					"VALUES('" + player_macro + "', 'empty', 'empty', 'empty')";
	
	final String add_player_misc =
			"INSERT INTO " + misc_table + "(" + player_column + ", " + datastring_column + ") " +
					"VALUES('" + player_macro + "', 'empty')";
	
	final String update_player_inventory =
			"UPDATE " + inventory_table + " " +
					"SET " + inventory_column + " = '" + value_macro + "' " +
					"WHERE " + player_column + " = '" + player_macro + "'";
	
	final String update_player_armor =
			"UPDATE " + inventory_table + " " +
					"SET " + armor_column + " = '" + value_macro + "' " +
					"WHERE " + player_column + " = '" + player_macro + "'";
	
	final String update_player_chest =
			"UPDATE " + inventory_table + " " +
					"SET " + chest_column + " = '" + value_macro + "' " +
					"WHERE " + player_column + " = '" + player_macro + "'";
	
	final String update_player_datastring =
			"UPDATE " + misc_table + " " +
					"SET " + datastring_column + " = '" + value_macro + "' " +
					"WHERE " + player_column + " = '" + player_macro + "'";
	
	final String get_player_inventory =
			"SELECT " + inventory_column + " FROM " + inventory_table + " " +
					"WHERE " + player_column + " = '" + player_macro + "'";
	
	final String get_player_armor =
			"SELECT " + armor_column + " FROM " + inventory_table + " " +
					"WHERE " + player_column + " = '" + player_macro + "'";
	
	final String get_player_chest =
			"SELECT " + chest_column + " FROM " + inventory_table + " " +
					"WHERE " + player_column + " = '" + player_macro + "'";
	
	final String get_player_datastring =
			"SELECT " + datastring_column + " FROM " + misc_table + " " +
					"WHERE " + player_column + " = '" + player_macro + "'";
	
	final String set_shard_holder = 
			"INSERT INTO " + shards_table + "(" + number_column + ", " + holder_column + ") " +
					"VALUES(" + number_macro + ", '" + value_macro + "')" +
							"ON DUPLICATE KEY UPDATE " + holder_column + " = '" + value_macro + "'";
	
	final String set_shard_spawn = 
			"INSERT INTO " + shards_table + "(" + number_column + ", " + spawn_loc_column + ") " +
					"VALUES(" + number_macro + ", '" + value_macro + "')" +
							"ON DUPLICATE KEY UPDATE " + spawn_loc_column + " = '" + value_macro + "'";
	
	final String get_shard_holder = 
			"SELECT " + holder_column + " FROM " + shards_table + " " +
					"WHERE " + number_column + " = " + number_macro;
	
	final String get_shard_spawn = 
			"SELECT " + spawn_loc_column + " FROM " + shards_table + " " +
					"WHERE " + number_column + " = " + number_macro;
	
	final String get_num_shards = 
			"SELECT * FROM " + shards_table + " ORDER BY " + number_column + " ASC";
	
	final String set_legend_holder = 
			"INSERT INTO " + legends_table + "(" + name_column + ", " + holder_column + ") " +
					"VALUES('" + name_macro + "', '" + value_macro + "')" +
							"ON DUPLICATE KEY UPDATE " + holder_column + " = '" + value_macro + "'";
	
	final String set_legend_spawn = 
			"INSERT INTO " + legends_table + "(" + name_column + ", " + spawn_loc_column + ") " +
					"VALUES('" + name_macro + "', '" + value_macro + "')" +
							"ON DUPLICATE KEY UPDATE " + spawn_loc_column + " = '" + value_macro + "'";
	
	final String get_legend_holder = 
			"SELECT " + holder_column + " FROM " + legends_table + " " +
					"WHERE " + name_column + " = '" + name_macro + "'";
	
	final String get_legend_spawn = 
			"SELECT " + spawn_loc_column + " FROM " + legends_table + " " +
					"WHERE " + name_column + " = '" + name_macro + "'";
	
	final String get_legends =
			"SELECT * FROM " + legends_table;
	
	public SQLHandler(String url, String user, String pass){
		try{
			Class.forName("com.mysql.jdbc.Driver");
			if(pass.equals("none"))
				conn = DriverManager.getConnection("jdbc:mysql://" + url + "/");
			else
				conn = DriverManager.getConnection("jdbc:mysql://" + url + "/", user, pass);
			
			conn.prepareStatement(create_database).execute();
			conn.prepareStatement(use_database).execute();
			conn.prepareStatement(create_inventory_table).execute();
			conn.prepareStatement(create_misc_table).execute();
			conn.prepareStatement(create_shards_table).execute();
			conn.prepareStatement(create_legends_table).execute();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void saveInventory(Player player){
		
		String itemStr = new JsonWriter().writeItemList(player.getInventory().getContents()).closeOut();
		String armorStr = new JsonWriter().writeItemList(player.getInventory().getArmorContents()).closeOut();
		
		try{
			ResultSet existsTest = conn.prepareStatement(get_player_inventory.replace(player_macro, player.getName())).executeQuery();
			if(!existsTest.first())
				conn.prepareStatement(add_player_inventory.replace(player_macro, player.getName())).execute();
			
			conn.prepareStatement(update_player_inventory.replace(player_macro, player.getName()).replace(value_macro, itemStr.toString())).executeUpdate();
			conn.prepareStatement(update_player_armor.replace(player_macro, player.getName()).replace(value_macro, armorStr.toString())).executeUpdate();
			
		}
		catch (SQLException e) { e.printStackTrace(); }
	}
	
	public void loadInventory(Player player) {
		String itemStr = "empty";
		String armorStr = "empty";
		
		try{
			ResultSet existsTest = conn.prepareStatement(get_player_inventory.replace(player_macro, player.getName())).executeQuery();
			if(!existsTest.first())
				conn.prepareStatement(add_player_inventory.replace(player_macro, player.getName())).execute();
			
			ResultSet result = conn.prepareStatement(get_player_inventory.replace(player_macro, player.getName())).executeQuery();
			if(result.first())
				itemStr = result.getString(inventory_column);
			
			result = conn.prepareStatement(get_player_armor.replace(player_macro, player.getName())).executeQuery();
			if(result.first())
				armorStr = result.getString(armor_column);
		}
		catch (SQLException e) {
			e.printStackTrace();
		}

		player.getInventory().setContents(new JsonReader(itemStr).readItemList().toArray(new ItemStack[36]));
		player.getInventory().setArmorContents(new JsonReader(armorStr).readItemList().toArray(new ItemStack[4]));
	}
	
	public void setInventory(OfflinePlayer player, Inventory inv){

		String invStr = new JsonWriter().writeItemList(inv.getContents()).closeOut();
		
		try{
			ResultSet existsTest = conn.prepareStatement(get_player_inventory.replace(player_macro, player.getName())).executeQuery();
			if(!existsTest.first())
				conn.prepareStatement(add_player_inventory.replace(player_macro, player.getName())).execute();
			
			conn.prepareStatement(update_player_inventory.replace(player_macro, player.getName()).replace(value_macro, invStr.toString())).executeUpdate();
			
		}
		catch (SQLException e) { e.printStackTrace(); }
	}

    public Inventory getInventory(OfflinePlayer player) {
		String invStr = "empty";
		
		try{
			ResultSet existsTest = conn.prepareStatement(get_player_inventory.replace(player_macro, player.getName())).executeQuery();
			if(!existsTest.first())
				conn.prepareStatement(add_player_inventory.replace(player_macro, player.getName())).execute();
			
			ResultSet result = conn.prepareStatement(get_player_inventory.replace(player_macro, player.getName())).executeQuery();
			if(result.first())
				invStr = result.getString(inventory_column);
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		
		Inventory inv = Bukkit.createInventory(null, InventoryType.PLAYER);
		inv.setContents(new JsonReader(invStr).readItemList().toArray(new ItemStack[36]));
		return inv;
	}
	
	public void saveChest(OfflinePlayer player, Inventory inv){

		String invStr = new JsonWriter().writeItemList(inv.getContents()).closeOut();
		
		try{
			ResultSet existsTest = conn.prepareStatement(get_player_inventory.replace(player_macro, player.getName())).executeQuery();
			if(!existsTest.first())
				conn.prepareStatement(add_player_inventory.replace(player_macro, player.getName())).execute();
			
			conn.prepareStatement(update_player_chest.replace(player_macro, player.getName()).replace(value_macro, invStr.toString())).executeUpdate();
			
		}
		catch (SQLException e) { e.printStackTrace(); }
	}
	
	public Inventory loadChest(OfflinePlayer player) {
		String invStr = "empty";
		
		try{
			ResultSet existsTest = conn.prepareStatement(get_player_inventory.replace(player_macro, player.getName())).executeQuery();
			if(!existsTest.first())
				conn.prepareStatement(add_player_inventory.replace(player_macro, player.getName())).execute();
			
			ResultSet result = conn.prepareStatement(get_player_chest.replace(player_macro, player.getName())).executeQuery();
			if(result.first())
				invStr = result.getString(chest_column);
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		
		Inventory inv = Bukkit.createInventory(null, 27, player.getName() + "'s storage");
		inv.setContents(new JsonReader(invStr).readItemList().toArray(new ItemStack[27]));
		return inv;
	}
	
	public void saveDataString(OfflinePlayer player, String dataString){
		try{
			ResultSet existsTest = conn.prepareStatement(get_player_datastring.replace(player_macro, player.getName())).executeQuery();
			if(!existsTest.first())
				conn.prepareStatement(add_player_misc.replace(player_macro, player.getName())).execute();
			
			conn.prepareStatement(update_player_datastring.replace(player_macro, player.getName()).replace(value_macro, dataString)).executeUpdate();
			
		}
		catch (SQLException e) { e.printStackTrace(); }
	}
	
	public String loadDataString(OfflinePlayer player) {
		
		try{
			ResultSet existsTest = conn.prepareStatement(get_player_datastring.replace(player_macro, player.getName())).executeQuery();
			if(!existsTest.first())
				conn.prepareStatement(add_player_misc.replace(player_macro, player.getName())).execute();
			
			ResultSet result = conn.prepareStatement(get_player_datastring.replace(player_macro, player.getName())).executeQuery();
			if(result.first())
				return result.getString(datastring_column);
			else
				return "empty";
		}
		catch (SQLException e) {
			e.printStackTrace();
			return "empty";
		}
	}

	public void setShardHolder(int number, OfflinePlayer player){
		try{
			conn.prepareStatement(set_shard_holder.replace(number_macro, "" + number).replace(value_macro, player == null ? "none" : player.getName())).executeUpdate();
		}
		catch(SQLException sqle) { sqle.printStackTrace(); }
	}
	
	public OfflinePlayer getShardHolder(int number){
		try{
			ResultSet result = conn.prepareStatement(get_shard_holder.replace(number_macro, "" + number)).executeQuery();
			if(result.first() && result.getString(holder_column) != null){
				OfflinePlayer player = Bukkit.getOfflinePlayer(result.getString(holder_column));
				if(player.hasPlayedBefore())
					return player;
				else
					return null;
			}
			else
				return null;
		}
		catch(SQLException sqle) {
			sqle.printStackTrace();
			return null;
		}
	}
	
	public void setShardSpawn(int number, Location loc){
		try{
			String locStr = loc.getWorld().getName() + "@" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
			conn.prepareStatement(set_shard_spawn.replace(number_macro, "" + number).replace(value_macro, locStr)).executeUpdate();
		}
		catch(SQLException sqle) { sqle.printStackTrace(); }
	}
	
	public Location getShardSpawn(int number){
		try{
			ResultSet result = conn.prepareStatement(get_shard_spawn.replace(number_macro, "" + number)).executeQuery();
			if(result.first()){
				String locStr = result.getString(spawn_loc_column);
				if(locStr == null || locStr.isEmpty())
					return null;
				String[] coords = locStr.substring(locStr.indexOf('@') + 1).split(",");
				return new Location(Bukkit.getWorld(locStr.substring(0, locStr.indexOf('@'))), Integer.parseInt(coords[0]), Integer.parseInt(coords[1]), Integer.parseInt(coords[2]));
			}
			else
				return null;
		}
		catch(SQLException sqle) {
			sqle.printStackTrace();
			return null;
		}
	}
	
	public int getNumShards(){
		try{
			ResultSet result = conn.prepareStatement(get_num_shards).executeQuery();
			if(result.last())
				return result.getInt(number_column) + 1;
			else
				return 0;
		}
		catch(SQLException sqle) {
			sqle.printStackTrace();
			return 0;
		}
	}

	public void setLegendHolder(String name, OfflinePlayer player){
		try{
			conn.prepareStatement(set_legend_holder.replace(name_macro, name).replace(value_macro, player == null ? "none" : player.getName())).executeUpdate();
		}
		catch(SQLException sqle) { sqle.printStackTrace(); }
	}
	
	public OfflinePlayer getLegendHolder(String name){
		try{
			ResultSet result = conn.prepareStatement(get_legend_holder.replace(name_macro, name)).executeQuery();
			if(result.first() && result.getString(holder_column) != null){
				OfflinePlayer player = Bukkit.getOfflinePlayer(result.getString(holder_column));
				if(player.hasPlayedBefore())
					return player;
				else
					return null;
			}
			else
				return null;
		}
		catch(SQLException sqle) {
			sqle.printStackTrace();
			return null;
		}
	}
	
	public void setLegendSpawn(String name, Location loc){
		try{
			String locStr = loc.getWorld().getName() + "@" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
			conn.prepareStatement(set_legend_spawn.replace(name_macro, name).replace(value_macro, locStr)).executeUpdate();
		}
		catch(SQLException sqle) { sqle.printStackTrace(); }
	}
	
	public Location getLegendSpawn(String name){
		try{
			ResultSet result = conn.prepareStatement(get_legend_spawn.replace(name_macro, name)).executeQuery();
			if(result.first()){
				String locStr = result.getString(spawn_loc_column);
				if(locStr == null || locStr.isEmpty())
					return null;
				String[] coords = locStr.substring(locStr.indexOf('@') + 1).split(",");
				return new Location(Bukkit.getWorld(locStr.substring(0, locStr.indexOf('@'))), Integer.parseInt(coords[0]), Integer.parseInt(coords[1]), Integer.parseInt(coords[2]));
			}
			else
				return null;
		}
		catch(SQLException sqle) {
			sqle.printStackTrace();
			return null;
		}
	}
	
	public List<String> getLegends(){
		try{
			ResultSet result = conn.prepareStatement(get_legends).executeQuery();
			List<String> names = new ArrayList<String>();
			while(result.next())
				names.add(result.getString(name_column));
			return names;
		}
		catch(SQLException sqle) {
			sqle.printStackTrace();
			return new ArrayList<String>();
		}
	}
}
