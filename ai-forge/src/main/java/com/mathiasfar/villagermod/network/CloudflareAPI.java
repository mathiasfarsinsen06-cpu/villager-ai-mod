package com.mathiasfar.villagermod.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mathiasfar.villagermod.AIVillagesMod;
import com.mathiasfar.villagermod.config.AIVillagesConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class CloudflareAPI {
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final Gson GSON = new Gson();

    /**
     * Send village data to Cloudflare Workers for AI processing
     */
    public static CompletableFuture<String> sendVillageDataToAI(String villageData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String apiUrl = AIVillagesConfig.CLOUDFLARE_API_URL;

                JsonObject payload = new JsonObject();
                payload.addProperty("action", "process_village");
                payload.addProperty("data", villageData);

                if (!AIVillagesConfig.API_KEY.isEmpty()) {
                    payload.addProperty("api_key", AIVillagesConfig.API_KEY);
                }

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl + "village-ai"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                        .build();

                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    AIVillagesMod.LOGGER.info("AI Response: " + response.body());
                    return response.body();
                } else {
                    AIVillagesMod.LOGGER.warn("API Error: " + response.statusCode());
                    return null;
                }
            } catch (Exception e) {
                AIVillagesMod.LOGGER.error("Failed to communicate with Cloudflare API", e);
                return null;
            }
        });
    }

    /**
     * Test forbindelse til Cloudflare Workers
     */
    public static CompletableFuture<Boolean> testConnection() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String apiUrl = AIVillagesConfig.CLOUDFLARE_API_URL;

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl + "health"))
                        .GET()
                        .build();

                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                boolean success = response.statusCode() == 200;

                AIVillagesMod.LOGGER.info("Cloudflare connection test: " + (success ? "SUCCESS" : "FAILED"));
                return success;
            } catch (Exception e) {
                AIVillagesMod.LOGGER.error("Connection test failed", e);
                return false;
            }
        });
    }
}