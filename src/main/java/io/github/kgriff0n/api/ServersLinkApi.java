package io.github.kgriff0n.api;

import com.mojang.authlib.GameProfile;
import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.packet.PacketHeader;
import io.github.kgriff0n.packet.info.ServersInfoPacket;
import io.github.kgriff0n.packet.play.PlayerDisconnectPacket;
import io.github.kgriff0n.packet.play.PlayerTransferRequestPacket;
import io.github.kgriff0n.socket.Gateway;
import io.github.kgriff0n.socket.G2SConnection;
import io.github.kgriff0n.socket.SubServer;
import io.github.kgriff0n.util.DummyPlayer;
import io.github.kgriff0n.server.ServerInfo;
import io.github.kgriff0n.util.PositionOverride;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static io.github.kgriff0n.ServersLink.SERVER;

public class ServersLinkApi {

    private static final ConcurrentHashMap<ServerInfo, G2SConnection> serverList = new ConcurrentHashMap<>();

    private static final HashSet<UUID> preventConnect = new HashSet<>();
    private static final HashSet<UUID> preventDisconnect = new HashSet<>();

    /** List of player UUIDs that can connect - Gateway will likely ignore this */
    private static final ArrayList<UUID> waitingPlayers = new ArrayList<>();

    private static final List<DummyPlayer> dummyPlayers = new ArrayList<>();

    private static final List<String> playerDataKeys = new ArrayList<>();

    private static final HashMap<UUID, PositionOverride> overridePosition = new HashMap<>();

    public static HashSet<UUID> getPreventConnect() {
        return preventConnect;
    }

    public static HashSet<UUID> getPreventDisconnect() {
        return preventDisconnect;
    }

    public static ArrayList<UUID> getWaitingPlayers() {
        return waitingPlayers;
    }

    public static void addWaitingPlayer(UUID uuid) {
        waitingPlayers.add(uuid);
    }

    public static void removeWaitingPlayer(UUID uuid) {
        waitingPlayers.remove(uuid);
    }

    public static ConcurrentHashMap<ServerInfo, G2SConnection> getServerMap() {
        return serverList;
    }

    public static ArrayList<ServerInfo> getImmutableServerList() {
        ArrayList<ServerInfo> list = new ArrayList<>();
        for (ServerInfo server : serverList.keySet()) {
            list.add(server.copy());
        }
        return list;
    }

    public static ArrayList<ServerInfo> getServerList() {
        return new ArrayList<>(serverList.keySet());
    }

    public static void setServerList(ArrayList<ServerInfo> list) {
        serverList.clear();
        for (ServerInfo server : list) {
            serverList.put(server, new G2SConnection(null));
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
    public static void addServer(ServerInfo server, @NotNull G2SConnection connection) {
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
                gateway.sendToAll(new PlayerDisconnectPacket(uuid));
                dummyPlayers.removeIf(player -> player.getUUID().equals(uuid));
            });
            server.getPlayersList().clear();
            server.getGameProfile().clear();
            serverList.put(server, new G2SConnection(null));
            gateway.sendToAll(new ServersInfoPacket(ServersLinkApi.getImmutableServerList()));
        });
    }

    /**
     * @return the total number of sub-servers
     *         connected to the hub
     */
    public static int getRunningSubServers() {
        int count = 0;
        for (G2SConnection connection : serverList.values()) {
            if (connection.isConnected()) count++;
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
     * Sends a packet.
     * @param packet the packet to send
     */
    public static void send(Packet packet) {
        if (ServersLink.isGateway) {
            packet.gatewayLogic();
            if (packet instanceof PacketHeader pkt) {
                Gateway.getInstance().sendTo(pkt.getRecipient(), packet);
            }
        } else {
            SubServer.getInstance().send(packet);
        }
    }

    /**
     * Sends a message to all operator players (ops).
     * @param text the text to send
     */
    public static void broadcastToOp(Component text) {
        for (String playerName : SERVER.getPlayerList().getOps().getUserList()) {
            ServerPlayer player = SERVER.getPlayerList().getPlayerByName(playerName);
            if (player != null && !(player instanceof DummyPlayer)) {
                player.sendSystemMessage(text);
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
        List<ServerPlayer> playerList = SERVER.getPlayerList().getPlayers();

        boolean alreadyPresent = false;
        for (DummyPlayer player : dummyPlayers) {
            if (player.getUUID().equals(profile.id())) {
                alreadyPresent = true;
            }
        }

        if (!alreadyPresent) {
            dummyPlayers.add(new DummyPlayer(profile));

            /* Update player list for all players */
            List<ServerPlayer> allPlayers = new ArrayList<>();
            allPlayers.addAll(playerList);
            allPlayers.addAll(dummyPlayers);
            for (ServerPlayer player : playerList) {
                player.connection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(allPlayers));
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
    public static ServerPlayer getDummyPlayer(UUID uuid) {
        for (DummyPlayer player : dummyPlayers) {
            if (player.getUUID().equals(uuid)) return player;
        }
        return null;
    }

    /**
     * Returns the player with the given username, used to retrieve dummy players.
     * @param playerName the name of the player
     * @return the player with this UUID
     */
    public static ServerPlayer getDummyPlayer(String playerName) {
        for (DummyPlayer player : dummyPlayers) {
            if (player.getScoreboardName().equals(playerName)) return player;
        }
        return null;
    }

    /**
     * Transfers a player to another server.
     * @param player the player to transfer
     * @param from name of the current server
     * @param to the name of the server to which the player will be transferred
     */
    public static void transferPlayer(ServerPlayer player, String from, String to) {
        PlayerTransferRequestPacket transferPacket = new PlayerTransferRequestPacket(player.getUUID(), from, to);
        ServersLinkApi.send(transferPacket);
    }

    public static void transferPlayer(ServerPlayer player, String from, String to, Vec3 position, Vec2 rotation, String world) {
        PlayerTransferRequestPacket transferPacket = new PlayerTransferRequestPacket(
                player.getUUID(), from, to,
                position.x(), position.y(), position.z(),
                rotation.x, rotation.y,
                world
        );
        ServersLinkApi.send(transferPacket);
    }

    public static String getServerName() {
        return ServersLink.getServerInfo().getName();
    }

    public static MinecraftServer getServer() {
        return SERVER;
    }

    public static void addPlayerDataKey(String key) {
        playerDataKeys.add(key);
    }

    public static List<String> getPlayerDataKeys() {
        return playerDataKeys;
    }

    public static void addPositionOverride(UUID uuid, Vec3 position, Vec2 rotation, String world) {
        overridePosition.put(uuid, new PositionOverride(position, rotation, world));
    }

    public static boolean shouldOverridePosition(UUID uuid) {
        return overridePosition.containsKey(uuid);
    }

    public static PositionOverride getPositionOverride(UUID uuid) {
        return overridePosition.remove(uuid);
    }

}
