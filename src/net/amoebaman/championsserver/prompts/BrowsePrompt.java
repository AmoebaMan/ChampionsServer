package net.amoebaman.championsserver.prompts;

import net.amoebaman.championsserver.ChampionsServer;
import net.amoebaman.championsserver.TradeHandler;

import org.bukkit.ChatColor;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.MessagePrompt;
import org.bukkit.conversations.Prompt;
import org.bukkit.inventory.ItemStack;

public class BrowsePrompt extends MessagePrompt {
	
	private String list = "";
	public BrowsePrompt(){
		try{
			boolean first = true;
			for(ItemStack ware : ChampionsServer.buyPrices.keySet())
				if(ware != null && !(ChampionsServer.isWeaponOrArmor(ware.getType()) || TradeHandler.getName(ware).toLowerCase().contains("talisman"))){
					if(!first)
						list += ", ";
					list += ChatColor.GREEN + TradeHandler.getName(ware) + ChatColor.RESET;
					first = false;
				}
		}
		catch(Exception e){ e.printStackTrace(); }
	}
	
	public String getPromptText(ConversationContext context) {
		return "In addition to any type of sword, axe, or armor, or any status effect talisman, I can sell you any of these items: " + list;
	}
	
	public Prompt getNextPrompt(ConversationContext context) { return Prompt.END_OF_CONVERSATION; }
	
}
