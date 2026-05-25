package io.github.kgriff0n.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public class ServersLinkEvents {

    /**
     * Gateway only
     */
    public static final Event<@NotNull FirstJoin> FIRST_JOIN = EventFactory.createArrayBacked(FirstJoin.class, listeners -> (player) -> {
        for (FirstJoin listener : listeners) {
            listener.onFirstJoin(player);
        }
    });

    @FunctionalInterface
    public interface FirstJoin {
        void onFirstJoin(ServerPlayer player);
    }

}
