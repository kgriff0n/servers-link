package io.github.kgriff0n.mixin;

import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.api.ServersLinkApi;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueOutput;
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

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerEntityMixin {

    @Shadow
    @Final
    private MinecraftServer server;


    @Inject(at = @At("TAIL"), method = "addAdditionalSaveData")
    private void writeCustomData(ValueOutput view, CallbackInfo ci) {
        if (view instanceof TagValueOutput originalNbtWriteView) {
            ServerPlayer player = (ServerPlayer) (Object) this;

            Path path = server
                    .getWorldPath(LevelResource.ROOT)
                    .resolve("sl-playerdata")
                    .resolve(player.getStringUUID() + ".dat");

            CompoundTag originalNbt = originalNbtWriteView.buildResult();
            CompoundTag newNbt = new CompoundTag();
            for (String key : ServersLinkApi.getPlayerDataKeys()) {
                Tag element = originalNbt.get(key);
                if (element != null) newNbt.put(key, element);
            }

            CompoundTag nbtCompound = newNbt.copy();
            CompletableFuture.runAsync(() -> {
                try {
                    Files.createDirectories(path.getParent());
                    NbtIo.writeCompressed(nbtCompound, path);
                } catch (IOException e) {
                    ServersLink.LOGGER.warn("Unable to save servers-link playerdata for {} {}", player.getStringUUID(), e);
                }
            });
        }
    }
}
