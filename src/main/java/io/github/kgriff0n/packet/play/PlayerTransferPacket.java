package io.github.kgriff0n.packet.play;

import io.github.kgriff0n.PlayersInformation;
import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.packet.server.PreventConnectPacket;
import io.github.kgriff0n.packet.server.PreventDisconnectPacket;
import io.github.kgriff0n.server.Settings;
import io.github.kgriff0n.socket.Gateway;
import io.github.kgriff0n.socket.SubServer;
import io.github.kgriff0n.api.ServersLinkApi;

import java.util.UUID;

public class PlayerTransferPacket implements Packet {

    private final UUID uuid;

    private final String serverToTransfer;

    public PlayerTransferPacket(UUID uuid, String serverToTransfer) {
        this.uuid = uuid;
        this.serverToTransfer = serverToTransfer;
    }

    @Override
    public boolean shouldReceive(Settings settings) {
        return false;
    }

    @Override
    public void onReceive() {
        if (!ServersLink.isGateway) {
            /* Sub-server receive the packet, add player to the waiting list to allow to connection */
            SubServer.getInstance().addWaitingPlayer(this.uuid);
        }
    }

    @Override
    public void onGatewayReceive(String sender) {
        /* The player is sent to the hub, remove from the player list to allowed it to connect */
        /* Add player to transferred list, to block the join message */
        Gateway gateway = Gateway.getInstance();
        Settings settings = gateway.getSettings(ServersLinkApi.getServer(sender).getGroupId(), ServersLinkApi.getServer(serverToTransfer).getGroupId());
        if (this.serverToTransfer.equals(ServersLink.getServerInfo().getName())) {
            if (settings.isPlayerListSynced()) {
                ServersLinkApi.getPreventConnect().add(uuid);
                gateway.sendTo(new PreventDisconnectPacket(uuid), sender);
            }
        } else { /* Redirect the packet to the other server */
            gateway.sendTo(this, this.serverToTransfer);
            if (settings.isPlayerListSynced()) { // prevents messages if both servers have synchronized players
                gateway.sendTo(new PreventConnectPacket(uuid), serverToTransfer);
                gateway.sendTo(new PreventDisconnectPacket(uuid), sender);
            }
        }
        gateway.removePlayer(this.uuid);
        /* Save last server */
        PlayersInformation.setLastServer(uuid, serverToTransfer);
    }
}
