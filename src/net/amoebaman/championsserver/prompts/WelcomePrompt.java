package net.amoebaman.championsserver.prompts;

import java.util.Map;

import net.amoebaman.championsserver.ChampionsServer;
import net.amoebaman.championsserver.utils.Utils;

import org.bukkit.ChatColor;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.MessagePrompt;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;

public class WelcomePrompt extends MessagePrompt{
	
	public Player getPlayer(ConversationContext context){
		return (Player) context.getSessionData("player");
	}
	
	public String getPromptText(ConversationContext context) {
		Player player = getPlayer(context);
		Map<String, String> data = Utils.jsonLoad(ChampionsServer.sql.loadDataString(player));
		if(data.get("doneconvotutorial") == null || data.get("doneconvotutorial").equals("false")){
			player.sendRawMessage(ChatColor.RED + "Chat with the NPC in chat, just like normal");
			player.sendRawMessage(ChatColor.GOLD + "Other players can't hear what you say to him");
			player.sendRawMessage(ChatColor.YELLOW + "You can end the conversation by typing " + ChatColor.RED + "SHUT UP" + ChatColor.YELLOW + " in chat");
			data.put("doneconvotutorial", "true");
			ChampionsServer.sql.saveDataString(player, Utils.jsonSave(data));
		}
		return "Whassup dawg?";
	}
	
	protected Prompt getNextPrompt(ConversationContext context) { return new ActionPrompt(); }
	
}
