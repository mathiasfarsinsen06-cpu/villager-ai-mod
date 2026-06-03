package com.mathiasfar.villagermod.village;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Village {
    private UUID id;
    private String name;
    private int x, y, z;
    private int population;
    private double prosperity;
    private String leader;
    private List<String> resources;
    private List<String> alliances;
    private List<String> enemies;
    private long lastAIUpdate;

    public Village(String name, int x, int y, int z) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.population = 10;
        this.prosperity = 50.0;
        this.leader = "Elder_" + name;
        this.resources = new ArrayList<>();
        this.alliances = new ArrayList<>();
        this.enemies = new ArrayList<>();
        this.lastAIUpdate = System.currentTimeMillis();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public String getName() { return name; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
    public int getPopulation() { return population; }
    public double getProsperity() { return prosperity; }
    public String getLeader() { return leader; }
    public List<String> getResources() { return resources; }
    public List<String> getAlliances() { return alliances; }
    public List<String> getEnemies() { return enemies; }
    public long getLastAIUpdate() { return lastAIUpdate; }

    public void setPopulation(int population) { this.population = population; }
    public void setProsperity(double prosperity) { this.prosperity = prosperity; }
    public void setLeader(String leader) { this.leader = leader; }
    public void setLastAIUpdate(long time) { this.lastAIUpdate = time; }

    @Override
    public String toString() {
        return "Village{" +
                "name='" + name + '\'' +
                ", pos=(" + x + "," + y + "," + z + ")" +
                ", population=" + population +
                ", prosperity=" + prosperity +
                ", leader='" + leader + '\'' +
                '}';
    }
}