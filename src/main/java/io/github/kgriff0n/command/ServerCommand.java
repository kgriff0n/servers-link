package io.github.kgriff0n.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.kgriff0n.Config;
import io.github.kgriff0n.packet.play.TeleportationAcceptPacket;
import io.github.kgriff0n.packet.play.TeleportationRequestPacket;
import io.github.kgriff0n.socket.Hub;
import io.github.kgriff0n.socket.SubServer;
import io.github.kgriff0n.util.IPlayerServersLink;
import io.github.kgriff0n.util.ServersLinkUtil;
import io.github.kgriff0n.util.ServerInfo;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.EnumSet;
import java.util.Locale;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ServerCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("server")
                .then(literal("list")
                        .requires(Permissions.require("server.list", 4))
                        .executes(context -> list(context.getSource()))
                )
                .then(literal("join")
                        .requires(Permissions.require("server.join", 4))
                        .then(argument("server", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    for (String serverName : ServersLinkUtil.getServerNames()) {
                                        builder.suggest(serverName);
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(context -> join(context.getSource().getPlayer(), StringArgumentType.getString(context, "server")))
                                .then(argument("player", EntityArgumentType.player())
                                        .requires(Permissions.require("server.join.other", 4))
                                        .executes(context -> join(EntityArgumentType.getPlayer(context, "player"), StringArgumentType.getString(context, "server")))
                                )
                        )

                )
                .then(literal("whereis")
                        .requires(Permissions.require("server.whereis", 4))
                        .then(argument("player", EntityArgumentType.player())
                                .executes(context -> whereis(context.getSource(), EntityArgumentType.getPlayer(context, "player")))
                        )
                )
                .then(literal("tpto")
                        .requires(Permissions.require("server.tpto", 4))
                        .then(argument("player", EntityArgumentType.player())
                                .executes(context -> teleportTo(context.getSource(), EntityArgumentType.getPlayer(context, "player")))
                        )
                )
                .then(literal("tphere")
                        .requires(Permissions.require("server.tphere", 4))
                        .then(argument("player", EntityArgumentType.player())
                                .executes(context -> teleportHere(context.getSource(), EntityArgumentType.getPlayer(context, "player")))
                        )
                )
                .then(CommandManager.literal("run")
                        .requires(Permissions.require("server.run", 4))
                        .redirect(dispatcher.getRoot()))
        ));
    }

    private static int list(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        player.sendMessage(Text.literal("Server List").formatted(Formatting.BOLD, Formatting.DARK_GRAY));

        for (ServerInfo server : ServersLinkUtil.getServerList()) {
            MutableText status = Text.literal("●");
            if (server.isDown()) {
                status.formatted(Formatting.RED);
            } else {
                status.formatted(Formatting.GREEN);
            }

            MutableText players = Text.literal(String.valueOf(server.getPlayersList().size())).formatted(Formatting.WHITE);

            MutableText tps = Text.literal(String.format(Locale.ENGLISH, "%.1f", server.getTps()));
            if (server.getTps() > 15) {
                tps.formatted(Formatting.GREEN);
            } else if (server.getTps() > 10) {
                tps.formatted(Formatting.YELLOW);
            } else if (server.getTps() > 0) {
                tps.formatted(Formatting.RED);
            } else {
                tps.formatted(Formatting.DARK_RED);
            }
            player.sendMessage(
                    Text.literal("[").append(status).append("] " + server.getName())
                            .append(" | ").append(players).append(" player(s)")
                            .append(" (").append(tps).append(" TPS)")
                            .formatted(Formatting.GRAY));
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int join(ServerPlayerEntity player, String serverName) {
        if (player != null) {
            /* Save player pos */
            ((IPlayerServersLink) player).servers_link$setLastServer(Config.serverName);
            ((IPlayerServersLink) player).servers_link$setServerPos(Config.serverName, player.getPos());

            if (Config.serverName.equals(serverName)) {
                player.sendMessage(Text.literal("You are already connected to this server").formatted(Formatting.RED));
            } else if (ServersLinkUtil.getServer(serverName) == null) {
                player.sendMessage(Text.literal("This server does not exist").formatted(Formatting.RED));
            } else {
                ServersLinkUtil.transferPlayer(player, serverName, Config.syncPlayerData);
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int whereis(ServerCommandSource source, ServerPlayerEntity player) {
        ServerPlayerEntity sender = source.getPlayer();
        sender.sendMessage(Text.literal(player.getName().getString() + " is on " + ServersLinkUtil.whereIs(player.getUuid())));
        return Command.SINGLE_SUCCESS;
    }

    private static int teleportTo(ServerCommandSource source, ServerPlayerEntity player) {
        ServerPlayerEntity sender = source.getPlayer();
        String server = ServersLinkUtil.whereIs(player.getUuid());
        if (sender == null) return 0;
        if (server.equals(Config.serverName)) {
            sender.teleport(player.getServerWorld(), player.getX(), player.getY(), player.getZ(), EnumSet.noneOf(PositionFlag.class), player.getYaw(), player.getPitch(), false);
        } else {
            TeleportationRequestPacket request = new TeleportationRequestPacket(player.getUuid(), sender.getUuid(), Config.serverName, server);
            if (Config.isHub) {
                Hub.getInstance().sendTo(request, server);
            } else {
                SubServer.getInstance().send(request);
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int teleportHere(ServerCommandSource source, ServerPlayerEntity player) {
        ServerPlayerEntity sender = source.getPlayer();
        String server = ServersLinkUtil.whereIs(player.getUuid());
        if (sender == null) return 0;
        if (server.equals(Config.serverName)) {
            player.teleport(sender.getServerWorld(), sender.getX(), sender.getY(), sender.getZ(), EnumSet.noneOf(PositionFlag.class), sender.getYaw(), sender.getPitch(), false);
        } else {
            TeleportationAcceptPacket accept = new TeleportationAcceptPacket(sender.getX(), sender.getY(), sender.getZ(), player.getUuid(), server, Config.serverName);
            if (Config.isHub) {
                Hub.getInstance().sendTo(accept, server);
            } else {
                SubServer.getInstance().send(accept);
            }
        }
        return Command.SINGLE_SUCCESS;
    }
}