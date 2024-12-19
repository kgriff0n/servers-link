package io.github.kgriff0n.packet.play;

import io.github.kgriff0n.Config;
import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.socket.Hub;
import io.github.kgriff0n.socket.SubServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

import static io.github.kgriff0n.Config.isHub;

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
        Vec3d pos = player != null ? player.getPos() : null;

        if (isHub) {
            if (this.destinationServer.equals(Config.serverName)) {
                /* Execute packet from hub */
                if (pos != null) {
                    Hub.getInstance().sendTo(new TeleportationAcceptPacket(pos.getX(), pos.getY(), pos.getZ(), this.senderUuid, this.originServer, this.destinationServer), this.originServer);
                }
            } else {
                /* Redirect the packet to the other server */
                Hub.getInstance().sendTo(this, this.destinationServer);
            }
        } else {
            /* Sub-server receive the packet */
            if (pos != null) {
                SubServer.getInstance().send(new TeleportationAcceptPacket(pos.getX(), pos.getY(), pos.getZ(), this.senderUuid, this.originServer, this.destinationServer));
            }
        }
    }
}
