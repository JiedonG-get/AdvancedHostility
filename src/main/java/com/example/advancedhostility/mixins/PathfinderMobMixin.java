package com.example.advancedhostility.mixins;

import com.example.advancedhostility.AdvancedHostility;
import com.example.advancedhostility.config.ConfigManager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

/**
 * 这个 Mixin 现在只负责修改AI，不再设置属性值。
 */
@Mixin(Mob.class)
public abstract class PathfinderMobMixin {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onMobConstructor(EntityType<?> p_21364_, Level p_21365_, CallbackInfo ci) {
        Mob self = (Mob)(Object)this;
        String mobId = ConfigManager.getEntityIdString(self);

        // 检查这个生物是否在 aggression_rules 中
        if (ConfigManager.getAggressionDamage(mobId) != null) {
            if (self instanceof PathfinderMob pathfinderMob) {

                AdvancedHostility.LOGGER.debug("Augmenting AI for {}.", mobId);

                // --- AI增强：植入新的高优先级AI任务 ---
                pathfinderMob.goalSelector.addGoal(1, new MeleeAttackGoal(pathfinderMob, 1.2D, false));
                pathfinderMob.targetSelector.addGoal(1, new HurtByTargetGoal(pathfinderMob));

                String mobFaction = ConfigManager.getFaction(mobId);
                Predicate<LivingEntity> hostilePredicate = (target) -> {
                    if (mobFaction == null) return target instanceof Player;

                    String targetId = ConfigManager.getEntityIdString(target);
                    String targetFaction = ConfigManager.getFaction(targetId);
                    return ConfigManager.getRelationship(mobFaction, targetFaction) == ConfigManager.Relationship.HOSTILE;
                };
                pathfinderMob.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(pathfinderMob, LivingEntity.class, true, hostilePredicate));

                // ★ 关键修改 ★：设置攻击力的逻辑已从此文件移除。
            }
        }
    }
}