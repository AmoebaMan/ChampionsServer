package net.amoebaman.ffamaster.prompts;

import net.amoebaman.ffamaster.FFAMaster;
import net.amoebaman.ffamaster.TradeMaster;

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
			for(ItemStack ware : FFAMaster.buyPrices.keySet())
				if(ware != null && !(FFAMaster.isWeaponOrArmor(ware.getType()) || TradeMaster.getName(ware).toLowerCase().contains("talisman"))){
					if(!first)
						list += ", ";
					list += ChatColor.GREEN + TradeMaster.getName(ware) + ChatColor.RESET;
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
