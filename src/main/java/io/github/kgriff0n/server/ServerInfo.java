package io.github.kgriff0n.server;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.client.data.PropertiesMap;

import java.io.Serializable;
import java.util.*;

public class ServerInfo implements Serializable {

    private final String groupId;
    private final String name;
    private final String ip;
    private final int port;

    private final int randomValue;

    private float tps;
    private boolean down;

    private final HashMap<UUID, String> playersList;
    private final HashMap<UUID, String> playersPropertiesList;

    public ServerInfo(String groupId, String name, String ip, int port) {
        this.groupId = groupId;
        this.name = name;
        this.ip = ip;
        this.port = port;

        this.tps = 20.0f;
        this.down = false;

        this.playersList = new HashMap<>();
        this.playersPropertiesList = new HashMap<>();

        this.randomValue = new Random().nextInt();
    }

    public String getGroupId() {
        return groupId;
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

    public List<GameProfile> getGameProfile() {
        List<GameProfile> list = new ArrayList<>();
        for (Map.Entry<UUID, String> entry : playersList.entrySet()) {
            PropertyMap properties = new PropertyMap.Serializer().deserialize(JsonParser.parseString(playersPropertiesList.get(entry.getKey())), null, null);
            GameProfile profile = new GameProfile(entry.getKey(), entry.getValue());
            /* Initialize game profile */
            PropertyMap gameProfileProperties = profile.getProperties();
            properties.forEach(gameProfileProperties::put);
            list.add(profile);
        }
        return list;
    }

    public void addPlayer(GameProfile profile) {
        this.playersList.put(profile.getId(), profile.getName());
        this.playersPropertiesList.put(profile.getId(), new Gson().toJson(new PropertyMap.Serializer().serialize(profile.getProperties(), null, null)));
    }

    public void addPlayer(UUID uuid, String name, String properties) {
        this.playersList.put(uuid, name);
        this.playersPropertiesList.put(uuid, properties);
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
    public String toString() {
        return String.format("%s[group=%s,player_list=%s,rng=%s]", getName(), getGroupId(), new ArrayList<>(playersList.keySet()), randomValue);
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }
}
