package io.github.kgriff0n;

import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.UUID;

public class PlayersInformation {

    private static final HashMap<UUID, String> lastServer = new HashMap<>();

    public static void setLastServer(UUID player, String serverName) {
        lastServer.put(player, serverName);
    }

    public static String getLastServer(UUID player) {
        return lastServer.get(player);
    }

    public static void saveNbt(MinecraftServer server) {
        NbtCompound nbt = new NbtCompound();
        lastServer.forEach((uuid, name) -> nbt.putString(uuid.toString(), name));

        Path dataFile = server
                .getSavePath(WorldSavePath.ROOT)
                .resolve("data")
                .resolve("servers_link.nbt");

        try (OutputStream os = Files.newOutputStream(dataFile)) {
            NbtIo.writeCompressed(nbt, os);
        } catch (IOException e) {
            ServersLink.LOGGER.error("Unable to save data");
        }
    }

    public static void loadNbt(MinecraftServer server) {
        Path dataFile = server
                .getSavePath(WorldSavePath.ROOT)
                .resolve("data")
                .resolve("servers_link.nbt");

        try (InputStream is = Files.newInputStream(dataFile)) {
            NbtCompound nbt = NbtIo.readCompressed(is, NbtSizeTracker.ofUnlimitedBytes());
            for (String uuid : nbt.getKeys()) {
                UUID player = UUID.fromString(uuid);
                nbt.getString(uuid).ifPresent(string -> lastServer.put(player, string));
            }
        } catch (IOException e) {
            ServersLink.LOGGER.error("Unable to load data");
        }
    }
}
