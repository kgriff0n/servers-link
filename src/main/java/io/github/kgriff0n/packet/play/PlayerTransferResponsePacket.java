package io.github.kgriff0n.packet.play;

import io.github.kgriff0n.PlayersInformation;
import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.api.ServersLinkApi;
import io.github.kgriff0n.event.ServerTick;
import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.packet.PacketHeader;
import io.github.kgriff0n.server.ServerInfo;
import io.github.kgriff0n.server.Settings;
import io.github.kgriff0n.socket.Gateway;
import net.minecraft.network.packet.s2c.common.ServerTransferS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

public class PlayerTransferResponsePacket extends PacketHeader implements Packet {

    private static final byte PREVENT_LEAVE_MESSAGE = 1 << 0;
    private byte flags;

    private final UUID uuid;

    protected PlayerTransferResponsePacket(UUID uuid, String sender, String recipient) {
        super(sender, recipient);
        this.uuid = uuid;
    }

    @Override
    public void onReceive() {
        ServerInfo server = ServersLinkApi.getServer(sender);
        ServerPlayerEntity player = ServersLink.SERVER.getPlayerManager().getPlayer(uuid);
        if ((flags & PREVENT_LEAVE_MESSAGE) != 0) {
            ServersLinkApi.getPreventDisconnect().add(uuid);
        }
        if (server != null && player != null) {
            player.networkHandler.sendPacket(new ServerTransferS2CPacket(server.getIp(), server.getPort()));
            ServerTick.scheduleDisconnect(player.getUuid(), 20);
        }
    }

    @Override
    public void gatewayLogic() {
        PlayersInformation.setLastServer(uuid, sender);
        Gateway gateway = Gateway.getInstance();
        Settings settings = gateway.getSettings(ServersLinkApi.getServer(sender).getGroupId(), ServersLinkApi.getServer(recipient).getGroupId());
        if (settings.isPlayerListSynced()) {
            flags |= PREVENT_LEAVE_MESSAGE;
        }
    }

}
