package io.github.kgriff0n.mixin;

import com.mojang.brigadier.ParseResults;
import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.packet.play.CommandPacket;
import io.github.kgriff0n.api.ServersLinkApi;
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
            if (command.startsWith("server run ")) {
                if (player != null) {
                    if (command.contains("@r")) {
                        player.sendMessage(Text.literal("Warning, using @r can cause desync between servers").formatted(Formatting.RED, Formatting.BOLD));
                    }
                    if (command.contains("execute")) {
                        player.sendMessage(Text.literal("Be careful when using execute, especially with the positions").formatted(Formatting.RED, Formatting.BOLD));
                    }
                    if (command.contains("teleport") || command.contains("tp") || command.contains("whitelist") || command.contains("op")) {
                        player.sendMessage(Text.literal("You should use native /server commands").formatted(Formatting.RED, Formatting.BOLD));
                    }
                }
            }
            ServersLinkApi.send(new CommandPacket(uuid, command), ServersLink.getServerInfo().getName());
        }
    }

}
