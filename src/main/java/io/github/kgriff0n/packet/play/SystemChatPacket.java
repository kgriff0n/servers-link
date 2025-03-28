package io.github.kgriff0n.packet.play;

import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.server.Settings;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static io.github.kgriff0n.ServersLink.SERVER;

public class SystemChatPacket implements Packet {

    private final String serializedMessage;

    public SystemChatPacket(String serializedMessage) {
        this.serializedMessage = serializedMessage;
    }

    @Override
    public boolean shouldReceive(Settings settings) {
        return settings.isChatSynced();
    }

    @Override
    public void onReceive() {
        /* Send message */
        for (ServerPlayerEntity player : SERVER.getPlayerManager().getPlayerList()) {
            player.sendMessage(Text.Serialization.fromJson(serializedMessage, SERVER.getRegistryManager()));
        }
    }
}
