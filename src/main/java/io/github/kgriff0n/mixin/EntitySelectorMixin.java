package io.github.kgriff0n.mixin;

import io.github.kgriff0n.api.ServersLinkApi;
import net.minecraft.command.EntitySelector;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mixin(EntitySelector.class)
public class EntitySelectorMixin {

    @Redirect(method = "getPlayers", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;getPlayer(Ljava/util/UUID;)Lnet/minecraft/server/network/ServerPlayerEntity;"))
    private ServerPlayerEntity getDummyPlayer(PlayerManager playerManager, UUID uuid) {
        ServerPlayerEntity player = playerManager.getPlayer(uuid);
        if (player == null) { // check for dummy player
            player = ServersLinkApi.getDummyPlayer(uuid);
        }
        return player;
    }

    @Redirect(method = "getPlayers", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;getPlayer(Ljava/lang/String;)Lnet/minecraft/server/network/ServerPlayerEntity;"))
    private ServerPlayerEntity getDummyPlayer(PlayerManager playerManager, String name) {
        ServerPlayerEntity player = playerManager.getPlayer(name);
        if (player == null) { // check for dummy player
            player = ServersLinkApi.getDummyPlayer(name);
        }
        return player;
    }

    @Redirect(method = "getPlayers", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;getPlayerList()Ljava/util/List;"))
    private List<ServerPlayerEntity> getPlayerList(PlayerManager playerManager) {
        List<ServerPlayerEntity> allPlayers = new ArrayList<>();
        allPlayers.addAll(playerManager.getPlayerList());
        allPlayers.addAll(ServersLinkApi.getDummyPlayers());
        return allPlayers;
    }

}
