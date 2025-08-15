package io.github.kgriff0n.api;

import com.mojang.authlib.GameProfile;
import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.event.ServerTick;
import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.packet.info.ServersInfoPacket;
import io.github.kgriff0n.packet.play.PlayerDisconnectPacket;
import io.github.kgriff0n.packet.play.PlayerTransferPacket;
import io.github.kgriff0n.socket.Gateway;
import io.github.kgriff0n.socket.G2SConnection;
import io.github.kgriff0n.socket.SubServer;
import io.github.kgriff0n.util.DummyPlayer;
import io.github.kgriff0n.server.ServerInfo;
import net.minecraft.network.packet.s2c.common.ServerTransferS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static io.github.kgriff0n.ServersLink.SERVER;

public class ServersLinkApi {

    private static final HashMap<ServerInfo, G2SConnection> serverList = new HashMap<>();

    private static final HashSet<UUID> preventConnect = new HashSet<>();
    private static final HashSet<UUID> preventDisconnect = new HashSet<>();

    public static List<DummyPlayer> dummyPlayers = new ArrayList<>();

    public static HashSet<UUID> getPreventConnect() {
        return preventConnect;
    }

    public static HashSet<UUID> getPreventDisconnect() {
        return preventDisconnect;
    }

    public static HashMap<ServerInfo, G2SConnection> getServerMap() {
        return serverList;
    }

    public static ArrayList<ServerInfo> getServerList() {
        return new ArrayList<>(serverList.keySet());
    }

    public static void setServerList(ArrayList<ServerInfo> list) {
        serverList.clear();
        for (ServerInfo server : list) {
            serverList.put(server, null);
        }
    }

    /**
     * Retrieves the list of server names
     * @return a list containing all the server names
     */
    public static ArrayList<String> getServerNames() {
        ArrayList<String> names = new ArrayList<>();
        for (ServerInfo server : serverList.keySet()) {
            names.add(server.getName());
        }
        return names;
    }

    /**
     * @param groupId id of the group
     * @return the list of server from a specified group
     */
    public static ArrayList<ServerInfo> getServers(String groupId) {
        ArrayList<ServerInfo> list = new ArrayList<>();
        for (ServerInfo server : serverList.keySet()) {
            if (server.getGroupId().equals(groupId)) {
                list.add(server);
            }
        }
        return list;
    }

    /**
     * @param serverName the name of the server
     * @return the server with this name
     */
    public static ServerInfo getServer(String serverName) {
        for (ServerInfo server : serverList.keySet()) {
            if (server.getName().equals(serverName)) {
                return server;
            }
        }
        return null;
    }

    /**
     * Adds a new server to the list of sub-servers
     * @param server a new server
     * @param connection used from the hub for packet transfer
     */
    public static void addServer(ServerInfo server, @Nullable G2SConnection connection) {
        SERVER.execute(() -> {
            serverList.remove(server); // remove old one
            serverList.put(server, connection);
        });
    }

    /**
     * Disconnects a server from the hub and prevents packets
     * from being sent to that server
     * @param server the server to be disconnected
     */
    public static void disconnectServer(ServerInfo server) {
        SERVER.execute(() -> {
            Gateway gateway = Gateway.getInstance();
            server.getPlayersList().forEach((uuid, name) -> {
                gateway.sendAll(new PlayerDisconnectPacket(uuid));
                dummyPlayers.removeIf(player -> player.getUuid().equals(uuid));
            });
            gateway.sendAll(new ServersInfoPacket(ServersLinkApi.getServerList()));
            server.getPlayersList().clear();
            server.getGameProfile().clear();
            serverList.put(server, null);
        });
    }

    /**
     * @return the total number of sub-servers
     *         connected to the hub
     */
    public static int getRunningSubServers() {
        int count = 0;
        for (G2SConnection connection : serverList.values()) {
            if (connection != null) count++;
        }
        return count;
    }

    /**
     * Find out which server a player is connected to
     * @param uuid the player uuid
     * @return the name of the server
     */
    public static String whereIs(UUID uuid) {
        for (ServerInfo serverInfo : ServersLinkApi.getServerList()) {
            if (serverInfo.getPlayersList().containsKey(uuid)) {
                return serverInfo.getName();
            }
        }
        return null;
    }

    /**
     * Sends a packet. If called from the hub, the packet is
     * sent to all other sub-servers, otherwise it is sent to the hub.
     * @param packet the packet to send
     */
    public static void send(Packet packet, String source) {
        if (ServersLink.isGateway) {
            Gateway.getInstance().forward(packet, source);
        } else {
            SubServer.getInstance().send(packet);
        }
    }

    /**
     * Sends a message to all operator players (ops).
     * @param text the text to send
     */
    public static void broadcastToOp(Text text) {
        for (String playerName : SERVER.getPlayerManager().getOpList().getNames()) {
            ServerPlayerEntity player = SERVER.getPlayerManager().getPlayer(playerName);
            if (player != null && !(player instanceof DummyPlayer)) {
                player.sendMessage(text);
            }
        }
    }

    /**
     * Adds a dummy player to the list of players,
     * allowing it to be displayed in the list and in the command auto-completion.
     * If a player or a dummy player with the same uuid is
     * already present in the list, the dummy player will not be added.
     * @param profile profile of the player, must contain his uuid, name and textures properties
     */
    public static void addDummyPlayer(GameProfile profile) {
        List<ServerPlayerEntity> playerList = SERVER.getPlayerManager().getPlayerList();

        boolean alreadyPresent = false;
        for (DummyPlayer player : dummyPlayers) {
            if (player.getUuid().equals(profile.getId())) {
                alreadyPresent = true;
            }
        }

        if (!alreadyPresent) {
            dummyPlayers.add(new DummyPlayer(profile));

            /* Update player list for all players */
            List<ServerPlayerEntity> allPlayers = new ArrayList<>();
            allPlayers.addAll(playerList);
            allPlayers.addAll(dummyPlayers);
            for (ServerPlayerEntity player : playerList) {
                player.networkHandler.sendPacket(PlayerListS2CPacket.entryFromPlayer(allPlayers));
            }
        }
    }

    public static List<DummyPlayer> getDummyPlayers() {
        return dummyPlayers;
    }

    /**
     * Returns the player with the given UUID, used to retrieve dummy players.
     * @param uuid the UUID of the player
     * @return the player with this UUID
     */
    public static ServerPlayerEntity getDummyPlayer(UUID uuid) {
        for (DummyPlayer player : dummyPlayers) {
            if (player.getUuid().equals(uuid)) return player;
        }
        return null;
    }

    /**
     * Returns the player with the given username, used to retrieve dummy players.
     * @param playerName the name of the player
     * @return the player with this UUID
     */
    public static ServerPlayerEntity getDummyPlayer(String playerName) {
        for (DummyPlayer player : dummyPlayers) {
            if (player.getNameForScoreboard().equals(playerName)) return player;
        }
        return null;
    }

    /**
     * Transfers a player to another server.
     * @param player the player to transfer
     * @param originServer name of the current server
     * @param serverName the name of the server to which the player will be transferred
     */
    public static void transferPlayer(ServerPlayerEntity player, String originServer, String serverName) {
        ServerInfo server = ServersLinkApi.getServer(serverName);

        if (ServersLink.isGateway) {
            /* add player to other server list and send packet */
            PlayerTransferPacket transferPacket = new PlayerTransferPacket(player.getUuid(), serverName);
            transferPacket.onGatewayReceive(originServer);
        } else {
            /* send packet, add player to transferred list and transfer the player */
            SubServer connection = SubServer.getInstance();
            connection.send(new PlayerTransferPacket(player.getUuid(), serverName));
        }

        player.networkHandler.sendPacket(new ServerTransferS2CPacket(server.getIp(), server.getPort()));
        ServerTick.scheduleDisconnect(player.getUuid(), 20); // delay
    }

}
