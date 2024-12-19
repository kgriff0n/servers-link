package io.github.kgriff0n.event;

import io.github.kgriff0n.Config;
import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.packet.server.PlayerDataPacket;
import io.github.kgriff0n.packet.info.ServerStatusPacket;
import io.github.kgriff0n.packet.server.ServerStopPacket;
import io.github.kgriff0n.socket.Hub;
import io.github.kgriff0n.socket.SubServer;
import io.github.kgriff0n.util.DummyPlayer;
import io.github.kgriff0n.util.ServersLinkUtil;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.IOException;

import static io.github.kgriff0n.ServersLink.*;

public class ServerStop implements ServerLifecycleEvents.ServerStopping {
    @Override
    public void onServerStopping(MinecraftServer minecraftServer) {
        if (!CONFIG_ERROR) {
            if (Config.isHub) {
                Hub.getInstance().sendAll(new ServerStopPacket());
                /* Wait for all servers to shut down */
                while (ServersLinkUtil.getRunningSubServers() > 0) ;
                IS_RUNNING = false;
            } else {
                for (ServerPlayerEntity player : SERVER.getPlayerManager().getPlayerList()) {
                    if (!(player instanceof DummyPlayer)) {
                        try {
                            if (Config.syncPlayerData)
                                SubServer.getInstance().send(new PlayerDataPacket(player.getUuid()));
                        } catch (IOException e) {
                            ServersLink.LOGGER.error("Unable to send player data for {}", player.getName());
                        }
                    }
                }
                SubServer.getInstance().send(new ServerStatusPacket(Config.serverName, 0.0f, true));
            }
        }
    }
}
