package com.mathiasfar.villagermod.village;

import com.google.gson.JsonObject;

public class VillageData {
    public String name;
    public int x, z;
    public int population;
    public double prosperity;
    public String leader;
    public int distance; // Fra Warden

    public VillageData(String name, int x, int z, int pop, double prosp, String leader) {
        this.name = name;
        this.x = x;
        this.z = z;
        this.population = pop;
        this.prosperity = prosp;
        this.leader = leader;
    }

    public void setDistance(int wardenX, int wardenZ) {
        this.distance = (int) Math.sqrt(Math.pow(x - wardenX, 2) + Math.pow(z - wardenZ, 2));
    }

    @Override
    public String toString() {
        return String.format("📍 %s | Pop: %d | Prosperity: %.1f | Distance: %d blocks",
                name, population, prosperity, distance);
    }
}