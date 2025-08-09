package com.example.advancedhostility.capability;

import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Hate implements IHate {
    private final Map<UUID, Double> hateMap = new HashMap<>();
    @Override public Map<UUID, Double> getHateMap() { return this.hateMap; }
    @Override public void setHate(LivingEntity target, double amount) {
        if (amount <= 0) this.hateMap.remove(target.getUUID());
        else this.hateMap.put(target.getUUID(), amount);
    }
    @Override public double getHate(LivingEntity target) { return this.hateMap.getOrDefault(target.getUUID(), 0.0); }
    @Override public void decayHate(double amount) {
        hateMap.replaceAll((uuid, hate) -> Math.max(0, hate - amount));
        hateMap.values().removeIf(hate -> hate <= 0);
    }
    @Override public void clear() { this.hateMap.clear(); }
}
