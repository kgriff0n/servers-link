package io.github.kgriff0n.packet.play;

import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.api.ServersLinkApi;
import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.packet.PacketHeader;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

public class TeleportationRequestPacket extends PacketHeader implements Packet {

    private final UUID requesterUuid;
    private final UUID targetUuid;

    public TeleportationRequestPacket(UUID requesterUuid, UUID targetUuid, String sender, String recipient) {
        super(sender, recipient);
        this.requesterUuid = requesterUuid;
        this.targetUuid = targetUuid;
    }

    @Override
    public void onReceive() {
        ServerPlayerEntity player = ServersLink.SERVER.getPlayerManager().getPlayer(targetUuid);
        Vec3d pos = player != null ? player.getEntityPos() : null;
        if (pos != null) {
            TeleportationResponsePacket packet = new TeleportationResponsePacket(
                    requesterUuid,
                    pos.getX(),
                    pos.getY(),
                    pos.getZ(),
                    player.getYaw(),
                    player.getPitch(),
                    player.getEntityWorld().getRegistryKey().getValue().toString(),
                    ServersLinkApi.getServerName(),
                    sender
            );
            ServersLinkApi.send(packet);
        }
    }
}
