package com.example.examplemod.village;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;
import java.util.Optional;

public final class VillageTPCommand {
    private VillageTPCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("village")
                        .then(Commands.literal("tp")
                                .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                        .executes(VillageTPCommand::teleportToVillage)))
                        .then(Commands.literal("list")
                                .executes(VillageTPCommand::listVillages))
                        .then(Commands.literal("count")
                                .executes(VillageTPCommand::villageCount))
        );
    }

    private static int teleportToVillage(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        int index = IntegerArgumentType.getInteger(context, "index") - 1;
        Optional<VillageData.Village> villageOptional = VillageData.getVillageByIndex(index);
        if (villageOptional.isEmpty()) {
            context.getSource().sendFailure(Component.literal("Invalid village index. Use /village list."));
            return 0;
        }

        VillageData.Village village = villageOptional.get();
        ServerLevel level = player.serverLevel();
        int y = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                new net.minecraft.core.BlockPos(village.blockX(), 0, village.blockZ())).getY();
        player.teleportTo(village.blockX() + 0.5D, y + 1.0D, village.blockZ() + 0.5D);
        context.getSource().sendSuccess(
                () -> Component.literal("Teleported to village #" + (index + 1) + " (" + village.blockX() + ", " + village.blockZ() + ")"),
                false
        );
        return 1;
    }

    private static int listVillages(CommandContext<CommandSourceStack> context) {
        List<VillageData.Village> villages = VillageData.getVillages();
        if (villages.isEmpty()) {
            context.getSource().sendFailure(Component.literal("No villages loaded yet. Wait for world load API sync."));
            return 0;
        }

        context.getSource().sendSuccess(() -> Component.literal("Village list (" + villages.size() + "):"), false);
        for (int i = 0; i < villages.size(); i++) {
            VillageData.Village village = villages.get(i);
            int displayIndex = i + 1;
            context.getSource().sendSuccess(
                    () -> Component.literal("#" + displayIndex + " block=(" + village.blockX() + ", " + village.blockZ()
                            + ") chunk=(" + village.chunkX() + ", " + village.chunkZ() + ")"),
                    false
            );
        }
        return 1;
    }

    private static int villageCount(CommandContext<CommandSourceStack> context) {
        int count = VillageData.getVillageCount();
        context.getSource().sendSuccess(() -> Component.literal("Total villages: " + count), false);
        return 1;
    }
}
