package com.mathiasfar.villagermod;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.level.BlockEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod.EventBusSubscriber(modid = "aivillages", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BlockPlaceListener {
    private static final Logger LOGGER = LoggerFactory.getLogger("aivillages");

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        LOGGER.info("Block placed event triggered");
    }
}