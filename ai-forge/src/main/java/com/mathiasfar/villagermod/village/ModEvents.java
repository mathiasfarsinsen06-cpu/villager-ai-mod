package com.mathiasfar.villagermod.village;

import com.mathiasfar.villagermod.AIVillagesMod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public final class ModEvents {
    private static boolean registered;

    private ModEvents() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        IEventBus forgeBus = MinecraftForge.EVENT_BUS;
        forgeBus.addListener(ModEvents::onRegisterCommands);
        forgeBus.addListener(WorldLoadHandler::onLevelLoad);
        registered = true;
        AIVillagesMod.LOGGER.info("Village events registered");
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        VillageTPCommand.register(event.getDispatcher());
        AIVillagesMod.LOGGER.info("/village command registered");
    }
}
