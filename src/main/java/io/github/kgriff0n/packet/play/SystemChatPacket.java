package io.github.kgriff0n.packet.play;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.server.Settings;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;

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
        for (ServerPlayer player : SERVER.getPlayerList().getPlayers()) {
            player.sendSystemMessage(ComponentSerialization.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, SERVER.registryAccess()), JsonParser.parseString(serializedMessage)).getOrThrow());
        }
    }
}
