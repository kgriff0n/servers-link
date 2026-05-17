package io.github.kgriff0n.mixin;

import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.api.ServersLinkApi;
import io.github.kgriff0n.util.PositionOverride;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.PlayerSaveHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

@Mixin(PlayerSaveHandler.class)
public class PlayerSaveHandlerMixin {

    @Inject(
            method = "loadPlayerData(Lnet/minecraft/server/PlayerConfigEntry;Ljava/lang/String;)Ljava/util/Optional;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void modifyPlayerData(PlayerConfigEntry playerConfigEntry, String extension, CallbackInfoReturnable<Optional<NbtCompound>> cir) {
        Optional<NbtCompound> optional = cir.getReturnValue();

        optional.ifPresent(nbt -> {
            Path path = ServersLink.SERVER
                    .getSavePath(WorldSavePath.ROOT)
                    .resolve("sl-playerdata")
                    .resolve(playerConfigEntry.id().toString() + ".dat");

            if (Files.exists(path)) {
                try (InputStream is = Files.newInputStream(path)) {
                    NbtCompound savedNbt = NbtIo.readCompressed(is, NbtSizeTracker.ofUnlimitedBytes());
                    for (String key : ServersLinkApi.getPlayerDataKeys()) {
                        NbtElement element = savedNbt.get(key);
                        if (element != null) {
                            nbt.put(key, element);
                        } else {
                            nbt.remove(key);
                        }
                    }
                } catch (IOException e) {
                    ServersLink.LOGGER.error("Unable to load servers-link playerdata for {} {}", playerConfigEntry.id(), e);
                }
            } else {
                for (String key : ServersLinkApi.getPlayerDataKeys()) {
                    nbt.remove(key);
                }
            }

            /* Position override */
            UUID playerUUID = playerConfigEntry.id();
            if (ServersLinkApi.shouldOverridePosition(playerUUID)) {
                PositionOverride override = ServersLinkApi.getPositionOverride(playerUUID);
                nbt.put("Pos", Vec3d.CODEC, override.position());
                nbt.put("Rotation", Vec2f.CODEC, override.rotation());
                nbt.putString("Dimension", override.world());
            }
        });

        cir.setReturnValue(optional);
    }
}
