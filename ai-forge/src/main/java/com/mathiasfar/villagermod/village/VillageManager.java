package com.mathiasfar.villagermod.village;

import com.google.gson.Gson;
import com.mathiasfar.villagermod.AIVillagesMod;
import com.mathiasfar.villagermod.network.CloudflareAPI;

import java.util.*;

public class VillageManager {
    private Map<UUID, Village> villages = new HashMap<>();
    private Gson gson = new Gson();
    private Random random = new Random();

    /**
     * Tilføj en ny by
     */
    public void addVillage(String name, int x, int y, int z) {
        Village village = new Village(name, x, y, z);
        villages.put(village.getId(), village);
        AIVillagesMod.LOGGER.info("Village created: " + village);
    }

    /**
     * Find by baseret på koordinater
     */
    public Village findVillageAtChunk(int chunkX, int chunkZ) {
        for (Village village : villages.values()) {
            if (village.getX() >> 4 == chunkX && village.getZ() >> 4 == chunkZ) {
                return village;
            }
        }
        return null;
    }

    /**
     * Hent alle byer
     */
    public Collection<Village> getAllVillages() {
        return villages.values();
    }

    /**
     * Generer byer baseret på seed
     */
    public void generateVillagesFromSeed(long worldSeed) {
        Random seededRandom = new Random(worldSeed);

        int numVillages = seededRandom.nextInt(5) + 3; // 3-8 byer

        for (int i = 0; i < numVillages; i++) {
            int x = seededRandom.nextInt(5000) - 2500;
            int z = seededRandom.nextInt(5000) - 2500;
            int y = 64;

            addVillage("Village_" + (i + 1), x, y, z);
        }

        AIVillagesMod.LOGGER.info("Generated " + numVillages + " villages from seed");
    }

    /**
     * Lad AI tænke og træffe beslutninger
     */
    public void updateAI() {
        for (Village village : villages.values()) {
            // Send til AI via Cloudflare Workers
            String villageJson = gson.toJson(village);

            CloudflareAPI.sendVillageDataToAI(villageJson).thenAccept(response -> {
                if (response != null) {
                    AIVillagesMod.LOGGER.info("AI decision for " + village.getName() + ": " + response);
                    // Parse response og update village state
                }
            });
        }
    }

    /**
     * Get village info
     */
    public String getVillageInfo(UUID villageId) {
        Village village = villages.get(villageId);
        if (village != null) {
            return gson.toJson(village);
        }
        return null;
    }
}