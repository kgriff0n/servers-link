package io.github.kgriff0n.event;

import io.github.kgriff0n.Config;
import io.github.kgriff0n.socket.SubServer;
import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.socket.Hub;
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

            if (Config.isHub) {
                Hub hub = new Hub(Config.hubPort);
                hub.setDaemon(true);
                hub.start();
            } else {
                SubServer connection = new SubServer(Config.hubIp, Config.hubPort);
                connection.setDaemon(true);
                connection.start();
            }
        }
    }
}
