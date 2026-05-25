package io.github.kgriff0n.mixin;

import com.mojang.brigadier.ParseResults;
import io.github.kgriff0n.packet.play.CommandPacket;
import io.github.kgriff0n.api.ServersLinkApi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

@Mixin(Commands.class)
public class CommandManagerMixin {

    @Inject(at = @At("TAIL"), method = "performCommand")
    private void executeCommand(ParseResults<CommandSourceStack> parseResults, String command, CallbackInfo ci) {
        if (!parseResults.getContext().getSource().getTextName().equals("do-not-send-back")) {
            ServerPlayer player = parseResults.getContext().getSource().getPlayer();
            UUID uuid = null;
            if (player != null) uuid = player.getUUID();
            if (command.startsWith("server run ")) {
                if (player != null) {
                    if (command.contains("@r")) {
                        player.sendSystemMessage(Component.literal("Warning, using @r can cause desync between servers").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                    }
                    if (command.contains("execute")) {
                        player.sendSystemMessage(Component.literal("Be careful when using execute, especially with the positions").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                    }
                    if (command.contains("teleport") || command.contains("tp") || command.contains("whitelist") || command.contains("op")) {
                        player.sendSystemMessage(Component.literal("You should use native /server commands").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                    }
                }
            }
            ServersLinkApi.send(new CommandPacket(uuid, command));
        }
    }

}
