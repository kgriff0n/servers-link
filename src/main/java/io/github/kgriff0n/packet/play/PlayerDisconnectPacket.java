package io.github.kgriff0n.packet.play;

import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.packet.info.ServersInfoPacket;
import io.github.kgriff0n.server.Settings;
import io.github.kgriff0n.socket.Gateway;
import io.github.kgriff0n.api.ServersLinkApi;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.github.kgriff0n.ServersLink.SERVER;

public class PlayerDisconnectPacket implements Packet {

    private final UUID uuid;

    public PlayerDisconnectPacket(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public boolean shouldReceive(Settings settings) {
        return settings.isPlayerListSynced();
    }

    @Override
    public void onReceive() {
        List<ServerPlayerEntity> playerList = SERVER.getPlayerManager().getPlayerList();
        /* Delete the fake player */
        playerList.removeIf(player -> player.getUuid().equals(uuid));

        /* Update player list for all players */
        for (ServerPlayerEntity player : playerList) {
            List<UUID> list = new ArrayList<>();
            list.add(uuid);
            player.networkHandler.sendPacket(new PlayerRemoveS2CPacket(list));
        }
    }

    @Override
    public void onGatewayReceive(String sender) {
        Packet.super.onGatewayReceive(sender);
        Gateway.getInstance().removePlayer(uuid);
        Gateway.getInstance().sendAll(new ServersInfoPacket(ServersLinkApi.getServerList()));
    }
}
