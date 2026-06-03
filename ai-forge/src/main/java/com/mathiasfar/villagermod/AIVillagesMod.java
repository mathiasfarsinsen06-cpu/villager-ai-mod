package com.mathiasfar.villagermod;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@Mod("aivillages")
public class AIVillagesMod {
    public static final String MODID = "aivillages";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);
    private static boolean commandsRegistered = false;
    private static int currentVillageIndex = 0;
    private static List<BlockPos> foundVillages = new ArrayList<>();
    private static boolean isSearching = false;
    private static final String OLELA_API = "https://api.olela.me/v1/structures";
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final Gson gson = new Gson();

    public AIVillagesMod() {
        LOGGER.info("AIVillages mod initializing...");
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent
        public static void onCommonSetup(FMLCommonSetupEvent event) {
            LOGGER.info("AIVillages common setup complete!");
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ServerEvents {
        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase == TickEvent.Phase.START && !commandsRegistered) {
                try {
                    CommandDispatcher<CommandSourceStack> dispatcher =
                            event.getServer().getCommands().getDispatcher();

                    dispatcher.register(
                            Commands.literal("aivillages")
                                    .executes(context -> {
                                        context.getSource().sendSuccess(
                                                () -> Component.literal("━━━━━ AI VILLAGES ━━━━━"), false);
                                        context.getSource().sendSuccess(
                                                () -> Component.literal("Use: /aivillages list | next | info"), false);
                                        return 1;
                                    })
                                    .then(Commands.literal("list")
                                            .executes(context -> {
                                                try {
                                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                                    ServerLevel level = player.serverLevel();

                                                    if (isSearching) {
                                                        context.getSource().sendSuccess(
                                                                () -> Component.literal("⏳ Søger efter byer..."), false);
                                                        return 0;
                                                    }

                                                    context.getSource().sendSuccess(
                                                            () -> Component.literal("⏳ Søger hele verden efter byer..."), false);

                                                    // Fetch async
                                                    new Thread(() -> {
                                                        findVillagesAsync(player, level, context.getSource());
                                                    }).start();

                                                    return 1;
                                                } catch (Exception e) {
                                                    LOGGER.error("Error in list command", e);
                                                    return 0;
                                                }
                                            }))
                                    .then(Commands.literal("next")
                                            .executes(context -> {
                                                try {
                                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                                    ServerLevel level = player.serverLevel();

                                                    if (foundVillages.isEmpty()) {
                                                        context.getSource().sendSuccess(
                                                                () -> Component.literal("❌ Ingen byer fundet! Brug /aivillages list først"), false);
                                                        return 0;
                                                    }

                                                    BlockPos village = foundVillages.get(currentVillageIndex % foundVillages.size());

                                                    int safeY = findSafeY(level, village.getX(), village.getZ());

                                                    double dist = Math.sqrt(
                                                            Math.pow(player.getX() - village.getX(), 2) +
                                                                    Math.pow(player.getZ() - village.getZ(), 2)
                                                    );

                                                    player.teleportTo(
                                                            village.getX() + 0.5,
                                                            safeY + 1.0,
                                                            village.getZ() + 0.5
                                                    );

                                                    context.getSource().sendSuccess(
                                                            () -> Component.literal(String.format("✅ TP til: Village %d / %d | Pos: (%d, %d) | Dist: %.0f",
                                                                    currentVillageIndex + 1, foundVillages.size(), village.getX(), village.getZ(), dist)), false);

                                                    currentVillageIndex++;
                                                    return 1;
                                                } catch (Exception e) {
                                                    LOGGER.error("TP failed", e);
                                                    return 0;
                                                }
                                            }))
                                    .then(Commands.literal("info")
                                            .executes(context -> {
                                                try {
                                                    if (foundVillages.isEmpty()) {
                                                        context.getSource().sendSuccess(
                                                                () -> Component.literal("❌ Ingen by valgt!"), false);
                                                        return 0;
                                                    }

                                                    BlockPos village = foundVillages.get((currentVillageIndex - 1) % foundVillages.size());

                                                    context.getSource().sendSuccess(
                                                            () -> Component.literal("📍 Village Info"), false);
                                                    context.getSource().sendSuccess(
                                                            () -> Component.literal("📍 Position: " + village), false);
                                                    context.getSource().sendSuccess(
                                                            () -> Component.literal("🏠 Source: OlelaFinder API"), false);
                                                    return 1;
                                                } catch (Exception e) {
                                                    LOGGER.error("Info command failed", e);
                                                    return 0;
                                                }
                                            })
                                    ));

                    commandsRegistered = true;
                    LOGGER.info("✅ All commands registered successfully!");
                } catch (Exception e) {
                    LOGGER.error("❌ Failed to register commands", e);
                }
            }
        }
    }

    private static void findVillagesAsync(ServerPlayer player, ServerLevel level, CommandSourceStack source) {
        if (isSearching) return;
        isSearching = true;
        foundVillages.clear();
        currentVillageIndex = 0;

        try {
            long seed = level.getSeed();
            BlockPos playerPos = player.blockPosition();

            LOGGER.info("🔍 Søger hele verden for byer med seed: {}", seed);

            // Search hele verden - scan fra center og ud
            int gridSize = 500; // Scan hver 500 blok
            int searchDistance = 10000; // Søg 10km væk
            int scanCount = 0;
            int apiCalls = 0;

            for (int gx = -searchDistance; gx <= searchDistance; gx += gridSize) {
                for (int gz = -searchDistance; gz <= searchDistance; gz += gridSize) {
                    int searchX = gx;
                    int searchZ = gz;

                    scanCount++;
                    apiCalls++;

                    String url = String.format("%s?seed=%d&x=%d&z=%d&type=village",
                            OLELA_API, seed, searchX, searchZ);

                    try {
                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(new URI(url))
                                .GET()
                                .timeout(java.time.Duration.ofSeconds(5))
                                .build();

                        HttpResponse<String> response = httpClient.send(request,
                                HttpResponse.BodyHandlers.ofString());

                        if (response.statusCode() == 200) {
                            JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);

                            if (jsonResponse.has("structures")) {
                                JsonArray structuresArray = jsonResponse.getAsJsonArray("structures");

                                for (int i = 0; i < structuresArray.size(); i++) {
                                    JsonObject structure = structuresArray.get(i).getAsJsonObject();

                                    int x = structure.get("x").getAsInt();
                                    int z = structure.get("z").getAsInt();

                                    // Check if already added
                                    boolean exists = foundVillages.stream()
                                            .anyMatch(v -> v.getX() == x && v.getZ() == z);

                                    if (!exists) {
                                        BlockPos villagePos = new BlockPos(x, 64, z);
                                        foundVillages.add(villagePos);

                                        LOGGER.info("✅ Found village at: {} {}", x, z);
                                    }
                                }
                            }
                        } else {
                            LOGGER.debug("API returned status: {} for chunk {}, {}", response.statusCode(), searchX, searchZ);
                        }
                    } catch (Exception e) {
                        LOGGER.debug("Skipped chunk at {} {}: {}", searchX, searchZ, e.getMessage());
                    }

                    // Rate limiting - small delay
                    if (apiCalls % 10 == 0) {
                        Thread.sleep(500);
                    }
                }
            }

            // Sort by distance from player
            foundVillages.sort((a, b) -> Double.compare(
                    a.distSqr(playerPos),
                    b.distSqr(playerPos)
            ));

            // Report results
            source.sendSuccess(
                    () -> Component.literal("━━━━━ ALLE BYER FRA VERDEN ━━━━━"), false);

            if (foundVillages.isEmpty()) {
                source.sendSuccess(
                        () -> Component.literal("❌ Ingen byer fundet på denne seed"), false);
            } else {
                for (int i = 0; i < Math.min(foundVillages.size(), 30); i++) {
                    final int index = i;
                    BlockPos pos = foundVillages.get(i);
                    double dist = Math.sqrt(
                            Math.pow(player.getX() - pos.getX(), 2) +
                                    Math.pow(player.getZ() - pos.getZ(), 2)
                    );
                    source.sendSuccess(
                            () -> Component.literal(String.format("📍 Village %d | Pos: (%d, %d) | Dist: %.0f blokke",
                                    index + 1, pos.getX(), pos.getZ(), dist)), false);
                }
                if (foundVillages.size() > 30) {
                    source.sendSuccess(
                            () -> Component.literal(String.format("... og %d mere", foundVillages.size() - 30)), false);
                }
            }

            LOGGER.info("✅ Søgning færdig! Fandt {} byer (scannede {} chunks)", foundVillages.size(), scanCount);
        } catch (Exception e) {
            LOGGER.error("Error searching for villages: {}", e.getMessage());
            e.printStackTrace();
            source.sendFailure(Component.literal("❌ Fejl: " + e.getMessage()));
        } finally {
            isSearching = false;
        }
    }

    private static int findSafeY(ServerLevel level, int x, int z) {
        for (int y = level.getHeight() - 1; y >= level.getMinY(); y--) {
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.getBlockState(pos).isAir()) {
                return y;
            }
        }
        return 64;
    }
}
