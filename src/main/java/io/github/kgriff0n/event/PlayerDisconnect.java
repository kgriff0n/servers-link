package io.github.kgriff0n.event;

import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.mixin.PlayerManagerInvoker;
import io.github.kgriff0n.packet.server.PlayerDataPacket;
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

import java.io.IOException;
import java.util.UUID;

import static io.github.kgriff0n.ServersLink.SERVER;

public class PlayerDisconnect implements ServerPlayConnectionEvents.Disconnect {
    @Override
    public void onPlayDisconnect(ServerPlayNetworkHandler serverPlayNetworkHandler, MinecraftServer minecraftServer) {
        ServerPlayerEntity player = serverPlayNetworkHandler.player;
        UUID uuid = player.getUuid();
        PlayerDisconnectPacket packet = new PlayerDisconnectPacket(uuid);

        /* Set player pos & last server */
        ((IPlayerServersLink) player).servers_link$setLastServer(ServersLink.getServerInfo().getName());
        ((IPlayerServersLink) player).servers_link$setServerPos(ServersLink.getServerInfo().getName(), player.getPos());

        if (ServersLink.isGateway) {
            Gateway gateway = Gateway.getInstance();
            /* Delete player from list and send packet ONLY if the player is not transferred */
            if (!ServersLinkApi.getPreventDisconnect().contains(uuid)) {
                ServersLinkApi.getServer(ServersLink.getServerInfo().getName()).removePlayer(uuid);
                gateway.sendAll(packet);
                gateway.sendAll(new ServersInfoPacket(ServersLinkApi.getServerList()));
            }
        } else {
            SubServer connection = SubServer.getInstance();
            /* Send packet ONLY if the player is not transferred */
            if (!ServersLinkApi.getPreventDisconnect().contains(uuid)) {
                connection.send(packet);
                /* Force inventory saving */
                SERVER.execute(() -> {
                    //FIXME don't force data save but detect post-disconnection
                    ((PlayerManagerInvoker) SERVER.getPlayerManager()).servers_link$savePlayerData(serverPlayNetworkHandler.player);
                    try {
                        connection.send(new PlayerDataPacket(uuid));
                    } catch (IOException e) {
                        ServersLink.LOGGER.error("Unable to send player data");
                    }
                });
            }
        }
    }
}
