package io.github.kgriff0n.packet.play;

import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.api.ServersLinkApi;
import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.packet.PacketHeader;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

public class TeleportationResponsePacket extends PacketHeader implements Packet {

    private final UUID requesterUuid;
    private final double targetX;
    private final double targetY;
    private final double targetZ;
    private final float yaw;
    private final float pitch;
    private final String world;


    public TeleportationResponsePacket(UUID requesterUuid, double targetX, double targetY, double targetZ, float yaw, float pitch, String world, String sender, String recipient) {
        super(sender, recipient);
        this.requesterUuid = requesterUuid;
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
        this.yaw = yaw;
        this.pitch = pitch;
        this.world = world;
    }

    @Override
    public void onReceive() {
        ServerPlayerEntity player = ServersLink.SERVER.getPlayerManager().getPlayer(requesterUuid);
        if (player != null) {
            PlayerTransferRequestPacket transferPacket = new PlayerTransferRequestPacket(player.getUuid(), recipient, sender, targetX, targetY, targetZ, yaw, pitch, world);
            ServersLinkApi.send(transferPacket);
        }
    }
}
