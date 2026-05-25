package io.github.kgriff0n.packet.play;

import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.api.ServersLinkApi;
import io.github.kgriff0n.server.Settings;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
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
        CommandSourceStack source;

        ServerPlayer player = null;
        if (uuid != null) {
            player = ServersLinkApi.getDummyPlayer(uuid);
        }

        if (player != null) {
            source = new CommandSourceStack(
                    player.commandSource(),
                    player.position(),
                    player.getRotationVector(),
                    player.level() instanceof ServerLevel ? player.level() : null,
                    SERVER.getProfilePermissions(new NameAndId(player.getUUID(), player.getName().getString())),
                    "do-not-send-back",
                    player.getDisplayName(),
                    player.level().getServer(),
                    player
            );
        } else {
            source = new CommandSourceStack(
                    SERVER,
                    SERVER.overworld() == null ? Vec3.ZERO : Vec3.atLowerCornerOf(SERVER.overworld().getRespawnData().pos()),
                    Vec2.ZERO,
                    SERVER.overworld(),
                    PermissionSet.ALL_PERMISSIONS,
                    "do-not-send-back",
                    Component.literal("Server"),
                    SERVER,
                    null
            );
        }
        SERVER.getCommands().performPrefixedCommand(source, cmd);
    }
}
