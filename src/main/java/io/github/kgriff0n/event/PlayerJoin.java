package io.github.kgriff0n.event;

import io.github.kgriff0n.PlayersInformation;
import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.packet.info.NewPlayerPacket;
import io.github.kgriff0n.packet.server.PlayerAcknowledgementPacket;
import io.github.kgriff0n.packet.info.ServersInfoPacket;
import io.github.kgriff0n.socket.Gateway;
import io.github.kgriff0n.socket.SubServer;
import io.github.kgriff0n.util.IPlayerServersLink;
import io.github.kgriff0n.server.ServerInfo;
import io.github.kgriff0n.api.ServersLinkApi;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static io.github.kgriff0n.ServersLink.LOGGER;

public class PlayerJoin implements ServerPlayConnectionEvents.Join, ServerEntityEvents.Load {

    private static ArrayList<ServerPlayerEntity> joinedPlayers = new ArrayList<>();

    @Override
    public void onPlayReady(ServerPlayNetworkHandler serverPlayNetworkHandler, PacketSender packetSender, MinecraftServer minecraftServer) {

        ServerPlayerEntity newPlayer = serverPlayNetworkHandler.player;

        if (!joinedPlayers.contains(newPlayer)) joinedPlayers.add(newPlayer);

        /* Dummy player packet */
        NewPlayerPacket dummyPlayer = new NewPlayerPacket(newPlayer.getGameProfile());

        /* Players can only connect from the hub */
        if (ServersLink.isGateway) {
            Gateway gateway = Gateway.getInstance();
            if (gateway.isConnectedPlayer(newPlayer.getUuid()) && !ServersLinkApi.getPreventConnect().contains(newPlayer.getUuid())) {
                ServersLinkApi.transferPlayer(newPlayer, ServersLink.getServerInfo().getName(), ServersLinkApi.whereIs(newPlayer.getUuid()));
                ServersLinkApi.getPreventConnect().add(newPlayer.getUuid());
                ServersLinkApi.getPreventDisconnect().add(newPlayer.getUuid());
            } else {
                String lastServer = PlayersInformation.getLastServer(newPlayer.getUuid());
                ServerInfo lastServerInfo = ServersLinkApi.getServer(lastServer);
                if (lastServer == null || lastServer.equals(ServersLink.getServerInfo().getName())
                        || lastServerInfo == null || lastServerInfo.isDown() || !gateway.shouldReconnectToLastServer()) {
                    ServersLinkApi.getServer(ServersLink.getServerInfo().getName()).addPlayer(newPlayer.getGameProfile());
                    /* Delete the fake player */
                    ServersLinkApi.getDummyPlayers().removeIf(player -> player.getName().equals(newPlayer.getName()));

                    /* Send player information to other servers */
                    gateway.forward(dummyPlayer, ServersLink.getServerInfo().getName());
                    gateway.sendAll(new ServersInfoPacket(ServersLinkApi.getServerList()));

                    if (gateway.shouldReconnectToLastServer() && lastServer != null && !lastServer.isEmpty() && (lastServerInfo == null || lastServerInfo.isDown())) {
                        newPlayer.sendMessage(Text.literal("An unexpected error occurred while attempting to reconnect you to your previous server").formatted(Formatting.RED));
                    }
                } else {
                    ServersLinkApi.transferPlayer(newPlayer, ServersLink.getServerInfo().getName(), lastServer);
                }
            }
        } else {
            SubServer connection = SubServer.getInstance();
            if (!connection.getWaitingPlayers().contains(newPlayer.getUuid())) {
                serverPlayNetworkHandler.disconnect(Text.translatable("multiplayer.status.cannot_connect").formatted(Formatting.RED));
                /* Used to prevent the logout message in ServerPlayNetworkHandlerMixin#preventDisconnectMessage */
                ServersLinkApi.getPreventConnect().add(serverPlayNetworkHandler.player.getUuid());
                ServersLinkApi.getPreventDisconnect().add(serverPlayNetworkHandler.player.getUuid());
            } else {
                /* The player logs in and is removed from the list of waiting players */
                connection.removeWaitingPlayer(newPlayer.getUuid());
                /* Delete the fake player */
                ServersLinkApi.getDummyPlayers().removeIf(player -> player.getName().equals(newPlayer.getName()));
                /* Send player information to other servers */
                connection.send(dummyPlayer);
                connection.send(new PlayerAcknowledgementPacket(ServersLink.getServerInfo().getName(), newPlayer.getGameProfile()));
            }
        }

    }

    @Override
    public void onLoad(Entity entity, ServerWorld serverWorld) {
        if (!(entity instanceof ServerPlayerEntity newPlayer)){
            return;
        }

        if (!joinedPlayers.contains(newPlayer)) {
            return;
        } else joinedPlayers.remove(newPlayer);

        Vec3d pos = ((IPlayerServersLink) newPlayer).servers_link$getServerPos(ServersLink.getServerInfo().getName());
        ServerWorld dim = ((IPlayerServersLink) newPlayer).servers_link$getServerDim(ServersLink.getServerInfo().getName());
        List<Float> rot = ((IPlayerServersLink) newPlayer).servers_link$getServerRot(ServersLink.getServerInfo().getName());

        if (pos == null || dim == null || rot == null) {
            // Player data not found, probably first join - teleport to world spawn
            pos = new Vec3d(newPlayer.getEntityWorld().getSpawnPoint().getPos().getX() + 0.5, newPlayer.getEntityWorld().getSpawnPoint().getPos().getY(), newPlayer.getEntityWorld().getSpawnPoint().getPos().getZ() + 0.5);
            dim = newPlayer.getEntityWorld().getServer().getOverworld(); // Change in 1.21.9 because world spawn can be in any dimension
            rot = List.of(newPlayer.getYaw(), newPlayer.getPitch());
        }

        TeleportTarget.PostDimensionTransition enableFlight = (flyingEntity) -> {
            if (!(flyingEntity instanceof ServerPlayerEntity player)) return;
            if (!player.getAbilities().allowFlying) return;
            player.getAbilities().flying = true;
            player.sendAbilitiesUpdate();
        };

        TeleportTarget teleportTarget = new TeleportTarget(
                dim, pos, Vec3d.ZERO, rot.get(0), rot.get(1), enableFlight);

        LOGGER.info("Player " + newPlayer.getName().getString() + " position: " + newPlayer.getX() + ", " + newPlayer.getY() + ", " + newPlayer.getZ() + " in dimension " + newPlayer.getEntityWorld().getRegistryKey().getValue().toString());
        LOGGER.info("Teleporting player " + newPlayer.getName().getString() + " to " + pos.x + ", " + pos.y + ", " + pos.z + " in dimension " + (dim != null ? dim.getRegistryKey().getValue().toString() : "null"));
        //if (pos != null && dim != null) newPlayer.teleport(dim, posX, posY, posZ, posFlags, yaw, pitch, true);
        newPlayer.teleportTo(teleportTarget);
        LOGGER.info("Player " + newPlayer.getName().getString() + " position: " + newPlayer.getX() + ", " + newPlayer.getY() + ", " + newPlayer.getZ() + " in dimension " + newPlayer.getEntityWorld().getRegistryKey().getValue().toString());


    }
}
