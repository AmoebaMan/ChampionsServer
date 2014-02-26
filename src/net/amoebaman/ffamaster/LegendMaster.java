package net.amoebaman.ffamaster;

import java.util.ArrayList;
import java.util.List;

import net.amoebaman.ffamaster.utils.Utils;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class LegendMaster implements Listener, Runnable{
	
	public static void spawnLegend(String name){
		
		ItemStack stack = CustomItems.get(name);
		String storageName = CustomItems.getName(stack);
		Location spawn = FFAMaster.sql.getLegendSpawn(storageName).getBlock().getLocation().add(0.5, 0.5, 0.5);
		
		//Remove the old item if necessary
		if(FFAMaster.legendItems.get(storageName) != null)
			FFAMaster.legendItems.get(storageName).remove();
		
		Item item = spawn.getWorld().dropItem(spawn, stack);
		item.setVelocity(new org.bukkit.util.Vector(0,0,0));
		item.getWorld().playEffect(spawn, Effect.MOBSPAWNER_FLAMES, 0);
		
		FFAMaster.legendItems.put(storageName, item);
		FFAMaster.sql.setLegendHolder(storageName, null);
	}
	
	public static boolean isLegend(String name){
		return CustomItems.isCustomItem(name) && FFAMaster.sql.getLegendSpawn(CustomItems.getName(CustomItems.get(name))) != null;
	}
	
	public static boolean isLegend(ItemStack stack){
		return CustomItems.isCustomItem(stack) && FFAMaster.sql.getLegendSpawn(CustomItems.getName(stack)) != null;
	}
	
	public static List<ItemStack> getLegends(){
		List<ItemStack> stacks = new ArrayList<ItemStack>();
		for(String name : FFAMaster.sql.getLegends())
			if(CustomItems.isCustomItem(name))
				stacks.add(CustomItems.get(name));
		return stacks;
	}
	
	@EventHandler
	public void droppingLegends(PlayerDropItemEvent event){
		Item item = event.getItemDrop();
		if(isLegend(item.getItemStack())){
			FFAMaster.sql.setLegendHolder(CustomItems.getName(item.getItemStack()), null);
			FFAMaster.legendItems.put(CustomItems.getName(item.getItemStack()), item);
		}
	}
	
	@EventHandler
	public void storingLgends(InventoryClickEvent event){
		if(!(event.getInventory().getType() == InventoryType.CRAFTING || event.getInventory().getType() == InventoryType.CREATIVE || event.getInventory().getType() == InventoryType.PLAYER || event.getInventory().getType() == InventoryType.ENDER_CHEST) && (isLegend(event.getCurrentItem()) || isLegend(event.getCursor())))
			event.setCancelled(true);
	}
	
	@EventHandler
	public void recordLegendPickups(PlayerPickupItemEvent event){
		if(isLegend(event.getItem().getItemStack()))
			FFAMaster.sql.setLegendHolder(CustomItems.getName(event.getItem().getItemStack()), event.getPlayer());
	}
	
	@EventHandler
	public void respawnLegends(ItemDespawnEvent event){
		if(isLegend(event.getEntity().getItemStack()))
			spawnLegend(CustomItems.getName(event.getEntity().getItemStack()));
	}
	
	@EventHandler
	public void mobsDontDropLegends(EntityDeathEvent event){
		if(event.getEntityType() != EntityType.PLAYER)
			for(ItemStack each : event.getDrops())
				if(isLegend(each)){
					each.setItemMeta(null);
					for(Enchantment enc : Enchantment.values())
						each.removeEnchantment(enc);
				}
	}
	
	@EventHandler
	public void legendsDropOnDeath(PlayerDeathEvent event){
		Inventory inv = event.getEntity().getInventory();
		for(int i = 0; i < inv.getSize(); i++){
			ItemStack stack = inv.getItem(i);
			if(stack != null && isLegend(stack)){
				inv.setItem(i, null);
				FFAMaster.sql.setLegendHolder(CustomItems.getName(stack), null);
				FFAMaster.legendItems.put(CustomItems.getName(stack), event.getEntity().getWorld().dropItemNaturally(event.getEntity().getLocation(), stack));
			}
		}
	}
	
	@EventHandler
	public void removeUncataloguedLegendDrops(ChunkLoadEvent event){
		//		for(int i = 0; i < FFAMaster.sql.getNumLegends(); i++)
		//			if(FFAMaster.sql.getLegendSpawn(i).getChunk().equals(event.getChunk()))
		//				FFAMaster.spawnLegend(i);
		for(Entity e : event.getChunk().getEntities())
			if(e instanceof Item){
				ItemStack stack = ((Item) e).getItemStack();
				if(isLegend(stack) && !FFAMaster.legendItems.containsValue((Item) e))
					e.remove();
			}
	}
	
	public static final int legendOfflineTimeoutHours = 120;
	
	public void run() {
		
		for(ItemStack legend : getLegends()){
			
			String name = CustomItems.getName(legend);
			OfflinePlayer holder = FFAMaster.sql.getLegendHolder(name);
			
			if(holder == null){
				if(Utils.shouldRespawnItem(FFAMaster.legendItems.get(name)))
					spawnLegend(name);
			}
			
			else if( ! Utils.checkAndKillExtras(holder, legend, Utils.timeSinceLastPlay(holder) > Utils.hoursToMillis(legendOfflineTimeoutHours) && !holder.isOnline()))
				spawnLegend(name);
			
		}
	}
	
}
