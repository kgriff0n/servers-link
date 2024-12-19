package io.github.kgriff0n.packet.server;

import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.packet.Packet;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class UpdateWhitelistPacket implements Packet {

    private static final Path PATH = FabricLoader.getInstance().getGameDir().resolve("whitelist.json");

    private final byte[] whitelist;

    public UpdateWhitelistPacket() throws IOException {
        this.whitelist = read();
    }

    @Override
    public void onReceive() {
        try {
            write();
        } catch (IOException e) {
            ServersLink.LOGGER.error("Unable to write whitelist");
        }
    }

    private byte[] read() throws IOException {
        return Files.readAllBytes(PATH);
    }

    private void write() throws IOException {
        Files.write(PATH, whitelist);
    }
}
