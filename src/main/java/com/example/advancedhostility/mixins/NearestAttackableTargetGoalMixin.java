package com.example.advancedhostility.mixins;

import com.example.advancedhostility.config.ConfigManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

@Mixin(NearestAttackableTargetGoal.class)
public class NearestAttackableTargetGoalMixin {

    @Shadow @Final @Mutable protected TargetingConditions targetConditions;

    @Inject(method = "<init>(Lnet/minecraft/world/entity/Mob;Ljava/lang/Class;IZZLjava/util/function/Predicate;)V", at = @At("RETURN"))
    private void onInit(Mob pMob, Class<?> pTargetType, int pRandomInterval, boolean pMustSee, boolean pMustReach, Predicate<LivingEntity> pTargetPredicate, CallbackInfo ci) {

        final Predicate<LivingEntity> originalPredicate = ((TargetingConditionsAccessor) (Object) this.targetConditions).getSelector();

        Predicate<LivingEntity> newPredicate = (targetEntity) -> {
            String attackerId = getEntityIdString(pMob);
            String targetId = getEntityIdString(targetEntity);

            // ★ 关键修复：使用“仲裁者”逻辑
            ConfigManager.Relationship finalRelationship = ConfigManager.getFinalRelationship(attackerId, targetId);

            switch (finalRelationship) {
                // 1. 如果我们的配置明确规定为“敌对”，则强制允许攻击
                case HOSTILE:
                    return true;

                // 2. 如果我们的配置明确规定为“友好”或“中立”，则强制阻止攻击
                case FRIENDLY:
                case NEUTRAL:
                    return false;

                // 3. 如果我们的配置没有规定关系(VANILLA)，则完全交由原版逻辑判断
                case VANILLA:
                default:
                    // 调用原始的筛选条件，保持原版行为（例如，蜘蛛依然会攻击玩家）
                    if (originalPredicate != null) {
                        return originalPredicate.test(targetEntity);
                    }
                    // 如果连原始筛选条件都没有，则默认为true（这是原版AI的行为）
                    return true;
            }
        };
        // 应用我们新的、更智能的筛选逻辑
        this.targetConditions = this.targetConditions.copy().selector(newPredicate);
    }

    private String getEntityIdString(LivingEntity entity) {
        if (entity instanceof Player) return "player";
        return ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
    }
}