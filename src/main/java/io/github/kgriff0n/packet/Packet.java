package io.github.kgriff0n.packet;

import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.api.ServersLinkApi;
import io.github.kgriff0n.server.Settings;
import io.github.kgriff0n.socket.Gateway;

import java.io.Serializable;

public interface Packet extends Serializable {

    /**
     * Must be implemented by all the packets.
     * Will be executed when the packet is received.
     */
    void onReceive();

    default void onGatewayReceive(String sender) {
        Gateway.getInstance().forward(this, sender);
        if (shouldReceive(Gateway.getInstance().getSettings(ServersLink.getServerInfo().getGroupId(), ServersLinkApi.getServer(sender).getGroupId()))) {
            onReceive();
        }
    }

    /**
     * Determines whether a server should
     * receive the packet, based on settings
     * @return true if the packet should be received,
     *         false otherwise
     */
    default boolean shouldReceive(Settings settings) {
        return true;
    }

}
