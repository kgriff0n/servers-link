package io.github.kgriff0n.packet.play;

import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.api.ServersLinkApi;
import io.github.kgriff0n.server.Settings;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.permission.PermissionPredicate;
import net.minecraft.server.PlayerConfigEntry;
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
    private final boolean isRun;

    public CommandPacket(UUID uuid, String command, boolean isRun) {
        this.uuid = uuid;
        this.command = command;
        this.isRun = isRun;
    }

    @Override
    public boolean shouldReceive(Settings settings) {
        return isRun
                || settings.isWhitelistSynced() && command.startsWith("whitelist")
                || settings.isRolesSynced() &&
                    (command.startsWith("op") || command.startsWith("deop")
                    || (FabricLoader.getInstance().isModLoaded("player-roles")
                        && command.startsWith("role")));
    }

    @Override
    public void onReceive() {
        ServerCommandSource source;

        ServerPlayerEntity player = null;
        if (uuid != null) {
            player = ServersLinkApi.getDummyPlayer(uuid);
        }

        if (player != null) {
            source = new ServerCommandSource(
                    player.getCommandOutput(),
                    player.getEntityPos(),
                    player.getRotationClient(),
                    player.getEntityWorld() instanceof ServerWorld ? player.getEntityWorld() : null,
                    SERVER.getPermissionLevel(new PlayerConfigEntry(player.getUuid(), player.getName().getString())),
                    "do-not-send-back",
                    player.getDisplayName(),
                    player.getEntityWorld().getServer(),
                    player
            );
        } else {
            source = new ServerCommandSource(
                    SERVER,
                    SERVER.getOverworld() == null ? Vec3d.ZERO : Vec3d.of(SERVER.getOverworld().getSpawnPoint().getPos()),
                    Vec2f.ZERO,
                    SERVER.getOverworld(),
                    PermissionPredicate.ALL,
                    "do-not-send-back",
                    Text.literal("Server"),
                    SERVER,
                    null
            );
        }
        SERVER.getCommandManager().parseAndExecute(source, command);
    }
}
