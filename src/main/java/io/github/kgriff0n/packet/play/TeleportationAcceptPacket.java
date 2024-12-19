package io.github.kgriff0n.packet.play;

import io.github.kgriff0n.Config;
import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.socket.Hub;
import io.github.kgriff0n.util.IPlayerServersLink;
import io.github.kgriff0n.util.ServersLinkUtil;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

import static io.github.kgriff0n.Config.isHub;

public class TeleportationAcceptPacket implements Packet {

    private final double targetX;
    private final double targetY;
    private final double targetZ;
    private final UUID senderUuid;

    private final String originServer;
    private final String destinationServer;

    public TeleportationAcceptPacket(double targetX, double targetY, double targetZ, UUID senderUuid, String originServer, String destinationServer) {
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
        this.senderUuid = senderUuid;

        this.originServer = originServer;
        this.destinationServer = destinationServer;
    }

    @Override
    public void onReceive() {
        ServerPlayerEntity player = ServersLink.SERVER.getPlayerManager().getPlayer(senderUuid);
        if (isHub) {
            if (this.originServer.equals(Config.serverName)) {
                /* Execute packet from hub */
                if (player != null) {
                    ((IPlayerServersLink) player).servers_link$setLastServer(Config.serverName);
                    ((IPlayerServersLink) player).servers_link$setServerPos(this.destinationServer, new Vec3d(targetX, targetY, targetZ));
                    ServersLinkUtil.transferPlayer(player, this.destinationServer, Config.syncPlayerData);
                }
            } else {
                /* Redirect the packet to the other server */
                Hub.getInstance().sendTo(this, this.destinationServer);
            }
        } else {
            /* Sub-server receive the packet */
            if (player != null) {
                ((IPlayerServersLink) player).servers_link$setLastServer(Config.serverName);
                ((IPlayerServersLink) player).servers_link$setServerPos(this.destinationServer, new Vec3d(targetX, targetY, targetZ));
                ServersLinkUtil.transferPlayer(player, this.destinationServer, Config.syncPlayerData);
            }
        }
    }
}
