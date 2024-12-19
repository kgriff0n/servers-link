package io.github.kgriff0n.util;

import java.io.Serializable;
import java.util.HashMap;
import java.util.UUID;

public class ServerInfo implements Serializable {

    private final String name;
    private final String ip;
    private final int port;

    private float tps;
    private boolean down;

    private final HashMap<UUID, String> playersList;

    public ServerInfo(String name, String ip, int port) {
        this.name = name;
        this.ip = ip;
        this.port = port;

        this.tps = 20.0f;
        this.down = false;

        this.playersList = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public HashMap<UUID, String> getPlayersList() {
        return playersList;
    }

    public void addPlayer(UUID uuid, String name) {
        this.playersList.put(uuid, name);
    }

    public void removePlayer(UUID uuid) {
        this.playersList.remove(uuid);
    }

    public boolean isDown() {
        return down;
    }

    public void setDown(boolean down) {
        this.down = down;
    }

    public float getTps() {
        return tps;
    }

    public void setTps(float tps) {
        this.tps = tps;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ServerInfo serverInfo) {
            return serverInfo.name.equals(this.name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }
}
