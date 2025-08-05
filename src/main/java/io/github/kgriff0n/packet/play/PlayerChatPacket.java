package io.github.kgriff0n.packet.play;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.server.Settings;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.TextCodecs;

import static io.github.kgriff0n.ServersLink.SERVER;

public class PlayerChatPacket implements Packet {

    private final String serializedMessage;
    private final String receiver;

    public PlayerChatPacket(String serializedMessage, String receiver) {
        this.serializedMessage = serializedMessage;
        this.receiver = receiver;
    }

    @Override
    public boolean shouldReceive(Settings settings) {
        return settings.isChatSynced();
    }

    @Override
    public void onReceive() {
        /* Send message */
        ServerPlayerEntity player = SERVER.getPlayerManager().getPlayer(receiver);
        if (player != null) {
            player.sendMessage(TextCodecs.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(serializedMessage)).getOrThrow());
        }
    }
}
