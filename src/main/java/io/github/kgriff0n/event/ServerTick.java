package io.github.kgriff0n.event;

import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.packet.info.ServerStatusPacket;
import io.github.kgriff0n.packet.info.ServersInfoPacket;
import io.github.kgriff0n.socket.Gateway;
import io.github.kgriff0n.socket.SubServer;
import io.github.kgriff0n.api.ServersLinkApi;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ServerTick implements ServerTickEvents.StartTick {

    private static final ConcurrentHashMap<UUID, Integer> shouldDisconnect = new ConcurrentHashMap<>();

    public static void scheduleDisconnect(UUID player, int ticks) {
        shouldDisconnect.put(player, ticks);
    }

    private int count = 0;

    @Override
    public void onStartTick(MinecraftServer server) {
        count++;
        if (count >= 600) { // every 30s
            count = 0;
            float tps = server.getTickManager().getTickRate();
            /* update self */
            ServersLink.getServerInfo().setTps(tps);
            if (ServersLink.isGateway) {
                Gateway.getInstance().sendAll(new ServersInfoPacket(ServersLinkApi.getServerList()));
            } else {
                SubServer.getInstance().send(new ServerStatusPacket(ServersLink.getServerInfo().getName(), tps, false));
            }
        }

        // Disconnect players
        Iterator<Map.Entry<UUID, Integer>> players = shouldDisconnect.entrySet().iterator();
        while (players.hasNext()) {
            Map.Entry<UUID, Integer> playerEntry = players.next();
            int ticksLeft = playerEntry.getValue() - 1;
            if (ticksLeft <= 0) {
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerEntry.getKey());
                if (player != null && !player.isDisconnected()) {
                    player.networkHandler.disconnect(Text.translatable("connect.transferring"));
                }
                players.remove();
            } else {
                playerEntry.setValue(ticksLeft);
            }
        }
    }
}
