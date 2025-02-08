package io.github.kgriff0n.mixin.compat;

import eu.pb4.polymer.core.impl.networking.PacketPatcher;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.network.ServerCommonNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.nucleoid.packettweaker.PacketContext;

@Mixin(PacketPatcher.class)
public class PacketPatcherMixin {
    @Inject(at = @At("HEAD"), method = "prevent", cancellable = true)
    private static void cancel(ServerCommonNetworkHandler handler, Packet<?> packet, CallbackInfoReturnable<Boolean> cir) {
        if (PacketContext.create(handler).getPacketListener() == null) {
            cir.setReturnValue(false);
        }
    }
}
