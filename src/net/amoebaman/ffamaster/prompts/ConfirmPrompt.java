package net.amoebaman.ffamaster.prompts;

import net.amoebaman.ffamaster.FFAMaster;
import net.amoebaman.ffamaster.TradeHandler;
import net.amoebaman.ffamaster.objects.Trade;
import net.amoebaman.ffamaster.objects.Trade.TradeType;

import org.bukkit.ChatColor;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.FixedSetPrompt;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ConfirmPrompt extends FixedSetPrompt{
	
	public ConfirmPrompt(){ super("yes", "no"); }
	
	public Player getPlayer(ConversationContext context){
		return (Player) context.getSessionData("player");
	}
	
	public Trade getTrade(ConversationContext context){
		return (Trade) context.getSessionData("trade");
	}
	
	
	public String getPromptText(ConversationContext context) {
		Trade trade = getTrade(context);
		
		return "So you're gonna " + (trade.type == TradeType.BUYING ? "buy" : "sell") + " " + trade.amount + " " + ChatColor.GREEN + TradeHandler.getName(trade.item) + (trade.amount > 1 ? "s" : "") + ChatColor.RESET + "?" +
				"  That's gonna " + (trade.type == TradeType.BUYING ? "run you" : "net you") + " " + trade.total + "LVL total.  " + ChatColor.GREEN + "Yes" + ChatColor.RESET + " or " + ChatColor.GREEN + "no" + ChatColor.RESET +
				", are you gonna " + (trade.type == TradeType.BUYING ? "buy" : "sell") + "?";
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
	
	protected Prompt acceptValidatedInput(ConversationContext context, String input) {
		Player player = getPlayer(context);
		Trade trade = getTrade(context);
		
		if(input.equals("no"))
			context.getForWhom().sendRawMessage(TradeHandler.PREFIX + "Sorry you changed your mind, homie.");
		if(input.equals("yes")){
			if(trade.type == TradeType.BUYING){
				if(player.getLevel() < trade.total){
					player.sendRawMessage(TradeHandler.PREFIX + "Hey I'm sorry bro, but you ain't got the dough for that!");
					player.sendRawMessage(TradeHandler.PREFIX + "Come back when you've gotten off your lazy bum and gotten some real cash!");
				}
				else{
					trade.item.setAmount(trade.amount);
					player.getInventory().addItem(trade.item);
					player.setLevel(player.getLevel() - trade.total);
					player.sendRawMessage(TradeHandler.PREFIX + "Have fun with your new toys, bro!");
				}
			}
			else{
				for(int i = 0; i < player.getInventory().getSize(); i++){
					ItemStack stack = player.getInventory().getItem(i);
					if(FFAMaster.sameItem(stack, trade.item)){
						if(stack.getAmount() <= trade.amount){
							trade.amount -= stack.getAmount();
							player.getInventory().setItem(i, null);
						}
						else{
							stack.setAmount(stack.getAmount() - trade.amount);
							break;
						}
						if(trade.amount == 0)
							break;
					}
				}
				player.setLevel(player.getLevel() + trade.total);
				player.sendRawMessage(TradeHandler.PREFIX + "There's your cash, thanks a bunch homie!");
			}
		}
		return Prompt.END_OF_CONVERSATION;
	}
	
}
