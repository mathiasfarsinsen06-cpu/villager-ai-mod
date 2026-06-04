package com.mathiasfar.villagermod.village;

import com.mathiasfar.villagermod.AIVillagesMod;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class ApiClient {
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final String VILLAGE_API_TEMPLATE = "http://localhost:8080/api/villages?seed=%d";
    private static final Duration API_TIMEOUT = Duration.ofSeconds(60);
    private static final int RESPONSE_BODY_LOG_LIMIT = 300;

    private ApiClient() {
    }

    public static List<VillageData.Village> fetchVillages(long seed) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format(VILLAGE_API_TEMPLATE, seed)))
                .timeout(API_TIMEOUT)
                .GET()
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            String responseBody = response.body();
            if (responseBody != null && responseBody.length() > RESPONSE_BODY_LOG_LIMIT) {
                responseBody = responseBody.substring(0, RESPONSE_BODY_LOG_LIMIT) + "...";
            }
            throw new IOException("Village API returned status " + response.statusCode() + " with body: " + responseBody);
        }

        JSONObject body = new JSONObject(response.body());
        JSONArray villages = body.optJSONArray("villages");
        if (villages == null) {
            return List.of();
        }

        List<VillageData.Village> parsedVillages = new ArrayList<>(villages.length());
        for (int i = 0; i < villages.length(); i++) {
            JSONObject village = villages.optJSONObject(i);
            if (village == null) {
                continue;
            }

            if (!village.has("blockX") || !village.has("blockZ")) {
                continue;
            }

            int blockX = village.getInt("blockX");
            int blockZ = village.getInt("blockZ");
            int chunkX = village.optInt("chunkX", blockX >> 4);
            int chunkZ = village.optInt("chunkZ", blockZ >> 4);
            parsedVillages.add(new VillageData.Village(blockX, blockZ, chunkX, chunkZ));
        }

        AIVillagesMod.LOGGER.info("Fetched {} villages from API for seed {}", parsedVillages.size(), seed);
        return parsedVillages;
    }
}
