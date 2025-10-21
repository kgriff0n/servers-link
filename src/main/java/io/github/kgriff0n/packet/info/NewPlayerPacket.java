package io.github.kgriff0n.packet.info;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.api.ServersLinkApi;
import io.github.kgriff0n.server.Settings;

import java.util.Objects;
import java.util.UUID;

public class NewPlayerPacket implements Packet {

    private final UUID uuid;
    private final String name;

    private final String properties;

    public NewPlayerPacket(GameProfile profile) {
        this.uuid = profile.id();
        this.name = profile.name();
        this.properties = new Gson().toJson(new PropertyMap.Serializer().serialize(profile.properties(), null, null));
    }

    @Override
    public boolean shouldReceive(Settings settings) {
        return settings.isPlayerListSynced();
    }

    @Override
    public void onReceive() {
        PropertyMap properties = new PropertyMap.Serializer().deserialize(JsonParser.parseString(this.properties), null, null);
        GameProfile profile = new GameProfile(this.uuid, this.name, properties);

        ServersLinkApi.addDummyPlayer(profile);
    }
}
