package io.github.kgriff0n.util;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.impl.event.interaction.FakePlayerNetworkHandler;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.server.network.ServerPlayerEntity;

import static io.github.kgriff0n.ServersLink.SERVER;

public class DummyPlayer extends ServerPlayerEntity {
    public DummyPlayer(GameProfile profile) {
        super(SERVER, SERVER.getOverworld(), profile, SyncedClientOptions.createDefault());
        this.networkHandler = new FakePlayerNetworkHandler(this);
    }
}
