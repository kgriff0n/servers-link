package io.github.kgriff0n.socket;

import io.github.kgriff0n.Config;
import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.util.ServerInfo;
import io.github.kgriff0n.util.ServersLinkUtil;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;

import static io.github.kgriff0n.ServersLink.IS_RUNNING;

public class Hub extends Thread {

    public static Hub hub;

    private ServerSocket serverSocket;

    public Hub(int port) {
        if (hub != null) {
            ServersLink.LOGGER.info("Central server already started");
        }
        try {
            serverSocket = new ServerSocket(port);
            hub = this;
            ServerInfo serverInfo = new ServerInfo(Config.serverName, Config.serverIp, Config.serverPort);
            ServersLinkUtil.addServer(serverInfo, null);
        } catch (IOException e) {
            ServersLink.LOGGER.info("Unable to start central server");
        }
    }

    public static Hub getInstance() {
        return hub;
    }

    public void sendAll(Packet packet) {
        for (H2SConnection sub : ServersLinkUtil.getServerMap().values()) {
            if (sub != null) sub.send(packet);
        }
    }

    public void sendTo(Packet packet, String serverName) {
        for (ServerInfo server : ServersLinkUtil.getServerList()) {
            if (server.getName().equals(serverName)) {
                ServersLinkUtil.getServerMap().get(server).send(packet);
            }
        }
    }

    public void sendExcept(Packet packet, String serverName) {
        for (ServerInfo server : ServersLinkUtil.getServerList()) {
            if (!server.getName().equals(serverName)) {
                H2SConnection sub = ServersLinkUtil.getServerMap().get(server);
                if (sub != null) sub.send(packet);
            }
        }
    }

    public void removePlayer(UUID uuid) {
        for (ServerInfo server : ServersLinkUtil.getServerList()) {
            server.removePlayer(uuid);
        }
    }

    public boolean isConnectedPlayer(UUID uuid) {
        for (ServerInfo server : ServersLinkUtil.getServerList()) {
            if (server.getPlayersList().containsKey(uuid)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void run() {
        while (IS_RUNNING) {
            try {
                Socket socket = serverSocket.accept();
                if (Config.whitelistIp) {
                    if (Config.whitelistedIp.contains(socket.getInetAddress().getHostAddress())) {
                        H2SConnection connection = new H2SConnection(socket);
                        connection.start();
                    } else {
                        ServersLink.LOGGER.warn("Unauthorized connection received from {}", socket.getInetAddress().getHostAddress());
                        socket.close();
                    }
                } else {
                    H2SConnection connection = new H2SConnection(socket);
                    connection.start();
                }
            } catch (IOException e) {
                ServersLink.LOGGER.info("Unable to accept connection");
            }

        }
    }
}
