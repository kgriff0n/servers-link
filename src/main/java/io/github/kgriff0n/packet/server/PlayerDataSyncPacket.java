package io.github.kgriff0n.packet.server;

import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.socket.SubServer;
import io.github.kgriff0n.util.DummyPlayer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.IOException;

import static io.github.kgriff0n.ServersLink.LOGGER;
import static io.github.kgriff0n.ServersLink.SERVER;

public class PlayerDataSyncPacket implements Packet {
    @Override
    public void onReceive() {
        for (ServerPlayerEntity player : SERVER.getPlayerManager().getPlayerList()) {
            if (!(player instanceof DummyPlayer)) {
                SERVER.execute(() -> {
                    LOGGER.info("Send data for {}", player.getNameForScoreboard());
                    try {
                        SubServer.getInstance().send(new PlayerDataPacket(player.getUuid()));
                    } catch (IOException e) {
                        ServersLink.LOGGER.error("Unable to send player data for {}", player.getName());
                    }
                });
            }
        }
    }
}
