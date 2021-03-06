package org.mcteam.ancientgates.commands.base;

import org.mcteam.ancientgates.Conf;
import org.mcteam.ancientgates.Gate;
import org.mcteam.ancientgates.Plugin;
import org.mcteam.ancientgates.Server;
import org.mcteam.ancientgates.commands.BaseCommand;
import org.mcteam.ancientgates.sockets.SocketClient;
import org.mcteam.ancientgates.sockets.events.SocketClientEventListener;
import org.mcteam.ancientgates.sockets.types.Packet;
import org.mcteam.ancientgates.sockets.types.Packets;
import org.mcteam.ancientgates.util.TeleportUtil;
import org.mcteam.ancientgates.util.types.PluginMessage;

public class CommandAddTo extends BaseCommand {
	
	public CommandAddTo() {
		aliases.add("addto");
		
		requiredParameters.add("id");
		
		requiredPermission = "ancientgates.addto";
		
		helpDescription = "Add another \"to\" to your location";
	}
	
	public void perform() {
		// Check if optional parameter exists
		String serverName = null;
		if (parameters.size() > 1) {
			serverName = parameters.get(1);
		}
		
		// Local 'addto' command
		if (serverName == null || !Conf.bungeeCordSupport) {
			if (gate.getTos() == null) {
				sendMessage("This gate needs an initial \"to\" location. Use:");
				sendMessage(new CommandSetTo().getUsageTemplate(true, true));
				return;
			}
			if (gate.getTos().size() < 1) {
				sendMessage("This gate needs an initial \"to\" location. Use:");
				sendMessage(new CommandSetTo().getUsageTemplate(true, true));
				return;
			}
		
			gate.addTo(player.getLocation());
			gate.addBungeeTo(null, null);
			sendMessage("Another \"to\" location for gate \""+gate.getId()+"\" is now where you stand.");
			Gate.save();
			
		// External BungeeCord 'addto' command
		} else {
			// Check bungeeServerName found
			if (Plugin.bungeeServerName == null) {
				sendMessage("Still connecting to BungeeCord. Try again.");
				return;
			}
			
			// Send command packet via BungeeCord
			if (!Conf.useSocketComms || Plugin.serv == null) {
				// Build the message, format is <command>#@#<player>#@#<server>#@#<gateid>#@#<data>
				String[] args = new String[] {parameters.get(0), TeleportUtil.locationToString(player.getLocation()), Plugin.bungeeServerName};
				PluginMessage msg = new PluginMessage("addto", player, serverName, args);
					
				// Send over the AGBungeeCom BungeeCord channel
				player.sendPluginMessage(Plugin.instance, "BungeeCord", msg.toByteArray());
				sendMessage("Another \"to\" location for gate \""+parameters.get(0)+"\" on server \""+serverName+"\" has been sent.");

			// Send command packet via client socket
			} else {
				// Check server exists
				if (!Server.exists(serverName)) {
					sendMessage("The server \""+serverName+"\" does not exist.");
					return;
				}
					
				// Get server
				final Server server = Server.get(serverName);
					
				// Build the packet, format is <player>,<server>,<gateid>,<data>,<fromserver>
				String[] args = new String[] {player.getName(), Plugin.bungeeServerName, parameters.get(0), TeleportUtil.locationToString(player.getLocation()), serverName};
				Packet packet = new Packet("addto", args);
					
				// Setup socket client and listener
				SocketClient client = new SocketClient(server.getAddress(), server.getPort(), server.getPassword());
				client.setListener(new SocketClientEventListener() {
					public void onServerMessageRecieve(SocketClient client, Packets packets) {
					    for (Packet packet : packets.packets) {
					    	if (packet.command.toLowerCase().equals("sendmsg")) {
					    		sendMessage(packet.args[0]);
					    	}
					    }
					    client.close();
					}
					public void onServerMessageError() {
						sendMessage("Could not connect to server \""+server.getName()+"\".");
						Plugin.log("There was an error connection to the server.");
					}
				});
					
				// Connect and send packet
				try {
					client.connect();
					client.send(packet);
				} catch (Exception e) {
					sendMessage("Could not connect to server \""+serverName+"\".");
					Plugin.log("There was an error connection to the server.");
				}
			}
		}
			
	}     
	
}