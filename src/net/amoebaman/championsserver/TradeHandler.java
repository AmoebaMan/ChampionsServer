package net.amoebaman.championsserver;

import java.util.*;

import net.amoebaman.championsserver.objects.Trade;
import net.amoebaman.championsserver.prompts.WelcomePrompt;
import net.milkbowl.vault.item.ItemInfo;
import net.milkbowl.vault.item.Items;

import org.bukkit.*;
import org.bukkit.conversations.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class TradeHandler implements Listener, ConversationAbandonedListener{
	
	public static final String TRADER_NAME = "Dave";
	public static final String PREFIX = "<" + ChatColor.LIGHT_PURPLE + "Merchant" + ChatColor.RESET + "> " + ChatColor.GRAY + TRADER_NAME + ": " + ChatColor.RESET;
	private ConversationFactory factory = new ConversationFactory(ChampionsServer.plugin())
	.addConversationAbandonedListener(this)
	.thatExcludesNonPlayersWithMessage("Must be a player")
	.withEscapeSequence("SHUT UP")
	.withLocalEcho(false)
	.withModality(false)
	.withPrefix(new StringPrefix(PREFIX))
	.withTimeout(10)
	.withFirstPrompt(new WelcomePrompt());
	
	
	@EventHandler
	public void tradeInitiation(PlayerInteractEntityEvent event){
		if(event.getRightClicked() instanceof LivingEntity && TRADER_NAME.equals(((LivingEntity) event.getRightClicked()).getCustomName())){
			Map<Object, Object> session = new HashMap<Object, Object>();
			session.put("player", event.getPlayer());
			session.put("trade", new Trade());
			event.getPlayer().beginConversation(factory.withInitialSessionData(session).buildConversation(event.getPlayer()));
		}
	}
	
	public void conversationAbandoned(ConversationAbandonedEvent event) {
		if(event.getCanceller() instanceof InactivityConversationCanceller){
			event.getContext().getForWhom().sendRawMessage(TradeHandler.PREFIX + "You're spacin' out homie, come back when you're awake!");
		}
		if(event.getCanceller() instanceof ExactMatchConversationCanceller){
			event.getContext().getForWhom().sendRawMessage(TradeHandler.PREFIX + "No need to be rude 'bout it, bro!");
		}
	}
	
	public static ItemStack getItem(String name){
		
		if(CustomItems.isCustomItem(name))
			return CustomItems.get(name);
		
		ItemInfo item = Items.itemByName(name);
		if(item != null)
			return item.toStack();
		
		Material mat = Material.matchMaterial(name);
		if(mat != null)
			return new ItemStack(mat);
		
		return null;
	}
	
	public static String getName(ItemStack item){
		if(item.hasItemMeta() && item.getItemMeta().hasDisplayName())
			return ChatColor.stripColor(item.getItemMeta().getDisplayName());
		ItemInfo info = Items.itemByStack(item);
		if(info != null)
			return info.getName();
		return item.getType().name().toLowerCase().replace('_', ' ');
	}
	
	public static int getAmountInInventory(ItemStack item, Inventory inv){
		int count = 0;
		for(ItemStack stack : inv.getContents())
			if(ChampionsServer.sameItem(stack, item))
				count += stack.getAmount();
		return count;
	}
	
	public static int getBuyPrice(ItemStack item){
		for(ItemStack each : ChampionsServer.buyPrices.keySet())
			if(ChampionsServer.sameItem(item, each))
				return ChampionsServer.buyPrices.get(each);
		return -1;
	}
	
	private class StringPrefix implements ConversationPrefix{
		private String prefix;
		public StringPrefix(String prefix){
			this.prefix = prefix;
		}
		public String getPrefix(ConversationContext context) { return prefix; }
		
	}
	
}
