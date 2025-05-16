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
//        ServersLinkApi.getPreventConnect().add(this.uuid);
        if (!ServersLink.isGateway) {
            /* Sub-server receive the packet, add player to the waiting list to allow to connection */
            SubServer.getInstance().addWaitingPlayer(this.uuid);
        }
    }

    @Override
    public void onGatewayReceive(String sender) {
//        Packet.super.onGatewayReceive(sender);
        /* The player is sent to the hub, remove from the player list to allowed it to connect */
        /* Add player to transferred list, to block the join message */
        Gateway gateway = Gateway.getInstance();
        if (this.serverToTransfer.equals(ServersLink.getServerInfo().getName())) {
            gateway.removePlayer(this.uuid);
        } else { /* Redirect the packet to the other server, add the player to the player list of this server */
            gateway.sendTo(this, this.serverToTransfer);
            Settings settings = gateway.getSettings(ServersLinkApi.getServer(sender).getGroupId(), ServersLinkApi.getServer(serverToTransfer).getGroupId());
            if (settings.isPlayerListSynced()) { // prevents messages if both servers have synchronized players
                gateway.sendTo(new PreventConnectPacket(uuid), serverToTransfer);
                gateway.sendTo(new PreventDisconnectPacket(uuid), sender);
            }
        }
        /* Save last server */
        PlayersInformation.setLastServer(uuid, serverToTransfer);
    }
}
