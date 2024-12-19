package io.github.kgriff0n.util;

import net.minecraft.util.math.Vec3d;

public interface IPlayerServersLink {

    String servers_link$getLastServer();
    void servers_link$setLastServer(String name);

    String servers_link$getNextServer();
    void servers_link$setNextServer(String name);

    void servers_link$setServerPos(String name, Vec3d pos);
    Vec3d servers_link$getServerPos(String name);

    void servers_link$removeServerPos(String name);

}
