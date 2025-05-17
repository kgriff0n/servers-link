package io.github.kgriff0n;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.UUID;

public class PlayersInformation extends PersistentState {

    private static final HashMap<UUID, String> lastServer = new HashMap<>();

    public static void setLastServer(UUID player, String serverName) {
        lastServer.put(player, serverName);
    }

    public static String getLastServer(UUID player) {
        return lastServer.get(player);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        lastServer.forEach((uuid, name) -> nbt.putString(uuid.toString(), name));
        return nbt;
    }

    public static PlayersInformation createFromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        PlayersInformation state = new PlayersInformation();
        for (String uuid : nbt.getKeys()) {
            UUID player = UUID.fromString(uuid);
            lastServer.put(player, nbt.getString(uuid));
        }
        return state;
    }

    private static final Type<PlayersInformation> type = new Type<>(
            PlayersInformation::new, // If there's no 'StateSaverAndLoader' yet create one
            PlayersInformation::createFromNbt, // If there is a 'StateSaverAndLoader' NBT, parse it with 'createFromNbt'
            null // Supposed to be an 'DataFixTypes' enum, but we can just pass null
    );

    public static PlayersInformation loadPlayersInfo(MinecraftServer server) {
        PersistentStateManager persistentStateManager = server.getWorld(World.OVERWORLD).getPersistentStateManager();
        PlayersInformation state = persistentStateManager.getOrCreate(type, ServersLink.MOD_ID);
        state.markDirty();
        return state;
    }
}
