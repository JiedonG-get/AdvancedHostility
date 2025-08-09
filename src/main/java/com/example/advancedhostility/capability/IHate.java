package com.example.advancedhostility.capability;

import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.UUID;

public interface IHate {
    Map<UUID, Double> getHateMap();
    void setHate(LivingEntity target, double amount);
    double getHate(LivingEntity target);
    void decayHate(double amount);
    void clear();
}
