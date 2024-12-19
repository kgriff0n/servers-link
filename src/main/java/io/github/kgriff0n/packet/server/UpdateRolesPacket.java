package io.github.kgriff0n.packet.server;

import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.packet.Packet;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class UpdateRolesPacket implements Packet {

    private static final Path OP_PATH = FabricLoader.getInstance().getGameDir().resolve("ops.json");
    private static final Path ROLES_PATH = FabricLoader.getInstance().getGameDir().resolve("playerdata").resolve("player_roles");

    private final byte[] ops;
    private final byte[] playerRoles;

    public UpdateRolesPacket() throws IOException {
        this.ops = readOps();
        if (FabricLoader.getInstance().isModLoaded("player-roles") && ROLES_PATH.toFile().exists()) {
            this.playerRoles = readPlayerRoles();
        } else {
            this.playerRoles = null;
        }
    }

    @Override
    public void onReceive() {
        try {
            writeOps();
            if (FabricLoader.getInstance().isModLoaded("player-roles")) {
                writePlayerRoles();
            }
        } catch (IOException e) {
            ServersLink.LOGGER.error("Unable to write whitelist");
        }
    }

    private byte[] readOps() throws IOException {
        return Files.readAllBytes(OP_PATH);
    }

    private byte[] readPlayerRoles() throws IOException {
        return Files.readAllBytes(ROLES_PATH);
    }

    private void writeOps() throws IOException {
        Files.write(OP_PATH, ops);
    }

    private void writePlayerRoles() throws IOException {
        Files.write(ROLES_PATH, playerRoles);
    }
}
