package com.jeussa.mc.junction.bukkit;

import com.jeussa.mc.junction.JunctionBridge;
import com.jeussa.mc.junction.core.network.JMCnetwork;
import com.jeussa.mc.junction.packet.Testpacket;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.NumberConversions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class cmdTest implements CommandExecutor, TabCompleter{

    private final JMC plugin;

    private JunctionBridge bridge = null;
    private boolean listener = false;

    public cmdTest(JMC plugin){
        this.plugin = plugin;

        var handle = plugin.getCommand("jtest");
        if(handle == null)throw new NullPointerException();

        handle.setExecutor(this);
        handle.setTabCompleter(this);
    }

    /**
     * onTabComplete
     */
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String @NotNull [] args){
        if(args.length == 1)return Arrays.asList("startclient", "startserver", "sayhi", "status", "close", "startnetwork", "testnetwork", "stopnetwork", "validation");
        return Collections.emptyList();
    }

    /**
     * onCommand
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String @NotNull [] args){
        if(args.length < 1)return false;

        // Start network
        else if(args[0].equalsIgnoreCase("startnetwork")){
            JMC.api().getNetworkManager().start();
            JMC.api().getNetworkManager().setCallback(evt -> {
                this.plugin.getLogger().info("Received packet type " + evt.getPacket().getClass() + " from server " + evt.getSenderName());
                if(evt.getPacket() instanceof Testpacket tp)this.plugin.getLogger().info("Message from test packet: " + tp.message);
            });
            sender.sendMessage("Network manager started");
            return true;
        }

        // Stop network
        else if(args[0].equalsIgnoreCase("stopnetwork")){
            JMC.api().getNetworkManager().stop();
            sender.sendMessage("Network manager stopped");
            return true;
        }

        // Test network
        else if(args[0].equalsIgnoreCase("testnetwork")){
            JMC.api().getNetworkManager().broadcast(new Testpacket("Why hello there ;)"));
            sender.sendMessage("Sent!");
            return true;
        }

        // Validation
        else if(args[0].equalsIgnoreCase("validation")){
            sender.sendMessage(((JMCnetwork)JMC.api().getNetworkManager()).util_validator());
            return true;
        }

        // Start client
        else if(args[0].equalsIgnoreCase("startclient")){
            if(args.length < 2){
                sender.sendMessage("Please specify a port!");
                return true;
            }

            int port = NumberConversions.toInt(args[1]);
            if(port == 0){
                sender.sendMessage("That is not a valid port!");
                return true;
            }

            JMC.api().connectToSocketServer("127.0.0.1", port, true, (@Nullable JunctionBridge bridge) -> {
                if(bridge == null)sender.sendMessage("Failed to establish connection with 127.0.0.1:" + port);
                else{
                    sender.sendMessage("Connection established with 127.0.0.1:" + port);
                    cmdTest.this.bridge = bridge;
                    cmdTest.this.listener = false;
                }
            });

            sender.sendMessage("Connecting...");
            return true;
        }

        // Start server
        else if(args[0].equalsIgnoreCase("startserver")){
            if(args.length < 2){
                sender.sendMessage("Please specify a port!");
                return true;
            }

            int port = NumberConversions.toInt(args[1]);
            if(port == 0){
                sender.sendMessage("That is not a valid port!");
                return true;
            }

            var server = JMC.api().createSocketServer(port, true, (@NotNull JunctionBridge bridge) -> {
                sender.sendMessage("Connection established on port " + port);
                cmdTest.this.bridge = bridge;
                cmdTest.this.listener = false;
            });

            if(server == null)sender.sendMessage("Failed to create server!");
            else sender.sendMessage("Server has started");

            return true;
        }

        // Say hi
        else if(args[0].equalsIgnoreCase("sayhi")){
            if(this.bridge == null || !this.bridge.isAlive()){
                sender.sendMessage("You must first connect before using this command!");
                return true;
            }

            if(!this.listener){
                sender.sendMessage("Setting listener...");
                this.bridge.setCallback(packet -> {
                    this.plugin.getLogger().info("Received a packet of type '" + packet.getClass() + "'");
                    if(packet instanceof  Testpacket tp)this.plugin.getLogger().info("Message: " + tp.message);
                });
            }

            this.bridge.send(new Testpacket(args.length > 1 ? args[1] : "hi there ;)"));

            sender.sendMessage("Sent!");

            return true;
        }

        // Status
        else if(args[0].equalsIgnoreCase("status")){
            if(this.bridge == null){
                sender.sendMessage("Bridge is null");
                return true;
            }

            sender.sendMessage("Bridge is " + (this.bridge.isAlive() ? "" : "not ") + "alive");
            return true;
        }

        // Close
        else if(args[0].equalsIgnoreCase("close")){
            this.bridge.close();
            this.bridge = null;
            sender.sendMessage("Closed!");
            return true;
        }

        return false;
    }
}
