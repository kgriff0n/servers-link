package io.github.kgriff0n.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.packet.play.TeleportationRequestPacket;
import io.github.kgriff0n.packet.play.TeleportationResponsePacket;
import io.github.kgriff0n.socket.Gateway;
import io.github.kgriff0n.socket.SubServer;
import io.github.kgriff0n.util.DummyPlayer;
import io.github.kgriff0n.api.ServersLinkApi;
import io.github.kgriff0n.server.ServerInfo;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.entity.Relative;
import java.util.EnumSet;
import java.util.Locale;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class ServerCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("server")
                .then(literal("list")
                        .requires(Permissions.require("server.list", PermissionLevel.GAMEMASTERS))
                        .executes(context -> list(context.getSource()))
                )
                .then(literal("join")
                        .requires(Permissions.require("server.join", PermissionLevel.GAMEMASTERS))
                        .then(argument("server", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    for (String serverName : ServersLinkApi.getServerNames()) {
                                        builder.suggest(serverName);
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(context -> join(context.getSource().getPlayer(), StringArgumentType.getString(context, "server")))
                                .then(argument("player", EntityArgument.player())
                                        .requires(Permissions.require("server.join.other", PermissionLevel.GAMEMASTERS))
                                        .executes(context -> join(EntityArgument.getPlayer(context, "player"), StringArgumentType.getString(context, "server")))
                                )
                        )

                )
                .then(literal("whereis")
                        .requires(Permissions.require("server.whereis", PermissionLevel.GAMEMASTERS))
                        .then(argument("player", EntityArgument.player())
                                .executes(context -> whereis(context.getSource(), EntityArgument.getPlayer(context, "player")))
                        )
                )
                .then(literal("tpto")
                        .requires(Permissions.require("server.tpto", PermissionLevel.GAMEMASTERS))
                        .then(argument("player", EntityArgument.player())
                                .executes(context -> teleportTo(context.getSource(), EntityArgument.getPlayer(context, "player")))
                        )
                )
                .then(literal("tphere")
                        .requires(Permissions.require("server.tphere", PermissionLevel.GAMEMASTERS))
                        .then(argument("player", EntityArgument.player())
                                .executes(context -> teleportHere(context.getSource(), EntityArgument.getPlayer(context, "player")))
                        )
                )
                .then(literal("dummyplayerlist")
                        .requires(Permissions.require("server.dummyplayerlist", PermissionLevel.GAMEMASTERS))
                        .executes(context -> dummyPlayerList(context.getSource()))
                )
                .then(Commands.literal("run")
                        .requires(Permissions.require("server.run", PermissionLevel.GAMEMASTERS))
                        .redirect(dispatcher.getRoot()))
        ));
    }

    private static int list(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player != null) {
            player.sendSystemMessage(Component.literal("Server List").withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_GRAY));

            for (ServerInfo server : ServersLinkApi.getServerList()) {
                MutableComponent status = Component.literal("●");
                if (server.isDown()) {
                    status.withStyle(ChatFormatting.RED);
                } else {
                    status.withStyle(ChatFormatting.GREEN);
                }

                MutableComponent players = Component.literal(String.valueOf(server.getPlayersList().size())).withStyle(ChatFormatting.WHITE);

                MutableComponent tps = Component.literal(String.format(Locale.ENGLISH, "%.1f", server.getTps()));
                if (server.getTps() > 15) {
                    tps.withStyle(ChatFormatting.GREEN);
                } else if (server.getTps() > 10) {
                    tps.withStyle(ChatFormatting.YELLOW);
                } else if (server.getTps() > 0) {
                    tps.withStyle(ChatFormatting.RED);
                } else {
                    tps.withStyle(ChatFormatting.DARK_RED);
                }
                player.sendSystemMessage(
                        Component.literal("[").append(status).append("] " + server.getName())
                                .append(" | ").append(players).append(" player(s)")
                                .append(" (").append(tps).append(" TPS)")
                                .withStyle(ChatFormatting.GRAY));
            }
        } else {
            for (ServerInfo server : ServersLinkApi.getServerList()) {
                ServersLink.LOGGER.info("{} | {} | {} TPS | {} players", server.getName(), server.isDown() ? "Closed" : "Running", server.getTps(), server.getPlayersList().size());
            }
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int join(ServerPlayer player, String serverName) {
        if (player != null) {
            /* Save player pos */
            String name = ServersLink.getServerInfo().getName();

            if (name.equals(serverName)) {
                player.sendSystemMessage(Component.literal("You are already connected to this server").withStyle(ChatFormatting.RED));
            } else if (ServersLinkApi.getServer(serverName) == null) {
                player.sendSystemMessage(Component.literal("This server does not exist").withStyle(ChatFormatting.RED));
            } else {
                ServersLinkApi.transferPlayer(player, ServersLink.getServerInfo().getName(), serverName);
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int whereis(CommandSourceStack source, ServerPlayer player) {
        ServerPlayer sender = source.getPlayer();
        if (sender != null) {
            sender.sendSystemMessage(Component.literal(player.getName().getString() + " is on " + ServersLinkApi.whereIs(player.getUUID())));
        } else {
            ServersLink.LOGGER.info("{} is on {}", player.getName().getString(), ServersLinkApi.whereIs(player.getUUID()));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int teleportTo(CommandSourceStack source, ServerPlayer player) {
        ServerPlayer sender = source.getPlayer();
        String server = ServersLinkApi.whereIs(player.getUUID());
        if (sender == null) return 0;
        if (server.equals(ServersLink.getServerInfo().getName())) {
            sender.teleportTo(player.level(), player.getX(), player.getY(), player.getZ(), EnumSet.noneOf(Relative.class), player.getYRot(), player.getXRot(), false);
        } else {
            TeleportationRequestPacket request = new TeleportationRequestPacket(sender.getUUID(), player.getUUID(), ServersLink.getServerInfo().getName(), server);
            if (ServersLink.isGateway) {
                Gateway.getInstance().sendTo(server, request);
            } else {
                SubServer.getInstance().send(request);
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int teleportHere(CommandSourceStack source, ServerPlayer player) {
        ServerPlayer sender = source.getPlayer();
        String server = ServersLinkApi.whereIs(player.getUUID());
        if (sender == null) return 0;
        if (server.equals(ServersLink.getServerInfo().getName())) {
            player.teleportTo(sender.level(), sender.getX(), sender.getY(), sender.getZ(), EnumSet.noneOf(Relative.class), sender.getYRot(), sender.getXRot(), false);
        } else {
            TeleportationResponsePacket accept = new TeleportationResponsePacket(player.getUUID(), sender.getX(), sender.getY(), sender.getZ(), sender.getYRot(), sender.getXRot(), sender.level().dimension().identifier().toString(), ServersLink.getServerInfo().getName(), server);
            if (ServersLink.isGateway) {
                Gateway.getInstance().sendTo(server, accept);
            } else {
                SubServer.getInstance().send(accept);
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int dummyPlayerList(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        for (DummyPlayer dummy : ServersLinkApi.getDummyPlayers()) {
            if (player == null) {
                ServersLink.LOGGER.info(dummy.getScoreboardName());
            } else {
                player.sendSystemMessage(dummy.getName());
            }
        }

        return Command.SINGLE_SUCCESS;
    }
}