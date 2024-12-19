package io.github.kgriff0n.mixin;

import com.mojang.brigadier.ParseResults;
import io.github.kgriff0n.Config;
import io.github.kgriff0n.packet.play.CommandPacket;
import io.github.kgriff0n.util.ServersLinkUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(CommandManager.class)
public class CommandManagerMixin {

    @Inject(at = @At("TAIL"), method = "execute")
    private void executeCommand(ParseResults<ServerCommandSource> parseResults, String command, CallbackInfo ci) {
        if (!parseResults.getContext().getSource().getName().equals("do-not-send-back")) {
            ServerPlayerEntity player = parseResults.getContext().getSource().getPlayer();
            UUID uuid = null;
            if (player != null) uuid = player.getUuid();
            if (Config.syncWhitelist && command.startsWith("whitelist") ||
                    Config.syncRoles && (command.startsWith("op") || command.startsWith("deop") ||
                            (FabricLoader.getInstance().isModLoaded("player-roles")
                                    && command.startsWith("role")))) {
                ServersLinkUtil.send(new CommandPacket(uuid, command));
            } else if (command.startsWith("server run ")) {
                String parseCommand = command.substring(11);
                if (player != null) {
                    if (parseCommand.contains("@r")) {
                        player.sendMessage(Text.literal("Warning, using @r can cause desync between servers").formatted(Formatting.RED, Formatting.BOLD));
                    }
                    if (parseCommand.contains("execute")) {
                        player.sendMessage(Text.literal("Be careful when using execute, especially with the positions").formatted(Formatting.RED, Formatting.BOLD));
                    }
                    if (parseCommand.contains("teleport") || parseCommand.contains("tp") || parseCommand.contains("whitelist") || parseCommand.contains("op")) {
                        player.sendMessage(Text.literal("You should use native /server commands").formatted(Formatting.RED, Formatting.BOLD));
                    }
                }
                ServersLinkUtil.send(new CommandPacket(uuid, parseCommand));
            }
        }
    }

}
