package io.github.kgriff0n.packet.play;

import io.github.kgriff0n.Config;
import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.socket.Hub;
import io.github.kgriff0n.socket.SubServer;
import io.github.kgriff0n.util.ServersLinkUtil;

import java.util.UUID;

import static io.github.kgriff0n.Config.isHub;

public class PlayerTransferPacket implements Packet {

    private final UUID uuid;

    private final String serverToTransfer;

    public PlayerTransferPacket(UUID uuid) {
        this.uuid = uuid;

        this.serverToTransfer = null;
    }

    public PlayerTransferPacket(UUID uuid, String serverToTransfer) {
        this.uuid = uuid;

        this.serverToTransfer = serverToTransfer;
    }

    @Override
    public void onReceive() {
        ServersLinkUtil.getPreventConnect().add(this.uuid);
        if (isHub) {
            /* The player is sent to the hub, remove from the player list to allowed it to connect */
            /* Add player to transferred list, to block the join message */
            if (this.serverToTransfer == null || this.serverToTransfer.equals(Config.serverName)) {
                Hub.getInstance().removePlayer(this.uuid);
            } else { /* Redirect the packet to the other server, add the player to the player list of this server */
                Hub.getInstance().sendTo(this, this.serverToTransfer);
            }
        } else {
            /* Sub-server receive the packet, add player to the waiting list to allow to connection */
            SubServer.getInstance().addWaitingPlayer(this.uuid);
        }
    }
}
