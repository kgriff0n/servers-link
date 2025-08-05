package io.github.kgriff0n.mixin;

import com.mojang.serialization.Codec;
import io.github.kgriff0n.util.IPlayerServersLink;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin implements IPlayerServersLink {

    @Unique
    private HashMap<String, Vec3d> serversPos = new HashMap<>();

    @Inject(at = @At("HEAD"), method = "writeCustomData")
    private void writeNbt(WriteView view, CallbackInfo ci) {
        WriteView serversLink = view.get("ServersLink");
        WriteView posView = serversLink.get("Pos");

        for (Map.Entry<String, Vec3d> entry : serversPos.entrySet()) {
            String name = entry.getKey();
            Vec3d pos = entry.getValue();

            WriteView.ListAppender<Double> appender = posView.getListAppender(name, Codec.DOUBLE);
            appender.add(pos.getX());
            appender.add(pos.getY());
            appender.add(pos.getZ());
        }
    }

    @Inject(at = @At("HEAD"), method = "readCustomData")
    private void readNbt(ReadView view, CallbackInfo ci) {
        Codec<Map<String, List<Double>>> mapCodec =
                Codec.unboundedMap(Codec.STRING, Codec.list(Codec.DOUBLE));

        view.getOptionalReadView("ServersLink")
            .flatMap(v -> v.read("Pos", mapCodec))
            .ifPresent(posMap -> {
                this.serversPos = new HashMap<>();
                posMap.forEach((server, coords) -> {
                    if (coords.size() >= 3) {
                        serversPos.put(server, new Vec3d(coords.get(0), coords.get(1), coords.get(2)));
                    }
                });
            });
    }

    @Override
    public void servers_link$setServerPos(String name, Vec3d pos) {
        this.serversPos.put(name, pos);
    }

    @Override
    public Vec3d servers_link$getServerPos(String name) {
        return this.serversPos.get(name);
    }

    @Override
    public void servers_link$removeServerPos(String name) {
        this.serversPos.remove(name);
    }
}
