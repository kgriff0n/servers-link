package io.github.kgriff0n.event;

import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.packet.info.ServerStatusPacket;
import io.github.kgriff0n.packet.info.ServersInfoPacket;
import io.github.kgriff0n.socket.Gateway;
import io.github.kgriff0n.socket.SubServer;
import io.github.kgriff0n.api.ServersLinkApi;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.world.ServerWorld;

public class ServerTick implements ServerTickEvents.StartWorldTick {

    private static int count = 0;

    @Override
    public void onStartTick(ServerWorld serverWorld) {
        count++;
        if (count >= 600) { // every 30s
            count = 0;
            float tps = serverWorld.getServer().getTickManager().getTickRate();
            /* update self */
            ServersLink.getServerInfo().setTps(tps);
            if (ServersLink.isGateway) {
                Gateway.getInstance().sendAll(new ServersInfoPacket(ServersLinkApi.getServerList()));
            } else {
                SubServer.getInstance().send(new ServerStatusPacket(ServersLink.getServerInfo().getName(), tps, false));
            }
        }
    }
}
