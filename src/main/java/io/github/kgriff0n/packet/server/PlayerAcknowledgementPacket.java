package io.github.kgriff0n.packet.server;

import io.github.kgriff0n.Config;
import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.packet.info.ServersInfoPacket;
import io.github.kgriff0n.socket.Hub;
import io.github.kgriff0n.util.ServersLinkUtil;

import java.util.UUID;

public class PlayerAcknowledgementPacket implements Packet {

    private final String serverName;
    private final UUID uuid;
    private final String name;

    public PlayerAcknowledgementPacket(String serverName, UUID uuid, String name) {
        this.serverName = serverName;
        this.uuid = uuid;
        this.name = name;
    }

    @Override
    public void onReceive() {
        if (Config.isHub) {
            ServersLinkUtil.getServer(serverName).addPlayer(this.uuid, this.name);
            Hub.getInstance().sendAll(new ServersInfoPacket(ServersLinkUtil.getServerList()));
        }
    }
}
