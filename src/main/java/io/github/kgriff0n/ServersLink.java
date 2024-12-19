package io.github.kgriff0n;

import io.github.kgriff0n.command.ServerCommand;
import io.github.kgriff0n.event.*;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServersLink implements ModInitializer {
	public static final String MOD_ID = "servers-link";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static MinecraftServer SERVER;
	public static boolean IS_RUNNING = true;
	public static boolean CONFIG_ERROR = false;

	@Override
	public void onInitialize() {

		if (!Config.exists()) {
			Config.write();
			CONFIG_ERROR = true;
		}
		Config.load();

		ServerCommand.register();

		ServerLifecycleEvents.SERVER_STARTED.register(new ServerStart());
		ServerLifecycleEvents.SERVER_STOPPING.register(new ServerStop());
		ServerPlayConnectionEvents.JOIN.register(new PlayerJoin());
		ServerPlayConnectionEvents.DISCONNECT.register(new PlayerDisconnect());
		ServerTickEvents.START_SERVER_TICK.register(new ServerTick());
    }
}