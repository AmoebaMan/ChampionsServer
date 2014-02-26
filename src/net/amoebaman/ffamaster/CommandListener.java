package net.amoebaman.ffamaster;

import net.amoebaman.ffamaster.utils.ChatUtils;
import net.amoebaman.ffamaster.utils.CommandController.CommandHandler;
import net.minecraft.util.com.google.common.collect.Lists;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CommandListener {

    @CommandHandler(name = "check-pt")
    public void checkPtCommand(Player player, String[] args){
        player.sendMessage(ChatColor.ITALIC + "That " + TradeMaster.getName(player.getItemInHand()) + " is worth " + FFAMaster.getPtValue(player.getItemInHand()) + "PT");
        player.sendMessage(ChatColor.ITALIC + "Your inventory is worth " + FFAMaster.getPtValue(player.getInventory()) + "PT");
    }

    @CommandHandler(name = "view-storage-chest")
    public void viewStorageChestCommand(Player player, String[] args){
        Player target = player;
        if(args.length > 0)
            target = Bukkit.getPlayer(args[0]);
        if(target == null)
            player.sendMessage("Unable to find player \"" + args[0] + "\"");
        else{
            player.sendMessage("Opening " + target.getName() + "'s storage");
            player.openInventory(FFAMaster.sql.loadChest(target));
        }
    }

    @CommandHandler(name = "get-custom-item")
    public void getCustomItemCommand(Player player, String[] args){
        if(args.length >= 1 && CustomItems.isCustomItem(args[0]))
            if(!LegendMaster.isLegend(args[0])){
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

    @CommandHandler(name = "shards")
    public void shardsCommand(CommandSender sender, String[] args){
        OfflinePlayer holder = ShardMaster.getCharmHolder();
        if(holder == null || holder.getName().equals("~CHARM~"))
            for(int i = 0; i < FFAMaster.sql.getNumShards(); i++){
                holder = FFAMaster.sql.getShardHolder(i);
                if(sender.equals(holder))
                    sender.sendMessage(ChatColor.GREEN + "You hold Wellspring Shard #" + (i+1));
                else if(holder == null)
                    sender.sendMessage(ChatColor.YELLOW + "Nobody holds Wellspring Shard #" + (i+1));
                else{
                    int offlineHours = (int) ((System.currentTimeMillis() - holder.getLastPlayed()) / 1000.0 / 60.0 / 60.0);
                    sender.sendMessage(ChatColor.RED + holder.getName() + " holds Wellspring Shard #" + (i+1) + (holder.isOnline() ? "" : " " + ChatUtils.makeProgressBar(40, ShardMaster.shardOfflineTimeoutHours, Lists.newArrayList(ChatColor.DARK_RED, ChatColor.DARK_GREEN), Lists.newArrayList(offlineHours))));
                }
            }
        else
            if(sender.equals(holder))
                sender.sendMessage(ChatColor.GREEN + "You hold the Wellspring Charm");
            else if(holder.getName().equals("~CHARM~"))
                sender.sendMessage(ChatColor.YELLOW + "Nobody holds the Wellspring Charm");
            else{
                int offlineHours = (int) ((System.currentTimeMillis() - holder.getLastPlayed()) / 1000.0 / 60.0 / 60.0);
                sender.sendMessage(ChatColor.RED + holder.getName() + " holds the Wellspring Charm" + (holder.isOnline() ? "" : " " + ChatUtils.makeProgressBar(40, ShardMaster.shardOfflineTimeoutHours, Lists.newArrayList(ChatColor.DARK_RED, ChatColor.DARK_GREEN), Lists.newArrayList(offlineHours))));
            }
    }

    @CommandHandler(name = "shard-spawn")
    public void shardSpawnCommand(Player player, String[] args){
        int num = Integer.parseInt(args[0]) - 1;
        FFAMaster.sql.setShardSpawn(num, player.getLocation());
        if(FFAMaster.sql.getShardHolder(num) == null)
            ShardMaster.spawnShard(num);
        player.sendMessage("Spawn location for shard #" + args[0] + " set to current location");
    }

    @CommandHandler(name = "legends")
    public void legendsCommand(CommandSender sender, String[] args){
        for(ItemStack legend : LegendMaster.getLegends()){
            OfflinePlayer holder = FFAMaster.sql.getLegendHolder(CustomItems.getName(legend));
            if(sender.equals(holder))
                sender.sendMessage(ChatColor.GREEN + "You hold " + TradeMaster.getName(legend));
            else if(holder == null)
                sender.sendMessage(ChatColor.YELLOW + "Nobody holds " + TradeMaster.getName(legend));
            else{
                int offlineHours = (int) ((System.currentTimeMillis() - holder.getLastPlayed()) / 1000.0 / 60.0 / 60.0);
                sender.sendMessage(ChatColor.RED + holder.getName() + " holds " + TradeMaster.getName(legend) + (holder.isOnline() ? "" : " " + ChatUtils.makeProgressBar(40, LegendMaster.legendOfflineTimeoutHours, Lists.newArrayList(ChatColor.DARK_RED, ChatColor.DARK_GREEN), Lists.newArrayList(offlineHours))));
            }
        }
    }

    @CommandHandler(name = "legend-spawn")
    public void legendSpawnCommand(Player player, String[] args){
        ItemStack legend = CustomItems.get(args[0]);
        String name = CustomItems.getName(legend);
        FFAMaster.sql.setLegendSpawn(name, player.getLocation());
        if(FFAMaster.sql.getLegendHolder(name) == null)
            LegendMaster.spawnLegend(name);
        player.sendMessage("Spawn location for " + TradeMaster.getName(legend) + " set to current location");
    }

}
