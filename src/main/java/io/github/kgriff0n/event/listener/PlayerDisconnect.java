package io.github.kgriff0n.event.listener;

import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.packet.play.PlayerDisconnectPacket;
import io.github.kgriff0n.packet.info.ServersInfoPacket;
import io.github.kgriff0n.socket.Gateway;
import io.github.kgriff0n.socket.SubServer;
import io.github.kgriff0n.api.ServersLinkApi;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import java.util.UUID;

public class PlayerDisconnect implements ServerPlayConnectionEvents.Disconnect {
    @Override
    public void onPlayDisconnect(ServerGamePacketListenerImpl serverPlayNetworkHandler, MinecraftServer minecraftServer) {
        ServerPlayer player = serverPlayNetworkHandler.player;
        UUID uuid = player.getUUID();
        PlayerDisconnectPacket packet = new PlayerDisconnectPacket(uuid);

        // Remove player from list
        ServersLinkApi.getServer(ServersLink.getServerInfo().getName()).removePlayer(uuid);

        if (ServersLink.isGateway) {
            Gateway gateway = Gateway.getInstance();
            /* Delete player from list and send packet ONLY if the player is not transferred */
            if (!ServersLinkApi.getPreventDisconnect().contains(uuid)) {
                gateway.sendToAll(packet);
                gateway.sendToAll(new ServersInfoPacket(ServersLinkApi.getServerList()));
            }
        } else {
            SubServer connection = SubServer.getInstance();
            connection.send(packet);
        }
    }
}
