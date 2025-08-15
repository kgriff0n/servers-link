package io.github.kgriff0n.socket;

import com.mojang.authlib.GameProfile;
import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.packet.info.NewPlayerPacket;
import io.github.kgriff0n.packet.info.NewServerPacket;
import io.github.kgriff0n.server.Group;
import io.github.kgriff0n.server.ServerInfo;
import io.github.kgriff0n.api.ServersLinkApi;
import io.github.kgriff0n.server.Settings;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import static io.github.kgriff0n.ServersLink.IS_RUNNING;
import static io.github.kgriff0n.ServersLink.SERVER;

public class G2SConnection extends Thread {

    private ServerInfo server;

    private final ExecutorService executor;

    private final Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public G2SConnection(Socket socket) {
        this.socket = socket;
        this.executor  = Executors.newSingleThreadExecutor();
    }

    public synchronized void send(Packet packet) {
        if (executor.isShutdown()) {
            ServersLink.LOGGER.warn("Can't send {}", packet.getClass().getName());
        } else {
            try {
                executor.submit(() -> {
                    try {
                        if (Gateway.getInstance().isDebugEnabled()) {
                            ServersLink.LOGGER.info("\u001B[34mPacket sent class {}", packet.getClass().getName());
                        }
                        out.writeObject(packet);
                        out.flush();
                        out.reset();
                    } catch (IOException e) {
                        ServersLink.LOGGER.error("Unable to send {} - {}", packet.getClass().getName(), e.getMessage());
                    }
                });
            } catch (RejectedExecutionException e) {
                ServersLink.LOGGER.error("Can't send {}, executor already closed", packet.getClass().getName());
            }
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
                if (Gateway.getInstance().isDebugEnabled()) ServersLink.LOGGER.info("\u001B[95mPacket received {}", packet.getClass());
                if (packet instanceof NewServerPacket pkt) {
                    this.server = pkt.getServer();
                    this.setName(String.format("%s thread", server.getName()));
                    ServersLinkApi.addServer(server, this);
                    Gateway.getInstance().getGroup(server.getGroupId()).addServer(server);
                    ServersLink.LOGGER.info("Add {} sub-server", server.getName());
                    /* Adds all players to the new server */
                    Settings globalSettings = Gateway.getInstance().getGroup("global").getSettings();
                    Settings serverSettings = Gateway.getInstance().getGroup(server.getGroupId()).getSettings();
                    if (globalSettings.isPlayerListSynced() && serverSettings.isPlayerListSynced()) {
                        // Send real players
                        for (ServerPlayerEntity player : SERVER.getPlayerManager().getPlayerList()) {
                            send(new NewPlayerPacket(player.getGameProfile()));
                        }
                        // Send fake players
                        for (ServerPlayerEntity player : ServersLinkApi.getDummyPlayers()) {
                            send(new NewPlayerPacket(player.getGameProfile()));
                        }
                    } else if (!globalSettings.isPlayerListSynced() && serverSettings.isPlayerListSynced()) {
                        Group serverGroup = Gateway.getInstance().getGroup(server.getGroupId());
                        List<ServerInfo> syncPlayers = new ArrayList<>(serverGroup.getServersList());
                        for (Group otherGroup : Gateway.getInstance().getGroups()) {
                            if (Gateway.getInstance().getSettings(serverGroup.getName(), otherGroup.getName()).isPlayerListSynced()) {
                                syncPlayers.addAll(otherGroup.getServersList());
                            }
                        }

                        for (ServerInfo serverInfo : syncPlayers) {
                            if (!serverInfo.getName().equals(server.getName())) {
                                for (GameProfile profile : serverInfo.getGameProfile()) {
                                    send(new NewPlayerPacket(profile));
                                }
                            }
                        }
                    }
                }
                SERVER.execute(() -> packet.onGatewayReceive(server.getName()));
            }
            socket.close();
        } catch (IOException e) {
            if (e.getMessage() != null) {
                ServersLink.LOGGER.error("Error {} in sub-server {}", e.getMessage(), server.getName());
                ServersLink.LOGGER.info(this.server.toString());
            }
            ServersLinkApi.disconnectServer(this.server);
            ServersLinkApi.broadcastToOp(Text.literal("Sub-server " + server.getName() + " has disconnected").formatted(Formatting.RED));
            this.interrupt();
        } catch (ClassNotFoundException e) {
            ServersLink.LOGGER.error("Receive invalid data: {}", e.getMessage());
        }
    }

    @Override
    public void interrupt() {
        super.interrupt();
        executor.shutdown();
    }
}
