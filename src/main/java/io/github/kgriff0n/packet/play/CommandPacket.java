package io.github.kgriff0n.packet.play;

import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.api.ServersLinkApi;
import io.github.kgriff0n.server.Settings;
import net.fabricmc.loader.api.FabricLoader;
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
    public boolean shouldReceive(Settings settings) {
        return command.startsWith("server run ")
                || settings.isWhitelistSynced() && command.startsWith("whitelist")
                || settings.isRolesSynced() &&
                    (command.startsWith("op") || command.startsWith("deop")
                    || (FabricLoader.getInstance().isModLoaded("player-roles")
                        && command.startsWith("role")));
    }

    @Override
    public void onReceive() {
        String cmd;
        if (command.startsWith("server run ")) {
            cmd = command.substring(11);
        } else {
            cmd = command;
        }
        ServerCommandSource source;

        ServerPlayerEntity player = null;
        if (uuid != null) {
            player = ServersLinkApi.getDummyPlayer(uuid);
        }

        if (player != null) {
            source = new ServerCommandSource(
                    player.getCommandOutput(),
                    player.getPos(),
                    player.getRotationClient(),
                    player.getWorld() instanceof ServerWorld ? (ServerWorld)player.getWorld() : null,
                    SERVER.getPermissionLevel(player.getGameProfile()),
                    "do-not-send-back",
                    player.getDisplayName(),
                    player.getWorld().getServer(),
                    player
            );
        } else {
            source = new ServerCommandSource(
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
        }
        SERVER.getCommandManager().executeWithPrefix(source, cmd);
    }
}
