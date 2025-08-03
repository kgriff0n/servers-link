package io.github.kgriff0n.event;

import io.github.kgriff0n.PlayersInformation;
import io.github.kgriff0n.socket.SubServer;
import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.socket.Gateway;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

import static io.github.kgriff0n.ServersLink.CONFIG_ERROR;

public class ServerStart implements ServerLifecycleEvents.ServerStarted {
    @Override
    public void onServerStarted(MinecraftServer minecraftServer) {
        if (CONFIG_ERROR) {
            ServersLink.LOGGER.error("You must configure servers-link before starting your server");
            minecraftServer.stop(false);
        } else {
            /* Initialize SERVER */
            ServersLink.SERVER = minecraftServer;
            if (ServersLink.isGateway) {
                // Players information
                PlayersInformation.loadNbt(minecraftServer);

                Gateway gateway = new Gateway(ServersLink.getGatewayPort());
                gateway.loadConfig();
                gateway.setDaemon(true);
                gateway.start();
            } else {
                SubServer connection = new SubServer(ServersLink.getGatewayIp(), ServersLink.getGatewayPort());
                connection.setDaemon(true);
                connection.start();
            }
        }
    }
}
