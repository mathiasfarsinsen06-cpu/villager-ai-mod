package com.mathiasfar.villagermod.network;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class VillageDataSync {
    private static final String SERVER_URL = "https://village-server-5io8.onrender.com";
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final Gson gson = new Gson();

    /**
     * Send seed til server og få villages tilbage
     */
    public static JsonArray getVillagesBySeed(String seed) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(SERVER_URL + "/api/villages?seed=" + seed))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return gson.fromJson(response.body(), JsonArray.class);
        }
        throw new Exception("Server svar: " + response.statusCode());
    }

    /**
     * Tjek om serveren er live
     */
    public static boolean isServerHealthy() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(SERVER_URL + "/api/villages/health"))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}