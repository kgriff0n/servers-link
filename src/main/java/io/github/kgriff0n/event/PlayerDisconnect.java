package io.github.kgriff0n.event;

import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.packet.play.PlayerDisconnectPacket;
import io.github.kgriff0n.packet.info.ServersInfoPacket;
import io.github.kgriff0n.socket.Gateway;
import io.github.kgriff0n.socket.SubServer;
import io.github.kgriff0n.util.IPlayerServersLink;
import io.github.kgriff0n.api.ServersLinkApi;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

public class PlayerDisconnect implements ServerPlayConnectionEvents.Disconnect {
    @Override
    public void onPlayDisconnect(ServerPlayNetworkHandler serverPlayNetworkHandler, MinecraftServer minecraftServer) {
        ServerPlayerEntity player = serverPlayNetworkHandler.player;
        UUID uuid = player.getUuid();
        PlayerDisconnectPacket packet = new PlayerDisconnectPacket(uuid);

        /* Set player pos, dim & last server */
        ((IPlayerServersLink) player).servers_link$setServerPos(ServersLink.getServerInfo().getName(), player.getEntityPos());
        ((IPlayerServersLink) player).servers_link$setServerDim(ServersLink.getServerInfo().getName(), player.getEntityWorld());
        ((IPlayerServersLink) player).servers_link$setServerRot(ServersLink.getServerInfo().getName(), player.getYaw(), player.getPitch());

        // Remove player from list
        ServersLinkApi.getServer(ServersLink.getServerInfo().getName()).removePlayer(uuid);

        if (ServersLink.isGateway) {
            Gateway gateway = Gateway.getInstance();
            /* Delete player from list and send packet ONLY if the player is not transferred */
            if (!ServersLinkApi.getPreventDisconnect().contains(uuid)) {
                gateway.sendAll(packet);
                gateway.sendAll(new ServersInfoPacket(ServersLinkApi.getServerList()));
            }
        } else {
            SubServer connection = SubServer.getInstance();
            /* Send packet ONLY if the player is not transferred */
            if (!ServersLinkApi.getPreventDisconnect().contains(uuid)) {
                connection.send(packet);
            }
        }
    }
}
