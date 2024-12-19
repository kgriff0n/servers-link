package io.github.kgriff0n;

import net.fabricmc.loader.api.FabricLoader;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static io.github.kgriff0n.ServersLink.CONFIG_ERROR;

public class Config {

    public static final String PATH = FabricLoader.getInstance().getGameDir().resolve("servers-link.properties").toString();

    public static boolean isHub;
    public static String hubIp;
    public static int hubPort;
    public static String serverName;
    public static String serverIp;
    public static int serverPort;
    public static boolean syncPlayerList;
    public static boolean syncChat;
    public static boolean syncPlayerData;
    public static boolean syncWhitelist;
    public static boolean syncRoles;
    public static boolean reconnectToLastServer;
    public static boolean whitelistIp;
    public static List<String> whitelistedIp;


    public static void write() {
        Properties properties = new Properties();

        properties.setProperty("hub", "");
        properties.setProperty("hub-ip", "");
        properties.setProperty("hub-port", "");
        properties.setProperty("server-name", "");
        properties.setProperty("server-ip", "");
        properties.setProperty("server-port", "");
        properties.setProperty("sync-player-list", "true");
        properties.setProperty("sync-chat", "true");
        properties.setProperty("sync-player-data", "true");
        properties.setProperty("sync-whitelist", "true");
        properties.setProperty("sync-roles", "true");
        properties.setProperty("reconnect-last-server", "true");
        properties.setProperty("whitelist-ip", "false");
        properties.setProperty("whitelisted-ip", "");

        try (FileOutputStream fos = new FileOutputStream(PATH)) {
            properties.store(fos, "Servers Link properties");
        } catch (IOException e) {
            ServersLink.LOGGER.info("Can't write properties");
        }
    }

    public static void load() {
        try (FileInputStream fis = new FileInputStream(PATH)) {
            Properties properties = new Properties();
            properties.load(fis);

            isHub = Boolean.parseBoolean(properties.getProperty("hub"));
            hubIp = properties.getProperty("hub-ip");
            hubPort = Integer.parseInt(properties.getProperty("hub-port"));
            serverName = properties.getProperty("server-name");
            serverIp = properties.getProperty("server-ip");
            serverPort = Integer.parseInt(properties.getProperty("server-port"));
            syncPlayerList = Boolean.parseBoolean(properties.getProperty("sync-player-list"));
            syncChat = Boolean.parseBoolean(properties.getProperty("sync-chat"));
            syncPlayerData = Boolean.parseBoolean(properties.getProperty("sync-player-data"));
            syncWhitelist = Boolean.parseBoolean(properties.getProperty("sync-whitelist"));
            syncRoles = Boolean.parseBoolean(properties.getProperty("sync-roles"));
            reconnectToLastServer = Boolean.parseBoolean(properties.getProperty("reconnect-last-server"));
            whitelistIp = Boolean.parseBoolean(properties.getProperty("whitelist-ip"));
            whitelistedIp = Arrays.asList(properties.getProperty("whitelisted-ip").split(","));

        } catch (Exception e) {
            ServersLink.LOGGER.info("Can't load properties");
            CONFIG_ERROR = true;
        }
    }

    public static boolean exists() {
        return Files.exists(Path.of(PATH));
    }

}
