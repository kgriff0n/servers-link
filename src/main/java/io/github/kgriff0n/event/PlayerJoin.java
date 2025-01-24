package io.github.kgriff0n.event;

import io.github.kgriff0n.Config;
import io.github.kgriff0n.packet.info.NewPlayerPacket;
import io.github.kgriff0n.packet.server.PlayerAcknowledgementPacket;
import io.github.kgriff0n.packet.info.ServersInfoPacket;
import io.github.kgriff0n.socket.Hub;
import io.github.kgriff0n.socket.SubServer;
import io.github.kgriff0n.util.IPlayerServersLink;
import io.github.kgriff0n.util.ServerInfo;
import io.github.kgriff0n.util.ServersLinkUtil;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import static io.github.kgriff0n.Config.isHub;
import static io.github.kgriff0n.ServersLink.SERVER;

public class PlayerJoin implements ServerPlayConnectionEvents.Join {

    @Override
    public void onPlayReady(ServerPlayNetworkHandler serverPlayNetworkHandler, PacketSender packetSender, MinecraftServer minecraftServer) {

        ServerPlayerEntity newPlayer = serverPlayNetworkHandler.player;
        /* Load player pos */
        Vec3d pos = ((IPlayerServersLink) newPlayer).servers_link$getServerPos(Config.serverName);
        if (pos != null) newPlayer.setPosition(pos);

        /* Dummy player packet */
        NewPlayerPacket dummyPlayer = new NewPlayerPacket(newPlayer.getGameProfile());

        /* Players can only connect from the hub */
        if (isHub) {
            Hub hub = Hub.getInstance();
            if (hub.isConnectedPlayer(newPlayer.getUuid()) && !ServersLinkUtil.getPreventConnect().contains(newPlayer.getUuid())) {
                serverPlayNetworkHandler.disconnect(Text.translatable("multiplayer.status.cannot_connect").formatted(Formatting.RED));
                ServersLinkUtil.getPreventConnect().add(newPlayer.getUuid());
                ServersLinkUtil.getPreventDisconnect().add(newPlayer.getUuid());
            } else {

                String lastServer = ((IPlayerServersLink) newPlayer).servers_link$getLastServer();
                String nextServer = ((IPlayerServersLink) newPlayer).servers_link$getNextServer();
                ServerInfo lastServerInfo = ServersLinkUtil.getServer(lastServer);
                if (lastServer == null || lastServer.equals(Config.serverName) || nextServer.equals(Config.serverName)
                        || lastServerInfo == null || lastServerInfo.isDown() || !Config.reconnectToLastServer) {
                    ServersLinkUtil.getServer(Config.serverName).addPlayer(newPlayer.getUuid(), newPlayer.getName().getString());
                    /* Delete the fake player */
                    SERVER.getPlayerManager().getPlayerList().removeIf(player -> player.getName().equals(newPlayer.getName()));

                    /* Send player information to other servers */
                    if (Config.syncPlayerList) hub.sendAll(dummyPlayer);
                    hub.sendAll(new ServersInfoPacket(ServersLinkUtil.getServerList()));

                    if (Config.reconnectToLastServer && lastServer != null && !lastServer.isEmpty() && (lastServerInfo == null || lastServerInfo.isDown())) {
                        newPlayer.sendMessage(Text.literal("An unexpected error occurred while attempting to reconnect you to your previous server").formatted(Formatting.RED));
                    }
                } else {
                    ServersLinkUtil.transferPlayer(newPlayer, lastServer, false);
                }
            }
        } else {
            SubServer connection = SubServer.getInstance();
            if (!connection.getWaitingPlayers().contains(newPlayer.getUuid())) {
                serverPlayNetworkHandler.disconnect(Text.translatable("multiplayer.status.cannot_connect").formatted(Formatting.RED));
                /* Used to prevent the logout message in ServerPlayNetworkHandlerMixin#preventDisconnectMessage */
                ServersLinkUtil.getPreventConnect().add(serverPlayNetworkHandler.player.getUuid());
                ServersLinkUtil.getPreventDisconnect().add(serverPlayNetworkHandler.player.getUuid());
            } else {
                /* The player logs in and is removed from the list of waiting players */
                connection.removeWaitingPlayer(newPlayer.getUuid());
                /* Delete the fake player */
                SERVER.getPlayerManager().getPlayerList().removeIf(player -> player.getName().equals(newPlayer.getName()));
                /* Send player information to other servers */
                if (Config.syncPlayerList) connection.send(dummyPlayer);
                connection.send(new PlayerAcknowledgementPacket(Config.serverName, newPlayer.getUuid(), newPlayer.getName().getString()));
            }
        }

    }
}
