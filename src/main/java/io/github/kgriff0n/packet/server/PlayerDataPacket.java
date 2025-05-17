package io.github.kgriff0n.packet.server;

import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.mixin.PlayerManagerAccessor;
import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.server.Settings;
import io.github.kgriff0n.util.PlayerData;
import net.minecraft.server.PlayerManager;

import java.io.IOException;
import java.util.UUID;

import static io.github.kgriff0n.ServersLink.SERVER;

public class PlayerDataPacket implements Packet {

    private final UUID uuid;

    private final byte[] data;
    private final byte[] advancements;
    private final byte[] stats;

    public PlayerDataPacket(UUID uuid) throws IOException {
        this.uuid = uuid;
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
    public boolean shouldReceive(Settings settings) {
        return settings.isPlayerDataSynced();
    }

    @Override
    public void onReceive() {
        /* Remove player data to reload them from file */
        PlayerManager playerManager = SERVER.getPlayerManager();
        ((PlayerManagerAccessor) playerManager).getAdvancementTrackers().remove(this.uuid);
        ((PlayerManagerAccessor) playerManager).getStatisticsMap().remove(this.uuid);
        writeData();
    }
}
