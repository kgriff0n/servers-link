package io.github.kgriff0n.packet.info;

import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.packet.server.UpdateRolesPacket;
import io.github.kgriff0n.packet.server.UpdateWhitelistPacket;
import io.github.kgriff0n.socket.Gateway;
import io.github.kgriff0n.server.ServerInfo;
import io.github.kgriff0n.api.ServersLinkApi;

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
    public void onReceive() {}

    @Override
    public void onGatewayReceive(String sender) {
        Gateway gateway = Gateway.getInstance();
        try {
            gateway.sendTo(new UpdateWhitelistPacket(), this.server.getName());
            gateway.sendTo(new UpdateRolesPacket(), this.server.getName());
        } catch (IOException e) {
            ServersLink.LOGGER.error("Unable to send data to {}", this.server.getName());
        }
        Gateway.getInstance().sendAll(new ServersInfoPacket(ServersLinkApi.getServerList()));
    }
}
