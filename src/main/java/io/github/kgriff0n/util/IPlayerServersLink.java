package io.github.kgriff0n.util;

import net.minecraft.util.math.Vec3d;

public interface IPlayerServersLink {

    void servers_link$setServerPos(String name, Vec3d pos);
    Vec3d servers_link$getServerPos(String name);
    void servers_link$removeServerPos(String name);

}
