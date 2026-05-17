package io.github.kgriff0n.mixin;

import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.api.ServersLinkApi;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.storage.NbtWriteView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.WorldSavePath;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {

    @Shadow
    @Final
    private MinecraftServer server;


    @Inject(at = @At("TAIL"), method = "writeCustomData")
    private void writeCustomData(WriteView view, CallbackInfo ci) {
        if (view instanceof NbtWriteView originalNbtWriteView) {
            ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;

            Path path = server
                    .getSavePath(WorldSavePath.ROOT)
                    .resolve("sl-playerdata")
                    .resolve(player.getUuidAsString() + ".dat");

            NbtCompound originalNbt = originalNbtWriteView.getNbt();
            NbtCompound newNbt = new NbtCompound();
            for (String key : ServersLinkApi.getPlayerDataKeys()) {
                NbtElement element = originalNbt.get(key);
                if (element != null) newNbt.put(key, element);
            }

            NbtCompound nbtCompound = newNbt.copy();
            CompletableFuture.runAsync(() -> {
                try {
                    Files.createDirectories(path.getParent());
                    NbtIo.writeCompressed(nbtCompound, path);
                } catch (IOException e) {
                    ServersLink.LOGGER.warn("Unable to save servers-link playerdata for {} {}", player.getUuidAsString(), e);
                }
            });
        }
    }
}
