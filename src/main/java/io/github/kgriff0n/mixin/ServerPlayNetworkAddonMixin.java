package io.github.kgriff0n.mixin;

import io.github.kgriff0n.util.DummyPlayer;
import net.fabricmc.fabric.impl.networking.server.ServerPlayNetworkAddon;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkAddon.class)
public class ServerPlayNetworkAddonMixin {
    @Shadow @Final private ServerPlayNetworkHandler handler;

    @Inject(at=@At("HEAD"), method = "invokeInitEvent", cancellable = true)
    private void cancelInitEvent(CallbackInfo ci) {
        if (handler.player instanceof DummyPlayer) {
            ci.cancel();
        }
    }

    @Inject(at=@At("HEAD"), method = "onClientReady", cancellable = true)
    private void cancelConnectEvent(CallbackInfo ci) {
        if (handler.player instanceof DummyPlayer) {
            ci.cancel();
        }
    }

    @Inject(at=@At("HEAD"), method = "invokeDisconnectEvent", cancellable = true)
    private void cancelDisconnectEvent(CallbackInfo ci) {
        if (handler.player instanceof DummyPlayer) {
            ci.cancel();
        }
    }
}
