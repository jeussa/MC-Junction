package com.jeussa.mc.junction.bungee;

import com.jeussa.mc.junction.JunctionBridge;
import com.jeussa.mc.junction.core.network.JMCnetwork;
import com.jeussa.mc.junction.packet.Testpacket;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class cmdTest extends Command{

    private final JMC plugin;

    private JunctionBridge bridge = null;
    private boolean listener = false;

    public cmdTest(JMC plugin){
        super("jtest");

        this.plugin = plugin;
    }

    /**
     * onCommand
     */
    @Override
    public void execute(@NotNull CommandSender sender, String @NotNull [] args){
        if(args.length < 1){
            sender.sendMessage("Please specify an argument!");
            return;
        }

        // Start network
        else if(args[0].equalsIgnoreCase("startnetwork")){
            JMC.api().getNetworkManager().start();
            JMC.api().getNetworkManager().setCallback(evt -> {
                this.plugin.getLogger().info("Received packet type " + evt.getPacket().getClass() + " from server " + evt.getSenderName());
                if(evt.getPacket() instanceof Testpacket tp)this.plugin.getLogger().info("Message from test packet: " + tp.message);
            });
            sender.sendMessage("Network manager started");
            return;
        }

        // Stop network
        else if(args[0].equalsIgnoreCase("stopnetwork")){
            JMC.api().getNetworkManager().stop();
            sender.sendMessage("Network manager stopped");
            return;
        }

        // Test network
        else if(args[0].equalsIgnoreCase("testnetwork")){
            JMC.api().getNetworkManager().broadcast(new Testpacket("Why hello there ;)"));
            sender.sendMessage("Sent!");
            return;
        }

        // Validation
        else if(args[0].equalsIgnoreCase("validation")){
            sender.sendMessage(((JMCnetwork)JMC.api().getNetworkManager()).util_validator());
            return;
        }

        // Start client
        else if(args[0].equalsIgnoreCase("startclient")){
            if(args.length < 2){
                sender.sendMessage("Please specify a port!");
                return;
            }

            int port = Integer.parseInt(args[1]);
            if(port == 0){
                sender.sendMessage("That is not a valid port!");
                return ;
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
            return;
        }

        // Start server
        else if(args[0].equalsIgnoreCase("startserver")){
            if(args.length < 2){
                sender.sendMessage("Please specify a port!");
                return;
            }

            int port = Integer.parseInt(args[1]);
            if(port == 0){
                sender.sendMessage("That is not a valid port!");
                return;
            }

            var server = JMC.api().createSocketServer(port, true, (@NotNull JunctionBridge bridge) -> {
                sender.sendMessage("Connection established on port " + port);
                cmdTest.this.bridge = bridge;
                cmdTest.this.listener = false;
            });

            if(server == null)sender.sendMessage("Failed to create server!");
            else sender.sendMessage("Server has started");

            return;
        }

        // Say hi
        else if(args[0].equalsIgnoreCase("sayhi")){
            if(this.bridge == null || !this.bridge.isAlive()){
                sender.sendMessage("You must first connect before using this command!");
                return;
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

            return;
        }

        // Status
        else if(args[0].equalsIgnoreCase("status")){
            if(this.bridge == null){
                sender.sendMessage("Bridge is null");
                return;
            }

            sender.sendMessage("Bridge is " + (this.bridge.isAlive() ? "" : "not ") + "alive");
            return;
        }

        // Close
        else if(args[0].equalsIgnoreCase("close")){
            this.bridge.close();
            this.bridge = null;
            sender.sendMessage("Closed!");
            return;
        }

        sender.sendMessage("Unknown argument!");
    }
}
