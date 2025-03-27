package io.github.kgriff0n.packet.server;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;
import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.packet.info.ServersInfoPacket;
import io.github.kgriff0n.socket.Gateway;
import io.github.kgriff0n.api.ServersLinkApi;

import java.util.UUID;


public class PlayerAcknowledgementPacket implements Packet {

    private final String serverName;
    private final UUID uuid;
    private final String name;
    private final String properties;

    public PlayerAcknowledgementPacket(String serverName, GameProfile profile) {
        this.serverName = serverName;
        this.uuid = profile.getId();
        this.name = profile.getName();
        this.properties = new Gson().toJson(new PropertyMap.Serializer().serialize(profile.getProperties(), null, null));
    }

    @Override
    public void onReceive(String sender) {
        if (ServersLink.isGateway) {
            ServersLinkApi.getServer(serverName).addPlayer(this.uuid, this.name, this.properties);
            Gateway.getInstance().sendAll(new ServersInfoPacket(ServersLinkApi.getServerList()));
        }
    }
}
