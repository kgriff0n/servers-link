package io.github.kgriff0n.packet.server;

import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.socket.SubServer;
import java.io.IOException;
import net.minecraft.server.level.ServerPlayer;

import static io.github.kgriff0n.ServersLink.SERVER;

public class PlayerDataSyncPacket implements Packet {
    @Override
    public void onReceive() {
        for (ServerPlayer player : SERVER.getPlayerList().getPlayers()) {
            try {
                SubServer.getInstance().send(new PlayerDataPacket(player.getUUID()));
            } catch (IOException e) {
                ServersLink.LOGGER.error("Unable to send player data for {}", player.getName());
            }
        }
    }
}
