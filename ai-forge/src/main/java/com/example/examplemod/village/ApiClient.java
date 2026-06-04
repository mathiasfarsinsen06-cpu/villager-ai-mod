package com.example.examplemod.village;

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

    private ApiClient() {
    }

    public static List<VillageData.Village> fetchVillages(long seed) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format(VILLAGE_API_TEMPLATE, seed)))
                .timeout(Duration.ofSeconds(60))
                .GET()
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Village API returned status " + response.statusCode());
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
