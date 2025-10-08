package io.github.kgriff0n;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.github.kgriff0n.command.ServerCommand;
import io.github.kgriff0n.event.*;
import io.github.kgriff0n.server.ServerInfo;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ServersLink implements ModInitializer {
	public static final String MOD_ID = "servers-link";
	public static final Path CONFIG = FabricLoader.getInstance().getConfigDir().resolve("servers-link");
	public static boolean isGateway;

	private static ServerInfo serverInfo;
	private static String gatewayIp;
	private static int gatewayPort;

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static MinecraftServer SERVER;
	public static boolean IS_RUNNING = true;
	public static boolean CONFIG_ERROR = false;

	@Override
	public void onInitialize() {

		loadServerInfo();

		ServerCommand.register();

		ServerLifecycleEvents.SERVER_STARTED.register(new ServerStart());
		ServerLifecycleEvents.SERVER_STOPPING.register(new ServerStopping());
		ServerLifecycleEvents.SERVER_STOPPED.register(new ServerStopped());
		ServerPlayConnectionEvents.JOIN.register(new PlayerJoin());
		ServerPlayConnectionEvents.DISCONNECT.register(new PlayerDisconnect());
		ServerTickEvents.START_SERVER_TICK.register(new ServerTick());
		ServerEntityEvents.ENTITY_LOAD.register(new PlayerJoin());
    }

	public static ServerInfo getServerInfo() {
		return serverInfo;
	}

	public static String getGatewayIp() {
		return gatewayIp;
	}

	public static int getGatewayPort() {
		return gatewayPort;
	}

	private void loadServerInfo() {
		Path path = CONFIG.resolve("info.json");
		try {
			String jsonContent = Files.readString(path);
			Gson gson = new Gson();
			JsonObject jsonObject = gson.fromJson(jsonContent, JsonObject.class);
            isGateway = jsonObject.get("gateway").getAsBoolean();
			gatewayIp = jsonObject.get("gateway-ip").getAsString();
			gatewayPort = jsonObject.get("gateway-port").getAsInt();
			serverInfo = new ServerInfo(
					jsonObject.get("group").getAsString(),
					jsonObject.get("server-name").getAsString(),
					jsonObject.get("server-ip").getAsString(),
					jsonObject.get("server-port").getAsInt()
			);
		} catch (IOException e) {
			CONFIG_ERROR = true;
			ServersLink.LOGGER.error("Unable to read info.json");
		}
	}
}