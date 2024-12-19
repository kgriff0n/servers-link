package io.github.kgriff0n.socket;

import io.github.kgriff0n.Config;
import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.packet.info.NewPlayerPacket;
import io.github.kgriff0n.packet.info.NewServerPacket;
import io.github.kgriff0n.util.ServerInfo;
import io.github.kgriff0n.util.ServersLinkUtil;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.*;
import java.net.Socket;

import static io.github.kgriff0n.ServersLink.IS_RUNNING;
import static io.github.kgriff0n.ServersLink.SERVER;

public class H2SConnection extends Thread {

    private ServerInfo server;

    private final Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public H2SConnection(Socket socket) {
        this.socket = socket;
    }

    public synchronized void send(Packet packet) {
        try {
            out.writeObject(packet);
            out.flush();
            out.reset();
        } catch (IOException e) {
            ServersLink.LOGGER.error("Unable to send packet");
        }
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();

            in = new ObjectInputStream(socket.getInputStream());

            while (IS_RUNNING) {
                Packet packet = (Packet) in.readObject();
                if (packet instanceof NewServerPacket pkt) {
                    this.server = pkt.getServer();
                    ServersLinkUtil.addServer(server, this);
                    ServersLink.LOGGER.info("Add {} sub-server", server.getName());
                    /* Adds all players to the new server */
                    if (Config.syncPlayerList) {
                        for (ServerPlayerEntity player : SERVER.getPlayerManager().getPlayerList()) {
                            send(new NewPlayerPacket(player.getGameProfile()));
                        }
                    }
                }
                if (packet.shouldTransfer()) {
                    Hub.getInstance().sendExcept(packet, server.getName());
                }
                packet.onReceive();
            }
            socket.close();
        } catch (IOException e) {
            ServersLink.LOGGER.error("Error in sub-server {}", server.getName());
            ServersLinkUtil.disconnectServer(this.server);
            ServersLinkUtil.broadcastToOp(Text.literal("Sub-server " + server.getName() + " has disconnected").formatted(Formatting.RED));
        } catch (ClassNotFoundException e) {
            ServersLink.LOGGER.error("Receive invalid data: {}", e.getMessage());
        }
    }
}
