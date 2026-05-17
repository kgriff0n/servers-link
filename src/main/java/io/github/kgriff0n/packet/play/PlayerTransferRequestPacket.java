package io.github.kgriff0n.packet.play;

import io.github.kgriff0n.api.ServersLinkApi;
import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.packet.PacketHeader;
import io.github.kgriff0n.server.Settings;
import io.github.kgriff0n.socket.Gateway;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

public class PlayerTransferRequestPacket extends PacketHeader implements Packet {

    private static final byte OVERRIDE_POSITION = 1 << 0;
    private static final byte PREVENT_JOIN_MESSAGE = 1 << 1;
    private byte flags;

    private final UUID uuid;
    private double targetX;
    private double targetY;
    private double targetZ;
    private float yaw;
    private float pitch;
    private String world;

    public PlayerTransferRequestPacket(UUID uuid, String sender, String recipient) {
        super(sender, recipient);
        this.uuid = uuid;
    }

    public PlayerTransferRequestPacket(UUID uuid, String sender, String recipient, double targetX, double targetY, double targetZ, float yaw, float pitch, String world) {
        this(uuid, sender, recipient);
        this.flags |= OVERRIDE_POSITION;
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
        this.yaw = yaw;
        this.pitch = pitch;
        this.world = world;
    }

    @Override
    public void onReceive() {
        ServersLinkApi.addWaitingPlayer(uuid);
        ServersLinkApi.send(new PlayerTransferResponsePacket(uuid, ServersLinkApi.getServerName(), sender));
        if ((flags & OVERRIDE_POSITION) != 0) {
            ServersLinkApi.addPositionOverride(uuid, new Vec3d(targetX, targetY, targetZ), new Vec2f(yaw, pitch), world);
        }
        if ((flags & PREVENT_JOIN_MESSAGE) != 0) {
            ServersLinkApi.getPreventConnect().add(uuid);
        }
    }

    @Override
    public void gatewayLogic() {
        Gateway gateway = Gateway.getInstance();
        Settings settings = gateway.getSettings(ServersLinkApi.getServer(sender).getGroupId(), ServersLinkApi.getServer(recipient).getGroupId());
        if (settings.isPlayerListSynced()) {
            flags |= PREVENT_JOIN_MESSAGE;
        }
    }
}
