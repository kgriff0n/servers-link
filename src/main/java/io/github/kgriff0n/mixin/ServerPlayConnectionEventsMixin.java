package io.github.kgriff0n.mixin;

import io.github.kgriff0n.util.DummyPlayer;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayConnectionEvents.class)
public class ServerPlayConnectionEventsMixin {

    @Inject(at = @At("HEAD"), method = "lambda$static$0", cancellable = true)
    private static void cancelInitEvent(ServerPlayConnectionEvents.Init[] callbacks, ServerPlayNetworkHandler handler, MinecraftServer server, CallbackInfo ci) {
        if (handler.player instanceof DummyPlayer) {
            ci.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "lambda$static$2", cancellable = true)
    private static void cancelConnectEvent(ServerPlayConnectionEvents.Join[] callbacks, ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server, CallbackInfo ci) {
        if (handler.player instanceof DummyPlayer) {
            ci.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "lambda$static$4", cancellable = true)
    private static void cancelDisconnectEvent(ServerPlayConnectionEvents.Disconnect[] callbacks, ServerPlayNetworkHandler handler, MinecraftServer server, CallbackInfo ci) {
        if (handler.player instanceof DummyPlayer) {
            ci.cancel();
        }
    }
}
