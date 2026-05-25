package io.github.kgriff0n.packet.play;

import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.packet.info.ServersInfoPacket;
import io.github.kgriff0n.server.Settings;
import io.github.kgriff0n.socket.Gateway;
import io.github.kgriff0n.api.ServersLinkApi;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.server.level.ServerPlayer;

import static io.github.kgriff0n.ServersLink.SERVER;

public class PlayerDisconnectPacket implements Packet {

    private final UUID uuid;

    public PlayerDisconnectPacket(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public boolean shouldReceive(Settings settings) {
        return settings.isPlayerListSynced();
    }

    @Override
    public void onReceive() {
        List<ServerPlayer> playerList = SERVER.getPlayerList().getPlayers();
        /* Delete the fake player */
        ServersLinkApi.getDummyPlayers().removeIf(player -> player.getUUID().equals(uuid));

        /* Update player list for all players */
        for (ServerPlayer player : playerList) {
            player.connection.send(new ClientboundPlayerInfoRemovePacket(List.of(uuid)));
        }
    }

    @Override
    public void gatewayLogic() {
        Gateway.getInstance().removePlayer(uuid);
        Gateway.getInstance().sendToAll(new ServersInfoPacket(ServersLinkApi.getServerList()));
    }
}
