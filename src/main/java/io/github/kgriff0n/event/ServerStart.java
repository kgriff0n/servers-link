package io.github.kgriff0n.event;

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
            ServersLink.LOGGER.error("Please configure the servers-link.properties file !");
            minecraftServer.stop(false);
        } else {

            /* Initialize SERVER */
            ServersLink.SERVER = minecraftServer;

            if (ServersLink.isGateway) {
                Gateway gateway = new Gateway(ServersLink.getGatewayPort());
                gateway.setDaemon(true);
                gateway.start();
            } else {
                SubServer connection = new SubServer(ServersLink.getGatewayIp(), ServersLink.getGatewayPort());
                connection.start();
            }
        }
    }
}
