package net.amoebaman.championsserver.prompts;

import net.amoebaman.championsserver.TradeHandler;
import net.amoebaman.championsserver.objects.Trade;

import org.bukkit.ChatColor;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class BuyPrompt extends StringPrompt{
	
	public Player getPlayer(ConversationContext context){
		return (Player) context.getSessionData("player");
	}
	
	public Trade getTrade(ConversationContext context){
		return (Trade) context.getSessionData("trade");
	}
	
	public String getPromptText(ConversationContext context) {
		if(getTrade(context).item != null)
			return "Anything else you were wantin' bro?";
		else
			return "I got all kinds of good stuff for you bro.  Whatcha need?";
	}
	
	public Prompt acceptInput(ConversationContext context, String input) {
		ItemStack item = TradeHandler.getItem(input);

		Player player = getPlayer(context);
		Trade trade = getTrade(context);
		trade.item = item;
		context.setSessionData("trade", trade);
		
		if(item == null){
			player.sendRawMessage(TradeHandler.PREFIX + "Dang man, you tripped up on something strong.  I ain't never heard of nothin' like that before.");
			return new BuyPrompt();
		}
		
		int price = TradeHandler.getBuyPrice(item);
		trade.eachAt = price;
		context.setSessionData("trade", trade);
		
		if(price < 0){
			player.sendRawMessage(TradeHandler.PREFIX + "Sorry bro, I don't have any " + ChatColor.GREEN + TradeHandler.getName(item) + "s" + ChatColor.RESET + " for sale.");
			return new BuyPrompt();
		}
		
		player.sendRawMessage(TradeHandler.PREFIX + "Ehh, nice choice man.  I can get you " + ChatColor.GREEN + TradeHandler.getName(item) + "s" + ChatColor.RESET + " for " + ChatColor.GREEN + price + "LVL" + ChatColor.RESET + " each!");
		if(trade.item.getType().getMaxStackSize() == 1){
			trade.amount = 1;
			trade.total = trade.amount * trade.eachAt;
			context.setSessionData("trade", trade);
			return new ConfirmPrompt();
		}
		else
			return new HowManyPrompt();
	}
	
}