package io.github.kgriff0n.packet.info;

import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.server.ServerInfo;
import io.github.kgriff0n.api.ServersLinkApi;

import java.util.ArrayList;

/**
 * Only send from hub to other sub-servers
 * Used to let sub-servers know each other
 */
public class ServersInfoPacket implements Packet {

    private final ArrayList<ServerInfo> servers;

    public ServersInfoPacket(ArrayList<ServerInfo> serversInfo) {
        this.servers = serversInfo;
    }

    @Override
    public void onReceive() {
        ServersLinkApi.setServerList(servers);
    }
}
