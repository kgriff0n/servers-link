package io.github.kgriff0n.event;

import io.github.kgriff0n.Config;
import io.github.kgriff0n.packet.info.ServerStatusPacket;
import io.github.kgriff0n.packet.info.ServersInfoPacket;
import io.github.kgriff0n.socket.Hub;
import io.github.kgriff0n.socket.SubServer;
import io.github.kgriff0n.util.ServersLinkUtil;
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
            ServersLinkUtil.getServer(Config.serverName).setTps(tps);
            if (Config.isHub) {
                Hub.getInstance().sendAll(new ServersInfoPacket(ServersLinkUtil.getServerList()));
            } else {
                SubServer.getInstance().send(new ServerStatusPacket(Config.serverName, tps, false));
            }
        }
    }
}
