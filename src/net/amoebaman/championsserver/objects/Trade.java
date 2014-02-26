package net.amoebaman.championsserver.objects;

import org.bukkit.inventory.ItemStack;

public class Trade {
	
	public TradeType type;
	public ItemStack item;
	public int amount;
	public int eachAt;
	public int total;
	
	public enum TradeType{ BUYING, SELLING }
	
}
