package com.example.examplemod.village;

import com.mathiasfar.villagermod.AIVillagesMod;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.level.LevelEvent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class WorldLoadHandler {
    private static final Object LOAD_LOCK = new Object();
    private static final AtomicLong LAST_LOADED_SEED = new AtomicLong(Long.MIN_VALUE);
    private static final AtomicBoolean IS_LOADING = new AtomicBoolean(false);
    private static final AtomicLong ACTIVE_REQUEST_ID = new AtomicLong(0);

    private WorldLoadHandler() {
    }

    public static void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!Level.OVERWORLD.equals(serverLevel.dimension())) {
            return;
        }

        long seed = serverLevel.getSeed();
        long requestId;
        synchronized (LOAD_LOCK) {
            long previousSeed = LAST_LOADED_SEED.getAndSet(seed);
            if (previousSeed == seed && (IS_LOADING.get() || VillageData.getVillageCount() > 0)) {
                return;
            }

            requestId = ACTIVE_REQUEST_ID.incrementAndGet();
            IS_LOADING.set(true);
            VillageData.clear();
        }
        sendProgress(serverLevel, "§e[Village] Loading villages for seed: " + seed);

        CompletableFuture.supplyAsync(() -> {
            try {
                return ApiClient.fetchVillages(seed);
            } catch (Exception exception) {
                throw new CompletionException("Unable to fetch villages for seed " + seed, exception);
            }
        }).thenAccept(villages -> {
            if (ACTIVE_REQUEST_ID.get() != requestId) {
                return;
            }
            VillageData.setVillages(villages);
            IS_LOADING.set(false);
            sendProgress(serverLevel, "§a[Village] Loaded " + villages.size() + " villages from API.");
        }).exceptionally(exception -> {
            if (ACTIVE_REQUEST_ID.get() != requestId) {
                return null;
            }
            IS_LOADING.set(false);
            Throwable cause = exception.getCause() != null ? exception.getCause() : exception;
            AIVillagesMod.LOGGER.error("Failed to load villages for seed {}", seed, exception);
            sendProgress(serverLevel, "§c[Village] Failed to load villages: " + cause.getMessage());
            return null;
        });
    }

    private static void sendProgress(ServerLevel serverLevel, String message) {
        serverLevel.getServer().execute(
                () -> serverLevel.getServer().getPlayerList().broadcastSystemMessage(Component.literal(message), false)
        );
    }
}
