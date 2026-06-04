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
    private static final String VILLAGE_API = "https://fictional-spoon-69q5vjprxjxp3rwq.github.dev/api/villages";
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();
    private static final Gson gson = new Gson();

    public AIVillagesMod() {
        LOGGER.info("AIVillages mod initializing...");
        com.mathiasfar.villagermod.village.ModEvents.register();
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
                                                            () -> Component.literal("🏠 Source: Village Server API"), false);
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

            // Call Village Server API
            String url = String.format("%s?seed=%d", VILLAGE_API, seed);
            LOGGER.info("📡 Kalder API: {}", url);

            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI(url))
                        .GET()
                        .timeout(java.time.Duration.ofSeconds(60))
                        .build();

                LOGGER.info("📤 Sender HTTP request...");
                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                LOGGER.info("📥 API respons status: {}", response.statusCode());
                LOGGER.info("📥 API respons body længde: {} bytes", response.body().length());

                // Log first 500 chars of response
                String bodyPreview = response.body().substring(0, Math.min(500, response.body().length()));
                LOGGER.info("📥 API respons preview: {}", bodyPreview);

                // Log redirect info if applicable
                if (response.statusCode() >= 300 && response.statusCode() < 400) {
                    response.headers().firstValue("location")
                            .ifPresent(location -> LOGGER.info("📍 Redirect location: {}", location));
                }

                if (response.statusCode() == 200) {
                    try {
                        JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
                        LOGGER.info("✅ JSON parsed successfully");

                        if (jsonResponse.has("villages")) {
                            JsonArray villagesArray = jsonResponse.getAsJsonArray("villages");
                            LOGGER.info("🏘️ Found {} villages in API response", villagesArray.size());

                            for (int i = 0; i < villagesArray.size(); i++) {
                                JsonObject village = villagesArray.get(i).getAsJsonObject();

                                int x = village.get("blockX").getAsInt();
                                int z = village.get("blockZ").getAsInt();

                                // Check if already added
                                boolean exists = foundVillages.stream()
                                        .anyMatch(v -> v.getX() == x && v.getZ() == z);

                                if (!exists) {
                                    // ✅ FAST - Find the safe Y coordinate
                                    int safeY = findSafeY(level, x, z);
                                    BlockPos villagePos = new BlockPos(x, safeY, z);
                                    foundVillages.add(villagePos);

                                    LOGGER.info("✅ Found village at: {} {} (Y: {})", x, z, safeY);
                                }
                            }
                        } else {
                            LOGGER.warn("⚠️ 'villages' array not found in response");
                        }
                    } catch (com.google.gson.JsonSyntaxException jse) {
                        LOGGER.error("❌ JSON parsing failed - response is not valid JSON", jse);
                        LOGGER.error("Full response body: {}", response.body());
                        source.sendFailure(Component.literal("❌ API Fejl: Invalid JSON response"));
                    }
                } else {
                    LOGGER.error("❌ API returned status: {}", response.statusCode());
                    source.sendFailure(Component.literal("❌ API Fejl: Status " + response.statusCode()));
                }
            } catch (java.net.ConnectException ce) {
                LOGGER.error("❌ Connection failed - is server running on {}?", VILLAGE_API, ce);
                source.sendFailure(Component.literal("❌ API Fejl: Kan ikke forbinde til " + VILLAGE_API));
            } catch (Exception e) {
                LOGGER.error("❌ API request failed with exception: ", e);
                LOGGER.error("Exception type: {}", e.getClass().getName());
                LOGGER.error("Exception message: {}", e.getMessage());
                e.printStackTrace();
                String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                source.sendFailure(Component.literal("❌ API Fejl: " + errorMsg));
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

            LOGGER.info("✅ Søgning færdig! Fandt {} byer", foundVillages.size());
        } catch (Exception e) {
            LOGGER.error("❌ Error searching for villages: {}", e.getMessage());
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
