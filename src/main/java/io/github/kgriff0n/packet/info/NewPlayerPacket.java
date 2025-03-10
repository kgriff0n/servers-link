package io.github.kgriff0n.packet.info;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;
import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.util.ServersLinkUtil;

import java.util.UUID;

public class NewPlayerPacket implements Packet {

    private final UUID uuid;
    private final String name;

    private final String properties;

    public NewPlayerPacket(GameProfile profile) {
        this.uuid = profile.getId();
        this.name = profile.getName();
        this.properties = new Gson().toJson(new PropertyMap.Serializer().serialize(profile.getProperties(), null, null));
    }

    @Override
    public boolean shouldTransfer() {
        return true;
    }

    @Override
    public void onReceive() {
        PropertyMap properties = new PropertyMap.Serializer().deserialize(JsonParser.parseString(this.properties), null, null);
        GameProfile profile = new GameProfile(this.uuid, this.name);

        /* Initialize game profile */
        PropertyMap gameProfileProperties = profile.getProperties();
        properties.forEach(gameProfileProperties::put);

        ServersLinkUtil.addDummyPlayer(profile);
    }
}
