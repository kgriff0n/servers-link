package io.github.kgriff0n.packet.info;

import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.api.ServersLinkApi;
import io.github.kgriff0n.server.ServerInfo;

public class ServerStatusPacket implements Packet {

    private final String serverName;
    private final float tps;
    private final boolean down;

    public ServerStatusPacket(String serverName, float tps, boolean down) {
        this.serverName = serverName;
        this.tps = tps;
        this.down = down;
    }

    @Override
    public void onReceive() {
        ServerInfo serverInfo = ServersLinkApi.getServer(serverName);
        if (serverInfo != null) {
            serverInfo.setTps(tps);
            serverInfo.setDown(down);
        }
    }
}
