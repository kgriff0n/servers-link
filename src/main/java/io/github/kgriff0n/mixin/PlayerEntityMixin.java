package io.github.kgriff0n.mixin;

import com.mojang.serialization.Codec;
import io.github.kgriff0n.util.IPlayerServersLink;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin implements IPlayerServersLink {

    @Unique
    private HashMap<String, Vec3d> serversPos = new HashMap<>();

    @Unique
    private HashMap<String, List<Float>> serversRot = new HashMap<>();

    @Unique
    private HashMap<String, ServerWorld> serversDim = new HashMap<>();

    @Inject(at = @At("HEAD"), method = "writeCustomData")
    private void writeNbt(WriteView view, CallbackInfo ci) {
        WriteView serversLink = view.get("ServersLink");
        WriteView posView = serversLink.get("Position");
        WriteView rotView = serversLink.get("Rotation");
        WriteView dimView = serversLink.get("Dimension");

        for (Map.Entry<String, Vec3d> entry : serversPos.entrySet()) {
            String name = entry.getKey();
            Vec3d pos = entry.getValue();

            WriteView.ListAppender<Double> posAppender = posView.getListAppender(name, Codec.DOUBLE);
            posAppender.add(pos.getX());
            posAppender.add(pos.getY());
            posAppender.add(pos.getZ());
        }

        for (Map.Entry<String, List<Float>> entry : serversRot.entrySet()) {
            String name = entry.getKey();
            List<Float> rot = entry.getValue();

            WriteView.ListAppender<Float> rotAppender = rotView.getListAppender(name, Codec.FLOAT);
            rotAppender.add(rot.get(0));
            rotAppender.add(rot.get(1));
        }

        for (Map.Entry<String, ServerWorld> entry : serversDim.entrySet()) {
            String name = entry.getKey();
            ServerWorld dim = entry.getValue();

            dimView.putString(name, dim.getRegistryKey().getValue().toString().split(":")[1]);
        }
    }

    @Inject(at = @At("HEAD"), method = "readCustomData")
    private void readNbt(ReadView view, CallbackInfo ci) {
        Codec<Map<String, List<Double>>> posMapCodec =
                Codec.unboundedMap(Codec.STRING, Codec.list(Codec.DOUBLE));
        Codec<Map<String, String>> dimMapCodec =
                Codec.unboundedMap(Codec.STRING, Codec.STRING);
        Codec<Map<String, List<Float>>> rotMapCodec =
                Codec.unboundedMap(Codec.STRING, Codec.list(Codec.FLOAT));


        view.getOptionalReadView("ServersLink")
            .flatMap(v -> v.read("Position", posMapCodec))
            .ifPresent(posMap -> {
                this.serversPos = new HashMap<>();
                posMap.forEach((server, coords) -> {
                    if (coords.size() >= 3) {
                        serversPos.put(server, new Vec3d(coords.get(0), coords.get(1), coords.get(2)));
                    }
                });
            });

        view.getOptionalReadView("ServersLink")
                .flatMap(v -> v.read("Rotation", rotMapCodec))
                .ifPresent(rotMap -> {
                    this.serversRot = new HashMap<>();
                    rotMap.forEach((server, rotations) -> {
                        if (rotations.size() >= 2) {
                            serversRot.put(server, List.of(rotations.get(0), rotations.get(1)));
                        }
                    });
                });

        view.getOptionalReadView("ServersLink")
            .flatMap(v -> v.read("Dimension", dimMapCodec))
                .ifPresent(dimMap -> {
                    this.serversDim = new HashMap<>();
                    dimMap.forEach((server, dimId) -> {
                        RegistryKey<World> key = RegistryKey.of(RegistryKeys.WORLD, Identifier.ofVanilla(dimId));
                        World world = Objects.requireNonNull(((PlayerEntity) (Object) this).getServer()).getWorld(key);
                        if (world instanceof ServerWorld serverWorld) {
                            serversDim.put(server, serverWorld);
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

    @Override
    public void servers_link$setServerRot(String name, float yaw, float pitch) {
        List<Float> rot = List.of(yaw, pitch);
        this.serversRot.put(name, rot);
    }

    @Override
    public List<Float> servers_link$getServerRot(String name) {
        return this.serversRot.get(name);
    }

    @Override
    public void servers_link$removeServerRot(String name) {
        this.serversRot.remove(name);
    }

    @Override
    public void servers_link$setServerDim(String name, ServerWorld dim) {
        this.serversDim.put(name, dim);
    }

    @Override
    public ServerWorld servers_link$getServerDim(String name) {
        return this.serversDim.get(name);
    }

    @Override
    public void servers_link$removeServerDim(String name) {
        this.serversDim.remove(name);
    }
}
