package io.github.kgriff0n.event;

import io.github.kgriff0n.socket.Gateway;
import io.github.kgriff0n.socket.SubServer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

import static io.github.kgriff0n.ServersLink.*;

public class ServerStopped implements ServerLifecycleEvents.ServerStopped {
    @Override
    public void onServerStopped(MinecraftServer server) {
        if (!CONFIG_ERROR) {
            if (isGateway) {
                Gateway.getInstance().interrupt();
            } else {
                SubServer.getInstance().interrupt();
            }
        }
    }
}
