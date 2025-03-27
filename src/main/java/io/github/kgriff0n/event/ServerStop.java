package io.github.kgriff0n.event;

import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.packet.server.PlayerDataPacket;
import io.github.kgriff0n.packet.info.ServerStatusPacket;
import io.github.kgriff0n.packet.server.ServerStopPacket;
import io.github.kgriff0n.socket.Gateway;
import io.github.kgriff0n.socket.SubServer;
import io.github.kgriff0n.util.DummyPlayer;
import io.github.kgriff0n.api.ServersLinkApi;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.IOException;

import static io.github.kgriff0n.ServersLink.*;

public class ServerStop implements ServerLifecycleEvents.ServerStopping {
    @Override
    public void onServerStopping(MinecraftServer minecraftServer) {
        if (!CONFIG_ERROR) {
            if (isGateway) {
                Gateway.getInstance().sendAll(new ServerStopPacket());
                /* Wait for all servers to shut down */
                while (ServersLinkApi.getRunningSubServers() > 0) ;
                IS_RUNNING = false;
            } else {
                for (ServerPlayerEntity player : SERVER.getPlayerManager().getPlayerList()) {
                    if (!(player instanceof DummyPlayer)) {
                        SERVER.execute(() -> {
                            try {
                                SubServer.getInstance().send(new PlayerDataPacket(player.getUuid()));
                            } catch (IOException e) {
                                ServersLink.LOGGER.error("Unable to send player data for {}", player.getName());
                            }
                        });
                    }
                }
                SubServer.getInstance().send(new ServerStatusPacket(ServersLink.getServerInfo().getName(), 0.0f, true));
            }
        }
    }
}
