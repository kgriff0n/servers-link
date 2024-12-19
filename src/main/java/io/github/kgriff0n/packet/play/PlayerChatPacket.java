package io.github.kgriff0n.packet.play;

import io.github.kgriff0n.packet.Packet;
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
    public boolean shouldTransfer() {
        return true;
    }

    @Override
    public void onReceive() {
        /* Send message */
        ServerPlayerEntity player = SERVER.getPlayerManager().getPlayer(receiver);
        if (player != null) {
            player.sendMessage(Text.Serialization.fromJson(serializedMessage, SERVER.getRegistryManager()));
        }
    }
}
