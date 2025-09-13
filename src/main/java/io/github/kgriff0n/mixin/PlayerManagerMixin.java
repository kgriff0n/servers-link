package io.github.kgriff0n.mixin;

import com.mojang.serialization.JsonOps;
import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.packet.play.SystemChatPacket;
import io.github.kgriff0n.packet.server.PlayerDataPacket;
import io.github.kgriff0n.util.DummyPlayer;
import io.github.kgriff0n.api.ServersLinkApi;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SentMessage;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.registry.RegistryOps;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static io.github.kgriff0n.ServersLink.SERVER;

@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {

    @Shadow public abstract void broadcast(Text message, boolean overlay);

    @Shadow public abstract void sendToAll(Packet<?> packet);

    @Shadow @Final private List<ServerPlayerEntity> players;

    @Unique
    private ServerPlayerEntity player;

    @Inject(at = @At("HEAD"), method = "broadcast(Lnet/minecraft/text/Text;Z)V")
    private void sendSystemPacket(Text message, boolean overlay, CallbackInfo ci) {
        SystemChatPacket packet = new SystemChatPacket(TextCodecs.CODEC.encodeStart(RegistryOps.of(JsonOps.INSTANCE, SERVER.getRegistryManager()), message).getOrThrow().toString());
        ServersLinkApi.send(packet, ServersLink.getServerInfo().getName());
    }

    @Inject(at = @At("HEAD"), method = "onPlayerConnect")
    private void getPlayer(ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData, CallbackInfo ci) {
        this.player = player;
    }

    @Inject(at = @At("TAIL"), method = "savePlayerData")
    private void sendPlayerData(ServerPlayerEntity player, CallbackInfo ci) {
        try {
            ServersLinkApi.send(new PlayerDataPacket(player.getUuid()), ServersLink.getServerInfo().getName());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Redirect(method = "onPlayerConnect", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;broadcast(Lnet/minecraft/text/Text;Z)V"))
    private void preventConnectMessage(PlayerManager instance, Text message, boolean overlay) {
        if (ServersLinkApi.getPreventConnect().contains(player.getUuid())) {
            ServersLinkApi.getPreventConnect().remove(player.getUuid());
        } else {
            this.broadcast(message, overlay);
        }
    }

    @Redirect(method = "onPlayerConnect", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;sendToAll(Lnet/minecraft/network/packet/Packet;)V"))
    private void sendPlayerList(PlayerManager instance, Packet<?> packet) {
        List<ServerPlayerEntity> allPlayers = new ArrayList<>();
        allPlayers.addAll(players);
        allPlayers.addAll(ServersLinkApi.getDummyPlayers());
        this.sendToAll(PlayerListS2CPacket.entryFromPlayer(allPlayers));
    }

    @Inject(at = @At("HEAD"), method = "savePlayerData", cancellable = true)
    private void savePlayerDataThreadSafe(ServerPlayerEntity player, CallbackInfo ci) {
        if (player instanceof DummyPlayer) {
            ci.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "broadcast(Lnet/minecraft/network/message/SignedMessage;Ljava/util/function/Predicate;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/network/message/MessageType$Parameters;)V")
    private void broadcastDummy(SignedMessage message, Predicate<ServerPlayerEntity> shouldSendFiltered, @Nullable ServerPlayerEntity sender, MessageType.Parameters params, CallbackInfo ci) {
        SentMessage sentMessage = SentMessage.of(message);
        for (ServerPlayerEntity serverPlayerEntity : ServersLinkApi.getDummyPlayers()) {
            boolean bl3 = shouldSendFiltered.test(serverPlayerEntity);
            serverPlayerEntity.sendChatMessage(sentMessage, bl3, params);
        }
    }
}
