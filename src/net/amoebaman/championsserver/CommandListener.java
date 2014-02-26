package net.amoebaman.championsserver;

import net.amoebaman.utils.ChatUtils;
import net.amoebaman.utils.CommandController.CommandHandler;
import net.minecraft.util.com.google.common.collect.Lists;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CommandListener {

    @CommandHandler(cmd = "check-pt")
    public void checkPtCommand(Player player, String[] args){
        player.sendMessage(ChatColor.ITALIC + "That " + TradeHandler.getName(player.getItemInHand()) + " is worth " + ChampionsServer.getPtValue(player.getItemInHand()) + "PT");
        player.sendMessage(ChatColor.ITALIC + "Your inventory is worth " + ChampionsServer.getPtValue(player.getInventory()) + "PT");
    }

    @CommandHandler(cmd = "view-storage-chest")
    public void viewStorageChestCommand(Player player, String[] args){
        Player target = player;
        if(args.length > 0)
            target = Bukkit.getPlayer(args[0]);
        if(target == null)
            player.sendMessage("Unable to find player \"" + args[0] + "\"");
        else{
            player.sendMessage("Opening " + target.getName() + "'s storage");
            player.openInventory(ChampionsServer.sql.loadChest(target));
        }
    }

    @CommandHandler(cmd = "get-custom-item")
    public void getCustomItemCommand(Player player, String[] args){
        if(args.length >= 1 && CustomItems.isCustomItem(args[0]))
            if(!LegendaryHandler.isLegend(args[0])){
                ItemStack item = CustomItems.get(args[0]);
                if(args.length >= 2)
                    item.setAmount(Integer.parseInt(args[1]));
                player.getInventory().addItem(item);
            }
            else
                player.sendMessage(ChatColor.RED + "You can't hack in legendary items");
        else
            player.sendMessage(ChatColor.RED + "Invalid custom item specified");
    }

    @CommandHandler(cmd = "shards")
    public void shardsCommand(CommandSender sender, String[] args){
        OfflinePlayer holder = ShardHandler.getCharmHolder();
        if(holder == null || holder.getName().equals("~CHARM~"))
            for(int i = 0; i < ChampionsServer.sql.getNumShards(); i++){
                holder = ChampionsServer.sql.getShardHolder(i);
                if(sender.equals(holder))
                    sender.sendMessage(ChatColor.GREEN + "You hold Wellspring Shard #" + (i+1));
                else if(holder == null)
                    sender.sendMessage(ChatColor.YELLOW + "Nobody holds Wellspring Shard #" + (i+1));
                else{
                    int offlineHours = (int) ((System.currentTimeMillis() - holder.getLastPlayed()) / 1000.0 / 60.0 / 60.0);
                    sender.sendMessage(ChatColor.RED + holder.getName() + " holds Wellspring Shard #" + (i+1) + (holder.isOnline() ? "" : " " + ChatUtils.makeProgressBar(40, ShardHandler.shardOfflineTimeoutHours, Lists.newArrayList(ChatColor.DARK_RED, ChatColor.DARK_GREEN), Lists.newArrayList(offlineHours))));
                }
            }
        else
            if(sender.equals(holder))
                sender.sendMessage(ChatColor.GREEN + "You hold the Wellspring Charm");
            else if(holder.getName().equals("~CHARM~"))
                sender.sendMessage(ChatColor.YELLOW + "Nobody holds the Wellspring Charm");
            else{
                int offlineHours = (int) ((System.currentTimeMillis() - holder.getLastPlayed()) / 1000.0 / 60.0 / 60.0);
                sender.sendMessage(ChatColor.RED + holder.getName() + " holds the Wellspring Charm" + (holder.isOnline() ? "" : " " + ChatUtils.makeProgressBar(40, ShardHandler.shardOfflineTimeoutHours, Lists.newArrayList(ChatColor.DARK_RED, ChatColor.DARK_GREEN), Lists.newArrayList(offlineHours))));
            }
    }

    @CommandHandler(cmd = "shard-spawn")
    public void shardSpawnCommand(Player player, String[] args){
        int num = Integer.parseInt(args[0]) - 1;
        ChampionsServer.sql.setShardSpawn(num, player.getLocation());
        if(ChampionsServer.sql.getShardHolder(num) == null)
            ShardHandler.spawnShard(num);
        player.sendMessage("Spawn location for shard #" + args[0] + " set to current location");
    }

    @CommandHandler(cmd = "legends")
    public void legendsCommand(CommandSender sender, String[] args){
        for(ItemStack legend : LegendaryHandler.getLegends()){
            OfflinePlayer holder = ChampionsServer.sql.getLegendHolder(CustomItems.getName(legend));
            if(sender.equals(holder))
                sender.sendMessage(ChatColor.GREEN + "You hold " + TradeHandler.getName(legend));
            else if(holder == null)
                sender.sendMessage(ChatColor.YELLOW + "Nobody holds " + TradeHandler.getName(legend));
            else{
                int offlineHours = (int) ((System.currentTimeMillis() - holder.getLastPlayed()) / 1000.0 / 60.0 / 60.0);
                sender.sendMessage(ChatColor.RED + holder.getName() + " holds " + TradeHandler.getName(legend) + (holder.isOnline() ? "" : " " + ChatUtils.makeProgressBar(40, LegendaryHandler.legendOfflineTimeoutHours, Lists.newArrayList(ChatColor.DARK_RED, ChatColor.DARK_GREEN), Lists.newArrayList(offlineHours))));
            }
        }
    }

    @CommandHandler(cmd = "legend-spawn")
    public void legendSpawnCommand(Player player, String[] args){
        ItemStack legend = CustomItems.get(args[0]);
        String name = CustomItems.getName(legend);
        ChampionsServer.sql.setLegendSpawn(name, player.getLocation());
        if(ChampionsServer.sql.getLegendHolder(name) == null)
            LegendaryHandler.spawnLegend(name);
        player.sendMessage("Spawn location for " + TradeHandler.getName(legend) + " set to current location");
    }

}
