package io.github.kgriff0n.packet.server;

import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.api.ServersLinkApi;
import io.github.kgriff0n.mixin.PlayerManagerAccessor;
import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.server.Settings;
import io.github.kgriff0n.socket.Gateway;
import io.github.kgriff0n.util.PlayerData;
import net.minecraft.server.PlayerManager;

import java.io.IOException;
import java.util.UUID;

import static io.github.kgriff0n.ServersLink.SERVER;

public class PlayerDataPacket implements Packet {

    private final UUID uuid;
    private final String serverToTransfer;

    private final byte[] data;
    private final byte[] advancements;
    private final byte[] stats;

    public PlayerDataPacket(UUID uuid) throws IOException {
        this.uuid = uuid;
        this.serverToTransfer = null;

        this.data = PlayerData.readData(uuid);
        this.advancements = PlayerData.readAdvancements(uuid);
        this.stats = PlayerData.readStats(uuid);
    }

    public PlayerDataPacket(UUID uuid, String serverToTransfer) throws IOException {
        this.uuid = uuid;
        this.serverToTransfer = serverToTransfer;

        this.data = PlayerData.readData(uuid);
        this.advancements = PlayerData.readAdvancements(uuid);
        this.stats = PlayerData.readStats(uuid);
    }

    private void writeData() {
        SERVER.execute(() -> {
            try {
                PlayerData.writeData(this.uuid, this.data);
                PlayerData.writeAdvancements(this.uuid, this.advancements);
                PlayerData.writeStats(this.uuid, this.stats);
            } catch (IOException e) {
                ServersLink.LOGGER.error("Unable to write player data");
            }
        });
    }

    @Override
    public void onReceive(String sender) {
        /* Remove player data to reload them from file */
        PlayerManager playerManager = SERVER.getPlayerManager();
        ((PlayerManagerAccessor) playerManager).getAdvancementTrackers().remove(this.uuid);
        ((PlayerManagerAccessor) playerManager).getStatisticsMap().remove(this.uuid);

        if (ServersLink.isGateway) {
            Settings settings = Gateway.getInstance().getSettings(ServersLinkApi.getServer(sender).getGroupId(), ServersLinkApi.getServer(serverToTransfer).getGroupId());
            if (this.serverToTransfer != null && !this.serverToTransfer.equals(ServersLink.getServerInfo().getName())) { // Redirect the packet to the other server
                if (settings.isPlayerDataSynced()) {
                    Gateway.getInstance().sendTo(this, this.serverToTransfer);
                }
            } else if (this.serverToTransfer.equals(ServersLink.getServerInfo().getName())) {
                if (settings.isPlayerDataSynced()) {
                    writeData();
                }
            }
        } else {
            writeData();
        }

    }
}
