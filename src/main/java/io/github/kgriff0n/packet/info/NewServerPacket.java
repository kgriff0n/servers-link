package io.github.kgriff0n.packet.info;

import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.packet.server.UpdateRolesPacket;
import io.github.kgriff0n.packet.server.UpdateWhitelistPacket;
import io.github.kgriff0n.socket.Hub;
import io.github.kgriff0n.util.ServerInfo;
import io.github.kgriff0n.util.ServersLinkUtil;

import java.io.IOException;

public class NewServerPacket implements Packet {

    private final ServerInfo server;

    public NewServerPacket(ServerInfo server) {
        this.server = server;
    }

    public ServerInfo getServer() {
        return this.server;
    }

    @Override
    public void onReceive() {
        Hub hub = Hub.getInstance();
        try {
            hub.sendTo(new UpdateWhitelistPacket(), this.server.getName());
            hub.sendTo(new UpdateRolesPacket(), this.server.getName());
        } catch (IOException e) {
            ServersLink.LOGGER.error("Unable to send data to {}", this.server.getName());
        }
        Hub.getInstance().sendAll(new ServersInfoPacket(ServersLinkUtil.getServerList()));
    }
}
