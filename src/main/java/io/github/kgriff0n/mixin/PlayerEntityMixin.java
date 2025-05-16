package io.github.kgriff0n.mixin;

import io.github.kgriff0n.util.IPlayerServersLink;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin implements IPlayerServersLink {

    @Unique
    private HashMap<String, Vec3d> serversPos = new HashMap<>();

    @Inject(at = @At("HEAD"), method = "writeCustomDataToNbt")
    private void writeNbt(NbtCompound nbt, CallbackInfo ci) {
        NbtCompound serversLink = new NbtCompound();
        NbtCompound nbtServersPos = new NbtCompound();
        serversPos.forEach((name, pos) -> {
            NbtList position = new NbtList();
            position.add(NbtDouble.of(pos.getX()));
            position.add(NbtDouble.of(pos.getY()));
            position.add(NbtDouble.of(pos.getZ()));
            nbtServersPos.put(name, position);
        });
        serversLink.put("Pos", nbtServersPos);
        nbt.put("ServersLink", serversLink);
    }

    @Inject(at = @At("HEAD"), method = "readCustomDataFromNbt")
    private void readNbt(NbtCompound nbt, CallbackInfo ci) {
        NbtCompound serversLink = nbt.getCompound("ServersLink");
        this.serversPos = new HashMap<>();
        NbtCompound nbtServersPos = serversLink.getCompound("Pos");
        for (String server : nbtServersPos.getKeys()) {
            NbtList position = nbtServersPos.getList(server, NbtElement.DOUBLE_TYPE);
            Vec3d pos = new Vec3d(position.getDouble(0), position.getDouble(1), position.getDouble(2));
            serversPos.put(server, pos);
        }
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
