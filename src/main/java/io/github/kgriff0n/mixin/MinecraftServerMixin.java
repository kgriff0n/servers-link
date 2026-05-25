package io.github.kgriff0n.mixin;

import com.mojang.authlib.GameProfile;
import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.api.ServersLinkApi;
import io.github.kgriff0n.server.ServerInfo;
import io.github.kgriff0n.socket.Gateway;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin implements net.minecraft.server.ServerInfo {

    @Inject(at = @At("HEAD"), method = "buildPlayerStatus", cancellable = true)
    private void customPlayerCount(CallbackInfoReturnable<ServerStatus.Players> cir) {
        if (ServersLink.isGateway && Gateway.getInstance().isGlobalPlayerCountEnabled()) {
            int maxPlayers = getMaxPlayers();
            int playerCount = 0;
            List<NameAndId> playerConfigEntries = new ArrayList<>();
            for (ServerInfo server : ServersLinkApi.getServerList()) {
                playerCount += server.getPlayersList().size();
                for (GameProfile player : server.getGameProfile()) {
                    playerConfigEntries.add(new NameAndId(player.id(), player.name()));
                }
            }
            cir.setReturnValue(new ServerStatus.Players(maxPlayers, playerCount, playerConfigEntries));
        }
    }

    @Inject(at = @At("HEAD"), method = "getPlayerCount", cancellable = true)
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
