package net.amoebaman.championsserver.prompts;

import net.amoebaman.championsserver.TradeHandler;
import net.amoebaman.championsserver.objects.Trade;
import net.amoebaman.championsserver.objects.Trade.TradeType;

import org.bukkit.ChatColor;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.FixedSetPrompt;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;

public class ActionPrompt extends FixedSetPrompt{
	
	public Player getPlayer(ConversationContext context){
		return (Player) context.getSessionData("player");
	}
	
	public Trade getTrade(ConversationContext context){
		return (Trade) context.getSessionData("trade");
	}
	
	public ActionPrompt(){ super("buy", "sell", "browse"); }
	
	public String getPromptText(ConversationContext context) {
		if(context.getSessionData("action") != null)
			return "Need anything else bro?  Are you here to " + ChatColor.GREEN + "buy" + ChatColor.RESET + ", " + ChatColor.GREEN + "sell" + ChatColor.RESET + ", or just " + ChatColor.GREEN + "browse" + ChatColor.RESET + "?";
		else
			return "What did you need today man, are you here to " + ChatColor.GREEN + "buy" + ChatColor.RESET + ", " + ChatColor.GREEN + "sell" + ChatColor.RESET + ", or just " + ChatColor.GREEN + "browse" + ChatColor.RESET + "?";
	}
	
	public boolean isInputValid(ConversationContext context, String input){
		boolean actionIncluded = false;
		for(String choice : fixedSet)
			if(input.toLowerCase().contains(choice.toLowerCase())){
				if(actionIncluded)
					return false;
				else
					actionIncluded = true;
			}
		return actionIncluded;
	}
	
	public Prompt acceptValidatedInput(ConversationContext context, String input) {
		Player player = getPlayer(context);
		Trade trade = getTrade(context);
		if(input.contains("buy"))
			trade.type = TradeType.BUYING;
		if(input.contains("sell"))
			trade.type = TradeType.SELLING;
		if(input.contains("brose"))
			trade.type = null;
		context.setSessionData("trade", trade);
		
		if(trade.type == TradeType.BUYING)
			if(getPlayer(context).getInventory().firstEmpty() == -1){
				player.sendRawMessage("<" + TradeHandler.TRADER_NAME + "> Yo, your pockets are full, man!  You ain't got no room left!");
				return Prompt.END_OF_CONVERSATION;
			}
			else
				return new BuyPrompt();
		else if(trade.type == TradeType.SELLING)
			return new SellPrompt();
		else if(trade.type == null)
			return new BrowsePrompt();
		else
			return Prompt.END_OF_CONVERSATION;
	}
	
}
