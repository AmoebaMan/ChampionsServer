package net.amoebaman.ffamaster;

import java.util.*;

import net.amoebaman.ffamaster.utils.Utils;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;

import com.google.common.collect.Lists;

public class ShardHandler implements Listener, Runnable{
	
	public static ItemStack getShard(int number){
		ItemStack item = new ItemStack(Material.QUARTZ);
		
		ItemMeta meta = Bukkit.getItemFactory().getItemMeta(Material.QUARTZ);
		meta.setDisplayName(ChatColor.RESET + "Wellspring Shard #" + (number + 1));
		meta.setLore(Lists.newArrayList(ChatColor.YELLOW + "Unite all shards to form the Wellspring Charm"));
		item.setItemMeta(meta);
		
		return item;
	}
	
	public static void spawnShard(int number){
		//We're just making sure there are enough empty slots in our list of shard items
		for(int i = FFAMaster.shardItems.size(); i <= number; i++)
			FFAMaster.shardItems.add(null);
		
		//Remove the old item if necessary
		if(FFAMaster.shardItems.get(number) != null)
			FFAMaster.shardItems.get(number).remove();
		
		Location spawn = FFAMaster.sql.getShardSpawn(number).getBlock().getLocation().add(0.5, 0.5, 0.5);
		Item item = spawn.getWorld().dropItem(spawn, getShard(number));
		item.setVelocity(new org.bukkit.util.Vector(0,0,0));
		item.getWorld().playEffect(spawn, Effect.MOBSPAWNER_FLAMES, 0);
		
		FFAMaster.shardItems.set(number, item);
		FFAMaster.sql.setShardHolder(number, null);
	}
	
	public static boolean isShard(ItemStack stack){
		if(stack == null)
			return false;
		return stack.getType() == Material.QUARTZ && stack.hasItemMeta() && stack.getItemMeta().hasDisplayName() && stack.getItemMeta().getDisplayName().contains("Wellspring Shard");
	}
	
	public static ItemStack getCharm(){
		return CustomItems.get("wellspring_charm");
	}
	
	public static boolean isCharm(ItemStack stack){
		if(stack == null)
			return false;
		return FFAMaster.sameItem(stack, getCharm());
	}
	
	public static int getShardNumber(ItemStack stack){
		String name = TradeHandler.getName(stack);
		name = name.replace("Wellspring Shard #", "");
		return Integer.parseInt(name) - 1;
	}
	
	public static OfflinePlayer getCharmHolder(){
		if(FFAMaster.sql.getNumShards() == 0)
			return null;
		OfflinePlayer holder = FFAMaster.sql.getShardHolder(0);
		if(holder != null)
			for(int i = 1; i < FFAMaster.sql.getNumShards(); i++)
				if(!holder.equals(FFAMaster.sql.getShardHolder(i)))
					return null;
		return holder;
	}
	
	public static Map<PotionEffectType, Integer> getCharmBonuses(){
		Map<PotionEffectType, Integer> effects = new HashMap<PotionEffectType, Integer>();
		effects.put(PotionEffectType.DAMAGE_RESISTANCE, 1);
		effects.put(PotionEffectType.INCREASE_DAMAGE, 1);
		effects.put(PotionEffectType.JUMP, 1);
		effects.put(PotionEffectType.REGENERATION, 1);
		effects.put(PotionEffectType.SATURATION, 1);
		effects.put(PotionEffectType.SPEED, 1);
		effects.put(PotionEffectType.FIRE_RESISTANCE, 0);
		effects.put(PotionEffectType.WATER_BREATHING, 0);
		effects.put(PotionEffectType.NIGHT_VISION, 0);
		return effects;
	}
	
	@EventHandler
	public void droppingShards(PlayerDropItemEvent event){
		Item item = event.getItemDrop();
		if(isShard(item.getItemStack())){
			FFAMaster.sql.setShardHolder(getShardNumber(item.getItemStack()), null);
			FFAMaster.shardItems.set(getShardNumber(item.getItemStack()), item);
		}
		if(isCharm(item.getItemStack()))
			for(int i = 0; i < FFAMaster.sql.getNumShards(); i++){
				FFAMaster.sql.setShardHolder(i, Bukkit.getOfflinePlayer("~CHARM~"));
				FFAMaster.shardItems.set(i, item);
			}
	}
	
	@EventHandler
	public void noStoringShards(InventoryClickEvent event){
		if(!(event.getInventory().getType() == InventoryType.CRAFTING || event.getInventory().getType() == InventoryType.CREATIVE || event.getInventory().getType() == InventoryType.PLAYER) && (isShard(event.getCurrentItem()) || isShard(event.getCursor()) || isCharm(event.getCurrentItem()) || isCharm(event.getCursor())))
			event.setCancelled(true);
	}
	
	@EventHandler
	public void recordShardPickups(PlayerPickupItemEvent event){
		if(isShard(event.getItem().getItemStack()))
			FFAMaster.sql.setShardHolder(getShardNumber(event.getItem().getItemStack()), event.getPlayer());
		if(isCharm(event.getItem().getItemStack()))
			for(int i = 0; i < FFAMaster.sql.getNumShards(); i++)
				FFAMaster.sql.setShardHolder(i, event.getPlayer());
	}
	
	@EventHandler
	public void respawnShards(ItemDespawnEvent event){
		if(isShard(event.getEntity().getItemStack()))
			spawnShard(getShardNumber(event.getEntity().getItemStack()));
		if(isCharm(event.getEntity().getItemStack()))
			for(int i = 0; i < FFAMaster.sql.getNumShards(); i++)
				spawnShard(i);
	}
	
	@EventHandler
	public void mobsDontDropShards(EntityDeathEvent event){
		if(event.getEntityType() != EntityType.PLAYER)
			for(int i = 0; i < FFAMaster.sql.getNumShards(); i++)
				event.getDrops().remove(getShard(i));
		event.getDrops().remove(getCharm());
	}
	
	@EventHandler
	public void shardsDropOnDeath(PlayerDeathEvent event){
		Player victim = event.getEntity();
		Inventory inv = victim.getInventory();
		for(int i = 0; i < inv.getSize(); i++)
			if(inv.getItem(i) != null){
				if(isShard(inv.getItem(i))){
					int num = getShardNumber(inv.getItem(i));
					inv.setItem(i, null);
					FFAMaster.sql.setShardHolder(num, null);
					FFAMaster.shardItems.set(num, victim.getWorld().dropItemNaturally(victim.getLocation(), getShard(num)));
				}
				if(isCharm(inv.getItem(i))){
					inv.setItem(i, null);
					Item drop = victim.getWorld().dropItemNaturally(victim.getLocation(), getCharm());
					for(int j = 0; j < FFAMaster.sql.getNumShards(); j++){
						FFAMaster.sql.setShardHolder(j, Bukkit.getOfflinePlayer("~CHARM~"));
						FFAMaster.shardItems.set(j, drop);
					}
				}
			}
	}
	
	@EventHandler
	public void removeUncataloguedShardDrops(ChunkLoadEvent event){
		//		for(int i = 0; i < FFAMaster.sql.getNumShards(); i++)
		//			if(FFAMaster.sql.getShardSpawn(i).getChunk().equals(event.getChunk()))
		//				FFAMaster.spawnShard(i);
		for(Entity e : event.getChunk().getEntities())
			if(e instanceof Item){
				ItemStack stack = ((Item) e).getItemStack();
				if((isShard(stack) || isCharm(stack)) && !FFAMaster.shardItems.contains((Item) e))
					e.remove();
			}
	}
	
	public static final int shardOfflineTimeoutHours = 36;
	public static final int charmOfflineTimeoutHours = 120;
	
	public void run() {
		
		OfflinePlayer holder = getCharmHolder();
		
		if(holder == null)
			
			for(int i = 0; i < FFAMaster.sql.getNumShards(); i++){
				
				holder = FFAMaster.sql.getShardHolder(i);
				
				if(holder == null){
					if(Utils.shouldRespawnItem(FFAMaster.shardItems.get(i)))
						spawnShard(i);
				}
				else if( ! Utils.checkAndKillExtras(holder, getShard(i), Utils.timeSinceLastPlay(holder) > Utils.hoursToMillis(shardOfflineTimeoutHours) && !holder.isOnline()) )
					spawnShard(i);
			}
		
		else{
			if( ! Utils.checkAndKillExtras(holder, CustomItems.get("wellspring_charm"), Utils.timeSinceLastPlay(holder) > Utils.hoursToMillis(charmOfflineTimeoutHours) && !holder.isOnline()) ){
				
				boolean missingShard = false;
				
				//If they don't actually have the charm, we see if they're holding every shard instead
				for(int i = 0; i < FFAMaster.sql.getNumShards(); i++){
					
					//Check to see if they're missing a shard, respawn it if they are (there's been a glitch)
					if( ! Utils.checkAndKillExtras(holder, getShard(i), false) ){
						missingShard = true;
						spawnShard(i);
					}
					
				}
				
				//If they have no charm, but every shard, remove all the shards and give them the charm
				if(!missingShard){
					for(int i = 0; i < FFAMaster.sql.getNumShards(); i++)
						Utils.checkAndKillExtras(holder, getShard(i), true);
					(holder.isOnline() ? holder.getPlayer().getInventory() : FFAMaster.sql.getInventory(holder)).addItem(CustomItems.get("wellspring_charm"));
				}
			}
			
		}
	}
	
}
