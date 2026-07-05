package io.github.kgriff0n.event.listener;

import io.github.kgriff0n.PlayersInformation;
import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.event.ServersLinkEvents;
import io.github.kgriff0n.packet.info.NewPlayerPacket;
import io.github.kgriff0n.packet.server.PlayerAcknowledgementPacket;
import io.github.kgriff0n.packet.info.ServersInfoPacket;
import io.github.kgriff0n.socket.Gateway;
import io.github.kgriff0n.socket.SubServer;
import io.github.kgriff0n.server.ServerInfo;
import io.github.kgriff0n.api.ServersLinkApi;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public class PlayerJoin implements ServerPlayerEvents.Join {

    @Override
    public void onJoin(@NotNull ServerPlayer player) {

        /* Dummy player packet */
        NewPlayerPacket dummyPlayer = new NewPlayerPacket(player.getGameProfile());

        /* Players can only connect from the hub */
        if (ServersLink.isGateway) {
            Gateway gateway = Gateway.getInstance();
            if (gateway.isConnectedPlayer(player.getUUID()) && !ServersLinkApi.getPreventConnect().contains(player.getUUID())) {
                ServersLinkApi.transferPlayer(player, ServersLink.getServerInfo().getName(), ServersLinkApi.whereIs(player.getUUID()));
                ServersLinkApi.getPreventConnect().add(player.getUUID());
                ServersLinkApi.getPreventDisconnect().add(player.getUUID());
            } else {
                String lastServer = PlayersInformation.getLastServer(player.getUUID());
                ServerInfo lastServerInfo = ServersLinkApi.getServer(lastServer);

                if (lastServer == null) {
                    ServersLinkEvents.FIRST_JOIN.invoker().onFirstJoin(player);
                }

                if (lastServer == null || lastServer.equals(ServersLink.getServerInfo().getName())
                        || lastServerInfo == null || lastServerInfo.isDown() || !gateway.shouldReconnectToLastServer()) {
                    ServersLinkApi.getServer(ServersLink.getServerInfo().getName()).addPlayer(player.getGameProfile());
                    /* Delete the fake player */
                    ServersLinkApi.getDummyPlayers().removeIf(dummy -> dummy.getName().equals(player.getName()));

                    /* Send player information to other servers */
                    gateway.sendToAllFrom(ServersLink.getServerInfo().getName(), dummyPlayer);
                    gateway.sendToAll(new ServersInfoPacket(ServersLinkApi.getImmutableServerList()));

                    if (gateway.shouldReconnectToLastServer() && lastServer != null && !lastServer.isEmpty() && (lastServerInfo == null || lastServerInfo.isDown())) {
                        player.sendSystemMessage(Component.literal("An unexpected error occurred while attempting to reconnect you to your previous server").withStyle(ChatFormatting.RED));
                    }
                } else {
                    ServersLinkApi.transferPlayer(player, ServersLink.getServerInfo().getName(), lastServer);
                }
            }
        } else {
            SubServer connection = SubServer.getInstance();
            if (!ServersLinkApi.getWaitingPlayers().contains(player.getUUID())) {
                player.connection.disconnect(Component.translatable("multiplayer.status.cannot_connect").withStyle(ChatFormatting.RED));
                /* Used to prevent the logout message in ServerPlayNetworkHandlerMixin#preventDisconnectMessage */
                ServersLinkApi.getPreventConnect().add(player.connection.player.getUUID());
                ServersLinkApi.getPreventDisconnect().add(player.connection.player.getUUID());
            } else {
                /* The player logs in and is removed from the list of waiting players */
                ServersLinkApi.removeWaitingPlayer(player.getUUID());
                /* Delete the fake player */
                ServersLinkApi.getDummyPlayers().removeIf(dummy -> dummy.getName().equals(player.getName()));
                /* Send player information to other servers */
                connection.send(dummyPlayer);
                connection.send(new PlayerAcknowledgementPacket(ServersLink.getServerInfo().getName(), player.getGameProfile()));
            }
        }

    }
}
