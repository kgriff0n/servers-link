package io.github.kgriff0n.mixin;

import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.api.ServersLinkApi;
import io.github.kgriff0n.util.PositionOverride;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.PlayerDataStorage;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
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

@Mixin(PlayerDataStorage.class)
public class PlayerSaveHandlerMixin {

    @Inject(
            method = "load(Lnet/minecraft/server/players/NameAndId;Ljava/lang/String;)Ljava/util/Optional;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void modifyPlayerData(NameAndId playerConfigEntry, String extension, CallbackInfoReturnable<Optional<CompoundTag>> cir) {
        Optional<CompoundTag> optional = cir.getReturnValue();

        optional.ifPresent(nbt -> {
            Path path = ServersLink.SERVER
                    .getWorldPath(LevelResource.ROOT)
                    .resolve("sl-playerdata")
                    .resolve(playerConfigEntry.id() + ".dat");

            if (Files.exists(path)) {
                try (InputStream is = Files.newInputStream(path)) {
                    CompoundTag savedNbt = NbtIo.readCompressed(is, NbtAccounter.unlimitedHeap());
                    for (String key : ServersLinkApi.getPlayerDataKeys()) {
                        Tag element = savedNbt.get(key);
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
                nbt.store("Pos", Vec3.CODEC, override.position());
                nbt.store("Rotation", Vec2.CODEC, override.rotation());
                nbt.putString("Dimension", override.world());
            }
        });

        cir.setReturnValue(optional);
    }
}
