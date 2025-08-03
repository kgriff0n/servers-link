package io.github.kgriff0n.event;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class PlayerInit implements ServerPlayConnectionEvents.Init {
    @Override
    public void onPlayInit(ServerPlayNetworkHandler serverPlayNetworkHandler, MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (player.isRemoved() || player.getName().equals(serverPlayNetworkHandler.player.getName())) {
                player.networkHandler.disconnect(Text.literal("Duplicate login"));
            }
        }
    }
}
