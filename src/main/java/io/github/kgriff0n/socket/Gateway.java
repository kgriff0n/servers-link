package io.github.kgriff0n.socket;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.server.Group;
import io.github.kgriff0n.server.ServerInfo;
import io.github.kgriff0n.api.ServersLinkApi;
import io.github.kgriff0n.server.Settings;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static io.github.kgriff0n.ServersLink.IS_RUNNING;
import static io.github.kgriff0n.ServersLink.SERVER;

public class Gateway extends Thread {

    public static Gateway gateway;

    private HashMap<String, Group> groups;
    private ServerSocket serverSocket;

    private boolean debug;
    private boolean globalPlayerCount;
    private boolean whitelistIp;
    private final List<String> whitelistedIp = new ArrayList<>();
    private boolean reconnectLastServer;

    public Gateway(int port) {
        if (gateway != null) {
            ServersLink.LOGGER.info("Gateway server already started");
        }
        try {
            serverSocket = new ServerSocket(port);
            gateway = this;
            groups = new HashMap<>();
            loadGroups();
            ServersLinkApi.addServer(ServersLink.getServerInfo(), null);
        } catch (IOException e) {
            ServersLink.LOGGER.info("Unable to start central server");
        }
    }

    public static Gateway getInstance() {
        return gateway;
    }

    public void sendAll(Packet packet) {
        for (G2SConnection sub : ServersLinkApi.getServerMap().values()) {
            if (sub != null) sub.send(packet);
        }
    }

    public void sendTo(Packet packet, String serverName) {
        if (serverName.equals(ServersLink.getServerInfo().getName())) {
            SERVER.execute(packet::onReceive);
        } else {
            for (ServerInfo server : ServersLinkApi.getServerList()) {
                if (server.getName().equals(serverName)) {
                    ServersLinkApi.getServerMap().get(server).send(packet);
                }
            }
        }
    }

    public void forward(Packet packet, String sourceServer) {
        String sourceGroup = ServersLinkApi.getServer(sourceServer).getGroupId();
        for (ServerInfo server : ServersLinkApi.getServerList()) {
            G2SConnection sub = ServersLinkApi.getServerMap().get(server);
            if (sub != null && !server.getName().equals(sourceServer)) {
                if (isDebugEnabled()) ServersLink.LOGGER.info("\u001B[33mForward packet {} to {}?", packet.getClass().getName(), server.getName());
                if (packet.shouldReceive(getSettings(sourceGroup, server.getGroupId()))) {
                    if (isDebugEnabled()) ServersLink.LOGGER.info("\u001B[32mYes");
                    sub.send(packet);
                } else {
                    if (isDebugEnabled()) ServersLink.LOGGER.info("\u001B[31mNo");
                }
            }
        }
    }

    public void removePlayer(UUID uuid) {
        for (ServerInfo server : ServersLinkApi.getServerList()) {
            server.removePlayer(uuid);
        }
    }

    public boolean isConnectedPlayer(UUID uuid) {
        for (ServerInfo server : ServersLinkApi.getServerList()) {
            if (server.getPlayersList().containsKey(uuid)) {
                return true;
            }
        }
        return false;
    }

    public Settings getSettings(String sourceGroup, String destinationGroup) {
        Group a = groups.get(sourceGroup);
        if (a.getRules().containsKey(destinationGroup)) {
            return a.getRules().get(destinationGroup);
        } else if (sourceGroup.equals(destinationGroup)) {
            return a.getSettings();
        } else {
            return groups.get("global").getSettings();
        }
    }

    public Group getGroup(String groupId) {
        return groups.get(groupId);
    }

    public Collection<Group> getGroups() {
        return groups.values();
    }

    public void loadConfig() {
        Path path = ServersLink.CONFIG.resolve("config.json");
        try {
            String jsonContent = Files.readString(path);
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(jsonContent, JsonObject.class);
            debug = jsonObject.get("debug").getAsBoolean();
            globalPlayerCount = jsonObject.get("global_player_count").getAsBoolean();
            whitelistIp = jsonObject.get("whitelist_ip").getAsBoolean();
            for (JsonElement element : jsonObject.getAsJsonArray("whitelisted_ip")) {
                whitelistedIp.add(element.getAsString());
            }
            reconnectLastServer = jsonObject.get("reconnect_last_server").getAsBoolean();
        } catch (IOException e) {
            ServersLink.LOGGER.error("Unable to read config.json");
        }
    }

    public boolean isDebugEnabled() {
        return debug;
    }

    public boolean isGlobalPlayerCountEnabled() {
        return globalPlayerCount;
    }

    public boolean hasWhitelistIp() {
        return whitelistIp;
    }

    public List<String> getWhitelistedIp() {
        return whitelistedIp;
    }

    public boolean shouldReconnectToLastServer() {
        return reconnectLastServer;
    }

    private void loadGroups() {
        Path path = ServersLink.CONFIG.resolve("groups.json");
        try {
            String jsonContent = Files.readString(path);
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(jsonContent, JsonObject.class);
            // GROUPS
            JsonObject jsonGroups = jsonObject.getAsJsonObject("groups");
            // Global
            JsonObject global = jsonGroups.getAsJsonObject("global");
            Settings globalSettings = new Settings(
                    global.get("player_list").getAsBoolean(),
                    global.get("chat").getAsBoolean(),
                    global.get("player_data").getAsBoolean(),
                    global.get("whitelist").getAsBoolean(),
                    global.get("roles").getAsBoolean()
            );
            groups.put("global", new Group("global", globalSettings));
            // Others
            for (Map.Entry<String, JsonElement> entry : jsonGroups.entrySet()) {
                JsonObject otherGroup = entry.getValue().getAsJsonObject();
                Settings otherSettings = new Settings(
                        otherGroup.has("player_list") ? otherGroup.get("player_list").getAsBoolean() : globalSettings.isPlayerListSynced(),
                        otherGroup.has("chat") ? otherGroup.get("chat").getAsBoolean() : globalSettings.isChatSynced(),
                        otherGroup.has("player_data") ? otherGroup.get("player_data").getAsBoolean() : globalSettings.isPlayerDataSynced(),
                        otherGroup.has("whitelist") ? otherGroup.get("whitelist").getAsBoolean() : globalSettings.isWhitelistSynced(),
                        otherGroup.has("roles") ? otherGroup.get("roles").getAsBoolean() : globalSettings.isRolesSynced()
                );
                if (!entry.getKey().equals("global")) { // Doesn't re-add global group
                    groups.put(entry.getKey(), new Group(entry.getKey(), otherSettings));
                }
            }

            // RULES
            JsonArray jsonRules = jsonObject.get("rules").getAsJsonArray();
            for (JsonElement element : jsonRules) {
                JsonObject rule = element.getAsJsonObject();
                JsonArray ruleGroups = rule.getAsJsonArray("groups");
                Settings ruleSettings = new Settings(
                        rule.has("player_list") ? rule.get("player_list").getAsBoolean() : globalSettings.isPlayerListSynced(),
                        rule.has("chat") ? rule.get("chat").getAsBoolean() : globalSettings.isChatSynced(),
                        rule.has("player_data") ? rule.get("player_data").getAsBoolean() : globalSettings.isPlayerDataSynced(),
                        rule.has("whitelist") ? rule.get("whitelist").getAsBoolean() : globalSettings.isWhitelistSynced(),
                        rule.has("roles") ? rule.get("roles").getAsBoolean() : globalSettings.isRolesSynced()
                );
                for (int i = 0; i < ruleGroups.size(); i++) {
                    String groupId = ruleGroups.get(i).getAsString();
                    for (int j = 0; j < ruleGroups.size(); j++) {
                        if (i != j) {
                            groups.get(groupId).addRule(ruleGroups.get(j).getAsString(), ruleSettings);
                        }
                    }
                }
            }
        } catch (IOException e) {
            ServersLink.LOGGER.error("Unable to read groups.json");
        }
    }

    @Override
    public void run() {
        while (IS_RUNNING) {
            try {
                Socket socket = serverSocket.accept();
                if (whitelistIp) {
                    if (whitelistedIp.contains(socket.getInetAddress().getHostAddress())) {
                        G2SConnection connection = new G2SConnection(socket);
                        connection.start();
                    } else {
                        ServersLink.LOGGER.warn("Unauthorized connection received from {}", socket.getInetAddress().getHostAddress());
                        socket.close();
                    }
                } else {
                    G2SConnection connection = new G2SConnection(socket);
                    connection.start();
                }
            } catch (IOException e) {
                ServersLink.LOGGER.info("Unable to accept connection");
            }

        }
    }
}
