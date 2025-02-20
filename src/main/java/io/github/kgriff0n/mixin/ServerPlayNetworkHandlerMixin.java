package io.github.kgriff0n.mixin;

import io.github.kgriff0n.Config;
import io.github.kgriff0n.packet.play.PlayerChatPacket;
import io.github.kgriff0n.util.ServersLinkUtil;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static io.github.kgriff0n.ServersLink.SERVER;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {

    @Shadow public abstract ServerPlayerEntity getPlayer();

    @Shadow public ServerPlayerEntity player;

    @Inject(at = @At("HEAD"), method = "sendChatMessage")
    private void sendChatMessage(SignedMessage message, MessageType.Parameters params, CallbackInfo ci) {
        if (Config.syncChat) {
            Text formattedMessage = params.applyChatDecoration(message.getContent());
            PlayerChatPacket packet = new PlayerChatPacket(Text.Serialization.toJsonString(formattedMessage, SERVER.getRegistryManager()), this.getPlayer().getName().getString());
            ServersLinkUtil.send(packet);
        }
    }

    @Redirect(method = "cleanUp", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;broadcast(Lnet/minecraft/text/Text;Z)V"))
    private void preventDisconnectMessage(PlayerManager instance, Text message, boolean overlay) {
        if (ServersLinkUtil.getPreventDisconnect().contains(player.getUuid())) {
            ServersLinkUtil.getPreventDisconnect().remove(player.getUuid());
        } else {
            getPlayer().getServer().getPlayerManager().broadcast(message, overlay);
        }
    }
}
