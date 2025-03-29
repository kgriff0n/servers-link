package io.github.kgriff0n.packet.server;

import io.github.kgriff0n.api.ServersLinkApi;
import io.github.kgriff0n.packet.Packet;

import java.util.UUID;

public class PreventConnectPacket implements Packet {

    private final UUID uuid;

    public PreventConnectPacket(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public void onReceive() {
        ServersLinkApi.getPreventConnect().add(uuid);
    }
}
