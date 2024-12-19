package io.github.kgriff0n.packet.play;

import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.util.ServersLinkUtil;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

import static io.github.kgriff0n.ServersLink.SERVER;

public class CommandPacket implements Packet {

    private final UUID uuid;
    private final String command;

    public CommandPacket(UUID uuid, String command) {
        this.uuid = uuid;
        this.command = command;
    }

    @Override
    public boolean shouldTransfer() {
        return true;
    }

    @Override
    public void onReceive() {
        if (uuid != null) {
            ServerPlayerEntity player = ServersLinkUtil.getDummyPlayer(uuid);
            if (player != null) {
                ServerCommandSource source = new ServerCommandSource(
                        player,
                        player.getPos(),
                        player.getRotationClient(),
                        player.getWorld() instanceof ServerWorld ? (ServerWorld)player.getWorld() : null,
                        SERVER.getPermissionLevel(player.getGameProfile()),
                        "do-not-send-back",
                        player.getDisplayName(),
                        player.getWorld().getServer(),
                        player
                );
                SERVER.getCommandManager().executeWithPrefix(source, command);
            }
        } else {
            ServerCommandSource source = new ServerCommandSource(
                    SERVER,
                    SERVER.getOverworld() == null ? Vec3d.ZERO : Vec3d.of(SERVER.getOverworld().getSpawnPos()),
                    Vec2f.ZERO,
                    SERVER.getOverworld(),
                    4,
                    "do-not-send-back",
                    Text.literal("Server"),
                    SERVER,
                    null
            );
            SERVER.getCommandManager().executeWithPrefix(source, command);
        }
    }
}
