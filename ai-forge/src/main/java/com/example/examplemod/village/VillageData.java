package com.example.examplemod.village;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class VillageData {
    private static final List<Village> VILLAGES = new ArrayList<>();

    private VillageData() {
    }

    public static synchronized void setVillages(List<Village> villages) {
        VILLAGES.clear();
        VILLAGES.addAll(villages);
    }

    public static synchronized List<Village> getVillages() {
        return List.copyOf(VILLAGES);
    }

    public static synchronized Optional<Village> getVillageByIndex(int index) {
        if (index < 0 || index >= VILLAGES.size()) {
            return Optional.empty();
        }
        return Optional.of(VILLAGES.get(index));
    }

    public static synchronized int getVillageCount() {
        return VILLAGES.size();
    }

    public static synchronized void clear() {
        VILLAGES.clear();
    }

    public record Village(int blockX, int blockZ, int chunkX, int chunkZ) {
    }
}
