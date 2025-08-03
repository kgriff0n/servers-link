package io.github.kgriff0n.event;

import io.github.kgriff0n.PlayersInformation;
import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.packet.info.ServerStatusPacket;
import io.github.kgriff0n.packet.server.PlayerDataSyncPacket;
import io.github.kgriff0n.packet.server.ServerStopPacket;
import io.github.kgriff0n.socket.Gateway;
import io.github.kgriff0n.socket.SubServer;
import io.github.kgriff0n.api.ServersLinkApi;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

import static io.github.kgriff0n.ServersLink.*;

public class ServerStop implements ServerLifecycleEvents.ServerStopping {
    @Override
    public void onServerStopping(MinecraftServer server) {
        if (!CONFIG_ERROR) {
            if (isGateway) {
                Gateway.getInstance().sendAll(new PlayerDataSyncPacket());
                try {
                    LOGGER.info("Begin servers synchronization");
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    LOGGER.error("Unable to synchronize servers");
                }
                Gateway.getInstance().sendAll(new ServerStopPacket());
                /* Wait for all servers to shut down */
                while (ServersLinkApi.getRunningSubServers() > 0);
                IS_RUNNING = false;
                PlayersInformation.saveNbt(server);
                Gateway.getInstance().interrupt();
            } else {
                /* Confirm shutdown */
                SubServer.getInstance().send(new ServerStatusPacket(ServersLink.getServerInfo().getName(), 0.0f, true));
            }
        }
    }
}
