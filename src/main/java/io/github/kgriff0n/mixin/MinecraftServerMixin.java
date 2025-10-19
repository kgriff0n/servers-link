package io.github.kgriff0n.mixin;

import com.mojang.authlib.GameProfile;
import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.api.ServersLinkApi;
import io.github.kgriff0n.server.ServerInfo;
import io.github.kgriff0n.socket.Gateway;
import net.minecraft.network.QueryableServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.ServerMetadata;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin implements QueryableServer {

    @Inject(at = @At("HEAD"), method = "createMetadataPlayers", cancellable = true)
    private void customPlayerCount(CallbackInfoReturnable<ServerMetadata.Players> cir) {
        if (ServersLink.isGateway && Gateway.getInstance().isGlobalPlayerCountEnabled()) {
            int maxPlayers = getMaxPlayerCount();
            int playerCount = 0;
            List<GameProfile> players = new ArrayList<>();
            List<PlayerConfigEntry> playerConfigEntries = new ArrayList<>();
            for (ServerInfo server : ServersLinkApi.getServerList()) {
                playerCount += server.getPlayersList().size();
                for (GameProfile player : players) {
                    playerConfigEntries.add(new PlayerConfigEntry(player.id(), player.name()));
                }

            }
            cir.setReturnValue(new ServerMetadata.Players(maxPlayers, playerCount, playerConfigEntries));
        }
    }

    @Inject(at = @At("HEAD"), method = "getCurrentPlayerCount", cancellable = true)
    private void getCurrentPlayerCount(CallbackInfoReturnable<Integer> cir) {
        if (ServersLink.isGateway && Gateway.getInstance().isGlobalPlayerCountEnabled()) {
            int playerCount = 0;
            for (ServerInfo server : ServersLinkApi.getServerList()) {
                playerCount += server.getPlayersList().size();
            }
            cir.setReturnValue(playerCount);
        }
    }

}
