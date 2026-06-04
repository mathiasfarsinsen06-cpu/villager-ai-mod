package com.mathiasfar.villagermod.commands;

import com.mathiasfar.villagermod.village.VillageInfo;
import com.mathiasfar.villagermod.village.VillageLocator;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class VillageCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger("aivillages");
    private static List<VillageInfo> cachedVillages = null;
    private static int currentVillageIndex = 0;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("aivillages")
                        .then(Commands.literal("list")
                                .executes(VillageCommand::listVillages))
                        .then(Commands.literal("next")
                                .executes(VillageCommand::nextVillage))
                        .then(Commands.literal("info")
                                .executes(VillageCommand::infoVillage))
        );
        LOGGER.info("✅ AIVillages commands registered successfully!");
    }

    private static int listVillages(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();

            if (cachedVillages == null) {
                cachedVillages = VillageLocator.findAllVillages(0,
                        (int) player.getX(), (int) player.getZ());
            }

            player.displayClientMessage(Component.literal("━━━━━ ALLE BYER ━━━━━"), false);
            if (cachedVillages.isEmpty()) {
                player.displayClientMessage(Component.literal("❌ Ingen byer fundet!"), false);
            } else {
                for (VillageInfo v : cachedVillages) {
                    player.displayClientMessage(Component.literal(v.toString()), false);
                }
            }

            return 1;
        } catch (Exception e) {
            LOGGER.error("Error in listVillages command", e);
            return 0;
        }
    }

    private static int nextVillage(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();

            if (cachedVillages == null) {
                cachedVillages = VillageLocator.findAllVillages(0,
                        (int) player.getX(), (int) player.getZ());
            }

            if (cachedVillages.isEmpty()) {
                player.displayClientMessage(Component.literal("❌ Ingen byer fundet!"), false);
                return 0;
            }

            VillageInfo village = cachedVillages.get(currentVillageIndex);
            player.teleportTo((double) village.x, 64, (double) village.z);

            player.displayClientMessage(
                    Component.literal("✅ TP til: " + village.toString()), false);

            currentVillageIndex = (currentVillageIndex + 1) % cachedVillages.size();
            return 1;
        } catch (Exception e) {
            LOGGER.error("Error in nextVillage command", e);
            return 0;
        }
    }

    private static int infoVillage(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();

            if (cachedVillages == null || cachedVillages.isEmpty()) {
                player.displayClientMessage(Component.literal("❌ Ingen by valgt!"), false);
                return 0;
            }

            VillageInfo village = cachedVillages.get(currentVillageIndex - 1);

            player.displayClientMessage(Component.literal("📍 " + village.name), false);
            player.displayClientMessage(Component.literal("👥 Population: " + village.population), false);
            player.displayClientMessage(Component.literal("⭐ Prosperity: " + village.prosperity), false);
            player.displayClientMessage(Component.literal("👑 Leader: " + village.leader), false);

            return 1;
        } catch (Exception e) {
            LOGGER.error("Error in infoVillage command", e);
            return 0;
        }
    }
}