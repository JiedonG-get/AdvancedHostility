package com.example.advancedhostility.mixins;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Predicate;

@Mixin(TargetingConditions.class)
public interface TargetingConditionsAccessor {
    /**
     * This accessor generates a public getter for the private 'selector' field at runtime.
     */
    @Accessor("selector")
    Predicate<LivingEntity> getSelector();
}
