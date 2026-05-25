package io.github.kgriff0n.mixin;

import io.github.kgriff0n.api.ServersLinkApi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

@Mixin(EntitySelector.class)
public class EntitySelectorMixin {

    @Redirect(method = "findPlayers", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;getPlayer(Ljava/util/UUID;)Lnet/minecraft/server/level/ServerPlayer;"))
    private ServerPlayer getDummyPlayer(PlayerList playerManager, UUID uuid) {
        ServerPlayer player = playerManager.getPlayer(uuid);
        if (player == null) { // check for dummy player
            player = ServersLinkApi.getDummyPlayer(uuid);
        }
        return player;
    }

    @Redirect(method = "findPlayers", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;getPlayerByName(Ljava/lang/String;)Lnet/minecraft/server/level/ServerPlayer;"))
    private ServerPlayer getDummyPlayer(PlayerList playerManager, String name) {
        ServerPlayer player = playerManager.getPlayerByName(name);
        if (player == null) { // check for dummy player
            player = ServersLinkApi.getDummyPlayer(name);
        }
        return player;
    }

    @Redirect(method = "findPlayers", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;getPlayers()Ljava/util/List;"))
    private List<ServerPlayer> getPlayerList(PlayerList playerManager) {
        List<ServerPlayer> allPlayers = new ArrayList<>();
        allPlayers.addAll(playerManager.getPlayers());
        allPlayers.addAll(ServersLinkApi.getDummyPlayers());
        return allPlayers;
    }

}
