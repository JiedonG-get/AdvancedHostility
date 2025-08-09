package com.example.advancedhostility.mixins;

import com.example.advancedhostility.config.ConfigManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobMixin {

    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void onSetTarget(LivingEntity pTarget, CallbackInfo ci) {
        Mob self = (Mob)(Object)this;

        if (pTarget == null) {
            return;
        }

        // ★ FIX: Use the new getFinalRelationship method to determine the TRUE relationship
        String selfId = ConfigManager.getEntityIdString(self);
        String targetId = ConfigManager.getEntityIdString(pTarget);
        ConfigManager.Relationship finalRelationship = ConfigManager.getFinalRelationship(selfId, targetId);

        // Only when the FINAL relationship is friendly, we check the friendly fire rule.
        // This ensures that a 'proactive' rule (which results in a HOSTILE relationship) will bypass this check.
        if (finalRelationship == ConfigManager.Relationship.FRIENDLY) {
            if (!ConfigManager.isFriendlyFireAllowed(self)) {
                ci.cancel();
            }
        }
    }
}
