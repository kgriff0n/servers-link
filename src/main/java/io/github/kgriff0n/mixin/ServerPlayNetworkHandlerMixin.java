package io.github.kgriff0n.mixin;

import com.mojang.serialization.JsonOps;
import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.packet.play.PlayerChatPacket;
import io.github.kgriff0n.api.ServersLinkApi;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {

    @Shadow public abstract ServerPlayerEntity getPlayer();

    @Shadow public ServerPlayerEntity player;

    @Inject(at = @At("HEAD"), method = "sendChatMessage")
    private void sendChatMessage(SignedMessage message, MessageType.Parameters params, CallbackInfo ci) {
        Text formattedMessage = params.applyChatDecoration(message.getContent());
        PlayerChatPacket packet = new PlayerChatPacket(TextCodecs.CODEC.encodeStart(JsonOps.INSTANCE, formattedMessage).getOrThrow().toString(), this.getPlayer().getName().getString());
        ServersLinkApi.send(packet, ServersLink.getServerInfo().getName());
    }

    @Redirect(method = "cleanUp", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;broadcast(Lnet/minecraft/text/Text;Z)V"))
    private void preventDisconnectMessage(PlayerManager instance, Text message, boolean overlay) {
        if (ServersLinkApi.getPreventDisconnect().contains(player.getUuid())) {
            ServersLinkApi.getPreventDisconnect().remove(player.getUuid());
        } else {
            getPlayer().getServer().getPlayerManager().broadcast(message, overlay);
        }
    }
}
