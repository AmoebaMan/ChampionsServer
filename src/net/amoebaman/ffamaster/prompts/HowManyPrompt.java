package net.amoebaman.ffamaster.prompts;

import net.amoebaman.ffamaster.TradeMaster;
import net.amoebaman.ffamaster.objects.Trade;
import net.amoebaman.ffamaster.objects.Trade.TradeType;

import org.bukkit.ChatColor;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.NumericPrompt;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;

public class HowManyPrompt extends NumericPrompt{
	
	public Player getPlayer(ConversationContext context){
		return (Player) context.getSessionData("player");
	}
	
	public Trade getTrade(ConversationContext context){
		return (Trade) context.getSessionData("trade");
	}

    public String getPromptText(ConversationContext context) {
    	return "How many of those did you want to " + (getTrade(context).type == TradeType.BUYING ? "buy from me?" : "sell to me?");
    }
	
	public String getFailedValidationText(ConversationContext context, String input){
		return ChatColor.RESET + TradeMaster.PREFIX + "C'mon guy, I can only handle so much at once.  No more than " + ChatColor.GREEN + getTrade(context).item.getType().getMaxStackSize() + ChatColor.RESET + " at once.";
	}
	
	public boolean isNumberValid(ConversationContext context, Number num){
		return num.intValue() <= getTrade(context).item.getType().getMaxStackSize() || getTrade(context).type == TradeType.SELLING;
	}

    protected Prompt acceptValidatedInput(ConversationContext context, Number num) {
		Player player = getPlayer(context);
		Trade trade = getTrade(context);
		trade.amount = num.intValue();
		trade.total = trade.amount * trade.eachAt;
		context.setSessionData("trade", trade);
		
    	if(trade.type == TradeType.SELLING && TradeMaster.getAmountInInventory(trade.item, player.getInventory()) < trade.amount){
			player.sendRawMessage(TradeMaster.PREFIX + "Hey, c'mon homie, don't try to scam a bro.  I know you ain't got that much stuff with you.");
			return new HowManyPrompt();
    	}
    	
    	return new ConfirmPrompt();
    }
	
}