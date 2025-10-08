package io.github.kgriff0n.util;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.apache.logging.log4j.core.jmx.Server;

import java.util.List;

public interface IPlayerServersLink {

    void servers_link$setServerPos(String name, Vec3d pos);
    Vec3d servers_link$getServerPos(String name);
    void servers_link$removeServerPos(String name);

    void servers_link$setServerRot(String name, float yaw, float pitch);
    List<Float> servers_link$getServerRot(String name);
    void servers_link$removeServerRot(String name);

    void servers_link$setServerDim(String name, ServerWorld dim);
    ServerWorld servers_link$getServerDim(String name);
    void servers_link$removeServerDim(String name);

}
