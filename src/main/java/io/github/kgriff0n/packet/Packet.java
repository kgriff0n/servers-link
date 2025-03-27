package io.github.kgriff0n.packet;

import io.github.kgriff0n.server.Settings;

import java.io.Serializable;

public interface Packet extends Serializable {

    /**
     * Must be implemented by all the packets.
     * Will be executed when the packet is received.
     */
    void onReceive(String sender);

    /**
     * Determines whether the hub should transfer
     * the packet to other servers, false by default
     * @return true if the packet is to be transferred,
     *         false otherwise
     */
    default boolean shouldTransfer(Settings settings) {
        return false;
    }

}
