package io.github.kgriff0n.packet.server;

import io.github.kgriff0n.packet.Packet;

import static io.github.kgriff0n.ServersLink.IS_RUNNING;
import static io.github.kgriff0n.ServersLink.SERVER;

public class ServerStopPacket implements Packet {
    @Override
    public void onReceive() {
        SERVER.stop(false); // onReceive is always executed on server thread
        IS_RUNNING = false;
    }
}
