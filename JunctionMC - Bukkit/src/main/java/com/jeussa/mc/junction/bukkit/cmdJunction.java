package com.jeussa.mc.junction.bukkit;

import com.jeussa.mc.junction.network.JunctionNetworkServer;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class cmdJunction implements CommandExecutor, TabCompleter{

    private final @NotNull JMC plugin;


    public cmdJunction(@NotNull JMC plugin){
        this.plugin = plugin;

        var handle = plugin.getCommand("junction");
        if(handle == null)throw new NullPointerException();
        handle.setExecutor(this);
        handle.setTabCompleter(this);
    }


    /**
     * onTabComplete
     */
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String @NotNull [] args){
        if(!sender.isOp())return null;
        else if(args.length == 1){
            var start = args[0].toLowerCase();
            return Stream.of("status", "network").filter(s -> s.toLowerCase().startsWith(start)).toList();
        }
        else if(args.length == 2){
            if(args[0].equalsIgnoreCase("network")){
                var start = args[1].toLowerCase();
                return Stream.of("status", "info", "overview").filter(s -> s.toLowerCase().startsWith(start)).toList();
            }
        }
        return null;
    }



    /**
     * onCommand
     */
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String @NotNull [] args){
        if(!sender.isOp()){
            sender.sendMessage(ChatColor.RED + "You do you not have the permission to use this command!");
            return true;
        }

        else if(args.length == 0)return false;

        /*
         * Status
         */
        else if(args[0].equalsIgnoreCase("status")){
            sender.sendMessage(
                ChatColor.DARK_AQUA + "JunctionMC status: " +
                ChatColor.GOLD + JMC.api().getNumBridges() + ChatColor.DARK_AQUA + " active connections, " +
                ChatColor.GOLD + JMC.api().getNumPendingServers() + ChatColor.DARK_AQUA + " pending servers and " +
                ChatColor.GOLD + JMC.api().getNumPendingClients() + ChatColor.DARK_AQUA + " pending clients."
            );
            return true;
        }

        /*
         * Network
         */
        else if(args[0].equalsIgnoreCase("network")){
            if(args.length == 1)return false;

            // = Status =
            else if(args[1].equalsIgnoreCase("status")){

            }

            // = Info =
            else if(args[1].equalsIgnoreCase("info")){

            }

            // = Overview =
            else if(args[1].equalsIgnoreCase("overview")){
                sender.sendMessage(ChatColor.WHITE + "============================================");
                for(var s : this.appendServerStatus(JMC.api().getNetworkManager().getMainServer(), new ArrayList<>(), 0))sender.sendMessage(s);
                sender.sendMessage(ChatColor.WHITE + "============================================");
                return true;
            }
        }

        return false;
    }


    /**
     * Utilities
     */
    private List<String> appendServerStatus(JunctionNetworkServer current, List<String> list, int indent){
        var lp = current.getLastPing();
        list.add(" ".repeat(indent) + ChatColor.GOLD + current.getName() + ChatColor.YELLOW + ((lp == -2L) ? " self" : (" last pinged " + this.formatDuration(System.currentTimeMillis() - current.getLastPing()) + " ago")));
        for(var c : current.getChildren())this.appendServerStatus(c, list, indent + 1);
        return list;
    }
    private String formatDuration(long millis){
        long days = millis / (24 * 60 * 60 * 1000);
        millis %= 24 * 60 * 60 * 1000;

        long hours = millis / (60 * 60 * 1000);
        millis %= 60 * 60 * 1000;

        long minutes = millis / (60 * 1000);
        millis %= 60 * 1000;

        double seconds = millis / 1000.0;

        StringBuilder sb = new StringBuilder();
        if(days > 0)sb.append(days).append(" day").append(days == 1 ? "" : "s").append(" ");
        if(hours > 0 || !sb.isEmpty())sb.append(hours).append(" hour").append(hours == 1 ? "" : "s").append(" ");
        if(minutes > 0 || !sb.isEmpty())sb.append(minutes).append(" minute").append(minutes == 1 ? "" : "s").append(" ");
        sb.append(String.format("%.3f", seconds)).append(" second").append(seconds == 1D ? "" : "s");

        return sb.toString();
    }
}
