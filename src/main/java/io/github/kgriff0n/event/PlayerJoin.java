package io.github.kgriff0n.event;

import io.github.kgriff0n.PlayersInformation;
import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.packet.info.NewPlayerPacket;
import io.github.kgriff0n.packet.server.PlayerAcknowledgementPacket;
import io.github.kgriff0n.packet.info.ServersInfoPacket;
import io.github.kgriff0n.socket.Gateway;
import io.github.kgriff0n.socket.SubServer;
import io.github.kgriff0n.server.ServerInfo;
import io.github.kgriff0n.api.ServersLinkApi;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

public class PlayerJoin implements ServerPlayConnectionEvents.Join {

    @Override
    public void onPlayReady(ServerPlayNetworkHandler serverPlayNetworkHandler, @NotNull PacketSender packetSender, @NotNull MinecraftServer minecraftServer) {

        ServerPlayerEntity newPlayer = serverPlayNetworkHandler.player;

        /* Dummy player packet */
        NewPlayerPacket dummyPlayer = new NewPlayerPacket(newPlayer.getGameProfile());

        /* Players can only connect from the hub */
        if (ServersLink.isGateway) {
            Gateway gateway = Gateway.getInstance();
            if (gateway.isConnectedPlayer(newPlayer.getUuid()) && !ServersLinkApi.getPreventConnect().contains(newPlayer.getUuid())) {
                ServersLinkApi.transferPlayer(newPlayer, ServersLink.getServerInfo().getName(), ServersLinkApi.whereIs(newPlayer.getUuid()));
                ServersLinkApi.getPreventConnect().add(newPlayer.getUuid());
                ServersLinkApi.getPreventDisconnect().add(newPlayer.getUuid());
            } else {
                String lastServer = PlayersInformation.getLastServer(newPlayer.getUuid());
                ServerInfo lastServerInfo = ServersLinkApi.getServer(lastServer);
                if (lastServer == null || lastServer.equals(ServersLink.getServerInfo().getName())
                        || lastServerInfo == null || lastServerInfo.isDown() || !gateway.shouldReconnectToLastServer()) {
                    ServersLinkApi.getServer(ServersLink.getServerInfo().getName()).addPlayer(newPlayer.getGameProfile());
                    /* Delete the fake player */
                    ServersLinkApi.getDummyPlayers().removeIf(player -> player.getName().equals(newPlayer.getName()));

                    /* Send player information to other servers */
                    gateway.sendToAllFrom(ServersLink.getServerInfo().getName(), dummyPlayer);
                    gateway.sendToAll(new ServersInfoPacket(ServersLinkApi.getServerList()));

                    if (gateway.shouldReconnectToLastServer() && lastServer != null && !lastServer.isEmpty() && (lastServerInfo == null || lastServerInfo.isDown())) {
                        newPlayer.sendMessage(Text.literal("An unexpected error occurred while attempting to reconnect you to your previous server").formatted(Formatting.RED));
                    }
                } else {
                    ServersLinkApi.transferPlayer(newPlayer, ServersLink.getServerInfo().getName(), lastServer);
                }
            }
        } else {
            SubServer connection = SubServer.getInstance();
            if (!ServersLinkApi.getWaitingPlayers().contains(newPlayer.getUuid())) {
                serverPlayNetworkHandler.disconnect(Text.translatable("multiplayer.status.cannot_connect").formatted(Formatting.RED));
                /* Used to prevent the logout message in ServerPlayNetworkHandlerMixin#preventDisconnectMessage */
                ServersLinkApi.getPreventConnect().add(serverPlayNetworkHandler.player.getUuid());
                ServersLinkApi.getPreventDisconnect().add(serverPlayNetworkHandler.player.getUuid());
            } else {
                /* The player logs in and is removed from the list of waiting players */
                ServersLinkApi.removeWaitingPlayer(newPlayer.getUuid());
                /* Delete the fake player */
                ServersLinkApi.getDummyPlayers().removeIf(player -> player.getName().equals(newPlayer.getName()));
                /* Send player information to other servers */
                connection.send(dummyPlayer);
                connection.send(new PlayerAcknowledgementPacket(ServersLink.getServerInfo().getName(), newPlayer.getGameProfile()));
            }
        }

    }
}
