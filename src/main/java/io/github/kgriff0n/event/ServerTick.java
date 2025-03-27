package io.github.kgriff0n.event;

import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.packet.info.ServerStatusPacket;
import io.github.kgriff0n.packet.info.ServersInfoPacket;
import io.github.kgriff0n.socket.Gateway;
import io.github.kgriff0n.socket.SubServer;
import io.github.kgriff0n.api.ServersLinkApi;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

public class ServerTick implements ServerTickEvents.StartTick {

    private static int count = 0;

    @Override
    public void onStartTick(MinecraftServer minecraftServer) {
        count++;
        if (count >= 600) { // every 30s
            count = 0;
            float tps = minecraftServer.getTickManager().getTickRate();
            /* update self */
            ServersLinkApi.getServer(ServersLink.getServerInfo().getName()).setTps(tps);
            if (ServersLink.isGateway) {
                Gateway.getInstance().sendAll(new ServersInfoPacket(ServersLinkApi.getServerList()));
            } else {
                SubServer.getInstance().send(new ServerStatusPacket(ServersLink.getServerInfo().getName(), tps, false));
            }
        }
    }
}
