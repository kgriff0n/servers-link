package io.github.kgriff0n.packet.play;

import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.server.Settings;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static io.github.kgriff0n.ServersLink.SERVER;

public class PlayerChatPacket implements Packet {

    private final String serializedMessage;
    private final String receiver;

    public PlayerChatPacket(String serializedMessage, String receiver) {
        this.serializedMessage = serializedMessage;
        this.receiver = receiver;
    }

    @Override
    public boolean shouldTransfer(Settings settings) {
        return settings.isChatSynced();
    }

    @Override
    public void onReceive(String sender) {
        /* Send message */
        ServerPlayerEntity player = SERVER.getPlayerManager().getPlayer(receiver);
        if (player != null) {
            player.sendMessage(Text.Serialization.fromJson(serializedMessage, SERVER.getRegistryManager()));
        }
    }
}
