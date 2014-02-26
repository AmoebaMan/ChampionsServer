package net.amoebaman.ffamaster.tasks;

import java.util.*;
import java.util.Map.Entry;

import net.amoebaman.ffamaster.CustomItems;
import net.amoebaman.ffamaster.FFAMaster;
import net.amoebaman.ffamaster.ShardMaster;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class TalismanEffectTask implements Runnable, Listener {
	
	private static Map<PotionEffectType, Integer> MAP = new HashMap<PotionEffectType, Integer>();
	
	public void run(){
		
		for(Player player : Bukkit.getOnlinePlayers()){
			
			MAP = FFAMaster.getTalismanEffects(player.getInventory());
			
			if(player.getInventory().contains(CustomItems.get("wellspring_charm")))
				for(Entry<PotionEffectType, Integer> entry : ShardMaster.getCharmBonuses().entrySet()){
					if(!MAP.containsKey(entry.getKey()))
						MAP.put(entry.getKey(), entry.getValue());
					else
						MAP.put(entry.getKey(), MAP.get(entry.getKey()) + entry.getValue() + 1);
				}
			
			if(player.getGameMode() != GameMode.CREATIVE){
				ApplicableRegionSet regions = WGBukkit.getRegionManager(player.getWorld()).getApplicableRegions(player.getLocation());
				for(ProtectedRegion region : regions)
					if(region.getId().contains("maze")){
						MAP.put(PotionEffectType.BLINDNESS, 0);
						MAP.put(PotionEffectType.CONFUSION, 0);
					}
			}
			
			for(PotionEffectType type : MAP.keySet()){
				int maxLevel = 3;
				if(type.equals(PotionEffectType.NIGHT_VISION) ||
						type.equals(PotionEffectType.FIRE_RESISTANCE) ||
						type.equals(PotionEffectType.WATER_BREATHING) ||
						type.equals(PotionEffectType.INVISIBILITY) ||
						type.equals(PotionEffectType.CONFUSION))
					maxLevel = 0;
				if(MAP.get(type) > maxLevel)
					MAP.put(type, maxLevel);
			}
			
			if(FFAMaster.sameItem(player.getItemInHand(), CustomItems.get("binoculars")))
				MAP.put(PotionEffectType.SLOW, 99);
			
			if(!getEffects(player).equals(MAP)){
				for(PotionEffect active : player.getActivePotionEffects())
					player.removePotionEffect(active.getType());
				for(PotionEffectType effect : MAP.keySet())
					player.addPotionEffect(new PotionEffect(effect, Integer.MAX_VALUE, MAP.get(effect), true), true);
			}
			
		}
		
	}
	
	public Map<PotionEffectType, Integer> getEffects(Player player){
		Map<PotionEffectType, Integer> map  = new HashMap<PotionEffectType, Integer>();
		for(PotionEffect effect : player.getActivePotionEffects())
			map.put(effect.getType(), effect.getAmplifier());
		return map;
	}
	
}
