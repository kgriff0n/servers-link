package io.github.kgriff0n.packet.server;

import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.api.ServersLinkApi;
import io.github.kgriff0n.packet.Packet;

import java.util.UUID;

public class PreventDisconnectPacket implements Packet {

    private final UUID uuid;

    public PreventDisconnectPacket(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public void onReceive() {
        ServersLinkApi.getPreventDisconnect().add(uuid);
    }
}
