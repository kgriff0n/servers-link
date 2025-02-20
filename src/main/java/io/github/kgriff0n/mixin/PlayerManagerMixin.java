package io.github.kgriff0n.mixin;

import io.github.kgriff0n.Config;
import io.github.kgriff0n.packet.play.SystemChatPacket;
import io.github.kgriff0n.util.DummyPlayer;
import io.github.kgriff0n.util.ServersLinkUtil;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static io.github.kgriff0n.ServersLink.SERVER;

@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {

    @Shadow public abstract void broadcast(Text message, boolean overlay);

    @Unique
    private ServerPlayerEntity player;

    @Inject(at = @At("HEAD"), method = "broadcast(Lnet/minecraft/text/Text;Z)V")
    private void sendSystemPacket(Text message, boolean overlay, CallbackInfo ci) {
        if (Config.syncChat) {
            SystemChatPacket packet = new SystemChatPacket(Text.Serialization.toJsonString(message, SERVER.getRegistryManager()));
            ServersLinkUtil.send(packet);
        }
    }

    @Inject(at = @At("HEAD"), method = "onPlayerConnect")
    private void getPlayer(ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData, CallbackInfo ci) {
        this.player = player;
    }

    @Redirect(method = "onPlayerConnect", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;broadcast(Lnet/minecraft/text/Text;Z)V"))
    private void preventConnectMessage(PlayerManager instance, Text message, boolean overlay) {
        if (ServersLinkUtil.getPreventConnect().contains(player.getUuid())) {
            ServersLinkUtil.getPreventConnect().remove(player.getUuid());
        } else {
            this.broadcast(message, overlay);
        }
    }

    @Inject(at = @At("HEAD"), method = "savePlayerData", cancellable = true)
    private void savePlayerDataThreadSafe(ServerPlayerEntity player, CallbackInfo ci) {
        if (player instanceof DummyPlayer) {
            ci.cancel();
        }
    }
}
