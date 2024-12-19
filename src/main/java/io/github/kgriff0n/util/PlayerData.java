package io.github.kgriff0n.util;

import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static io.github.kgriff0n.ServersLink.SERVER;

public class PlayerData {

    private static final Path PATH = FabricLoader.getInstance().getGameDir().resolve(SERVER.getSaveProperties().getLevelName());

    public static Path getDataPath(UUID uuid) {
        Path playerPath = PATH.resolve("playerdata").resolve(uuid + ".dat");
        return playerPath;
    }

    public static Path getAdvancementsPath(UUID uuid) {
        File file = PATH.toFile();
        file.mkdir();
        return PATH.resolve("advancements").resolve(uuid + ".json");
    }

    public static Path getStatsPath(UUID uuid) {
        return PATH.resolve("stats").resolve(uuid + ".json");
    }

    public static byte[] readData(UUID uuid) throws IOException {
        return Files.readAllBytes(getDataPath(uuid));
    }

    public static byte[] readAdvancements(UUID uuid) throws IOException {
        return Files.readAllBytes(getAdvancementsPath(uuid));
    }

    public static byte[] readStats(UUID uuid) throws IOException {
        return Files.readAllBytes(getStatsPath(uuid));
    }

    public static void writeData(UUID uuid, byte[] data) throws IOException {
        Files.write(getDataPath(uuid), data);
    }

    public static void writeAdvancements(UUID uuid, byte[] data) throws IOException {
        /* If the server is started for the first time, folder doesn't exist */
        File dir = PATH.resolve("advancements").toFile();
        if (!dir.exists()) {
            dir.mkdir();
        }
        Files.write(getAdvancementsPath(uuid), data);
    }

    public static void writeStats(UUID uuid, byte[] data) throws IOException {
        /* If the server is started for the first time, folder doesn't exist */
        File dir = PATH.resolve("stats").toFile();
        if (!dir.exists()) {
            dir.mkdir();
        }
        Files.write(getStatsPath(uuid), data);
    }

}
