package io.github.kgriff0n.mixin;

import com.mojang.serialization.JsonOps;
import io.github.kgriff0n.packet.play.PlayerChatPacket;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import io.github.kgriff0n.api.ServersLinkApi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static io.github.kgriff0n.ServersLink.SERVER;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerPlayNetworkHandlerMixin {

    @Shadow public abstract ServerPlayer getPlayer();

    @Shadow public ServerPlayer player;

    @Inject(at = @At("HEAD"), method = "sendPlayerChatMessage")
    private void sendChatMessage(PlayerChatMessage message, ChatType.Bound params, CallbackInfo ci) {
        Component formattedMessage = params.decorate(message.decoratedContent());
        PlayerChatPacket packet = new PlayerChatPacket(ComponentSerialization.CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE, SERVER.registryAccess()), formattedMessage).getOrThrow().toString(), this.getPlayer().getName().getString());
        ServersLinkApi.send(packet);
    }

    @Redirect(method = "removePlayerFromWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    private void preventDisconnectMessage(PlayerList instance, Component message, boolean overlay) {
        if (ServersLinkApi.getPreventDisconnect().contains(player.getUUID())) {
            ServersLinkApi.getPreventDisconnect().remove(player.getUUID());
        } else {
            getPlayer().level().getServer().getPlayerList().broadcastSystemMessage(message, overlay);
        }
    }
}
