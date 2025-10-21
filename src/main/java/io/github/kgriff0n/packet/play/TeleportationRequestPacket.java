package io.github.kgriff0n.packet.play;

import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.socket.Gateway;
import io.github.kgriff0n.socket.SubServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

public class TeleportationRequestPacket implements Packet {

    private final UUID targetUuid;
    private final UUID senderUuid;

    private final String originServer;
    private final String destinationServer;

    public TeleportationRequestPacket(UUID targetUuid, UUID senderUuid, String originServer, String destinationServer) {
        this.targetUuid = targetUuid;
        this.senderUuid = senderUuid;

        this.originServer = originServer;
        this.destinationServer = destinationServer;
    }

    @Override
    public void onReceive() {
        ServerPlayerEntity player = ServersLink.SERVER.getPlayerManager().getPlayer(targetUuid);
        Vec3d pos = player != null ? player.getEntityPos() : null;

        if (ServersLink.isGateway) { //FIXME
            if (this.destinationServer.equals(ServersLink.getServerInfo().getName())) {
                /* Execute packet from hub */
                if (pos != null) {
                    Gateway.getInstance().sendTo(new TeleportationAcceptPacket(pos.getX(), pos.getY(), pos.getZ(), this.senderUuid, this.originServer, this.destinationServer), this.originServer);
                }
            } else {
                /* Redirect the packet to the other server */
                Gateway.getInstance().sendTo(this, this.destinationServer);
            }
        } else {
            /* Sub-server receive the packet */
            if (pos != null) {
                SubServer.getInstance().send(new TeleportationAcceptPacket(pos.getX(), pos.getY(), pos.getZ(), this.senderUuid, this.originServer, this.destinationServer));
            }
        }
    }
}
