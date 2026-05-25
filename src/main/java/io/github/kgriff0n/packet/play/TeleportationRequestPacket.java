package io.github.kgriff0n.packet.play;

import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.api.ServersLinkApi;
import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.packet.PacketHeader;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

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
        ServerPlayer player = ServersLink.SERVER.getPlayerList().getPlayer(targetUuid);
        Vec3 pos = player != null ? player.position() : null;
        if (pos != null) {
            TeleportationResponsePacket packet = new TeleportationResponsePacket(
                    requesterUuid,
                    pos.x(),
                    pos.y(),
                    pos.z(),
                    player.getYRot(),
                    player.getXRot(),
                    player.level().dimension().identifier().toString(),
                    ServersLinkApi.getServerName(),
                    sender
            );
            ServersLinkApi.send(packet);
        }
    }
}
