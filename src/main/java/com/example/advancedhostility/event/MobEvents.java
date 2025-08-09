package com.example.advancedhostility.event;

import com.example.advancedhostility.AdvancedHostility;
import com.example.advancedhostility.config.ConfigManager;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = AdvancedHostility.MOD_ID)
public class MobEvents {

    private static final UUID AGGRESSION_SPEED_MODIFIER_ID = UUID.fromString("b8b2f296-8d4e-4c8a-9c2b-a82b9b5e5f3f");

    /**
     * Centralized logic to apply or remove aggression rules from a mob.
     * It first cleans up old AI goals and then applies new ones based on the current config.
     * @param mob The mob to apply rules to.
     */
    public static void applyAggressionRules(Mob mob) {
        String mobId = ConfigManager.getEntityIdString(mob);
        Double configuredDamage = ConfigManager.getAggressionDamage(mobId);

        // ★ 关键修复：如果生物不在 aggression_rules 中，则立即返回，不做任何操作！
        // 这可以防止该方法错误地清除原版怪物的AI。
        if (configuredDamage == null) {
            return;
        }

        // --- 仅当生物在 aggression_rules 中时，才执行以下AI重构 ---

        // 1. 清理旧AI
        Set<Goal> goalsToRemove = mob.goalSelector.getAvailableGoals().stream()
                .map(WrappedGoal::getGoal)
                .filter(goal -> goal instanceof MeleeAttackGoal)
                .collect(Collectors.toSet());
        goalsToRemove.forEach(mob.goalSelector::removeGoal);

        Set<Goal> targetGoalsToRemove = mob.targetSelector.getAvailableGoals().stream()
                .map(WrappedGoal::getGoal)
                .filter(goal -> goal instanceof HurtByTargetGoal || goal instanceof NearestAttackableTargetGoal)
                .collect(Collectors.toSet());
        targetGoalsToRemove.forEach(mob.targetSelector::removeGoal);

        // 2. 应用新规则
        if (mob instanceof PathfinderMob pathfinderMob) {
            AttributeInstance attackAttribute = pathfinderMob.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attackAttribute != null) {
                // 这个设置是可选的，因为我们会在LivingHurtEvent中强制设定最终伤害
                // 但设置一个基础值是个好习惯
                attackAttribute.setBaseValue(1.0D);
            }

            AdvancedHostility.LOGGER.debug("Applying/Refreshing aggression AI for {}", mobId);
            pathfinderMob.goalSelector.addGoal(1, new MeleeAttackGoal(pathfinderMob, 1.2D, false));
            pathfinderMob.targetSelector.addGoal(1, new HurtByTargetGoal(pathfinderMob));
            pathfinderMob.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(pathfinderMob, LivingEntity.class, true, (target) -> {
                return ConfigManager.getFinalRelationship(mobId, ConfigManager.getEntityIdString(target)) == ConfigManager.Relationship.HOSTILE;
            }));
        }
    }

    @SubscribeEvent
    public static void onEntitySpawn(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob)) return;

        // 步骤1：应用“攻击性改造”规则。
        // 这主要用于将和平生物（如鸡、村民）改造为攻击性生物。
        applyAggressionRules(mob);

        // ★ ======================= 兼容性注入逻辑 START ======================= ★

        // 步骤2：为所有在我们的配置中定义了阵营的生物，尝试注入一个标准的索敌AI。
        // 这是为了让其他模组的生物也能理解我们的阵营和敌对关系。

        String mobId = ConfigManager.getEntityIdString(mob);
        String mobFaction = ConfigManager.getFaction(mobId);

        // 只有当这个生物被定义了阵营，我们才需要为它注入AI
        if (mobFaction != null) {
            // 确保它是可寻路的生物
            if (mob instanceof PathfinderMob pathfinderMob) {

                // 创建一个基于我们配置的、标准的索敌AI
                // 优先级设为5，一个中等偏后的值，尽量不与该生物自带的核心AI冲突
                NearestAttackableTargetGoal<LivingEntity> customGoal = new NearestAttackableTargetGoal<>(
                        pathfinderMob,
                        LivingEntity.class,
                        5,        // checkRate: 每5个tick检查一次
                        true,     // mustSee: 需要可见
                        false,    // mustReach: 不需要能到达
                        (target) -> ConfigManager.getFinalRelationship(mobId, ConfigManager.getEntityIdString(target)) == ConfigManager.Relationship.HOSTILE
                );

                // 将我们自己的AI目标注入到该生物的目标选择器中
                pathfinderMob.targetSelector.addGoal(4, customGoal); // AI任务的优先级设为4
                AdvancedHostility.LOGGER.debug("Injected custom targeting AI for modded mob: {}", mobId);
            }
        }
        // ★ ======================= 兼容性注入逻辑 END ========================= ★
    }

    @SubscribeEvent(priority = EventPriority.HIGH) // 使用高优先级，确保我们的伤害修改先于其他模组
    public static void onLivingHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        // 确保伤害来源是一个实体
        if (!(source.getEntity() instanceof LivingEntity attacker)) {
            return;
        }

        // --- ★ 伤害覆盖逻辑 (修复后) ★ ---
        String attackerId = ConfigManager.getEntityIdString(attacker);
        Double configuredDamage = ConfigManager.getAggressionDamage(attackerId);

        // 如果这个攻击者在我们的 aggression_rules 中有定义，就强制设置伤害
        if (configuredDamage != null && configuredDamage > 0) {
            // 使用 event.setAmount() 来覆盖本次攻击的最终伤害值
            event.setAmount(configuredDamage.floatValue());
        }
        // 如果 configuredDamage 为 null (即规则未定义)，我们什么都不做，让原版伤害计算流程继续。


        // --- 友军伤害逻辑 (保持不变，但现在更可靠) ---
        LivingEntity victim = event.getEntity();

        // 在检查友伤之前，先检查事件是否已经被取消，或者伤害是否为0
        if (event.isCanceled() || event.getAmount() <= 0) {
            return;
        }

        ConfigManager.Relationship finalRelationship = ConfigManager.getFinalRelationship(attackerId, ConfigManager.getEntityIdString(victim));

        if (finalRelationship == ConfigManager.Relationship.FRIENDLY) {
            if (!ConfigManager.isFriendlyFireAllowed(attacker)) {
                event.setCanceled(true); // 取消事件，伤害和受击反馈都会消失
                return;
            }
        }

        // --- 同盟支援逻辑 (保持不变) ---
        if (!ConfigManager.isAllianceEnabled()) return;

        String victimFaction = ConfigManager.getFaction(ConfigManager.getEntityIdString(victim));
        if (victimFaction == null) return;

        double range = ConfigManager.getAllianceResponseRange();
        AABB searchBox = victim.getBoundingBox().inflate(range);
        List<Mob> nearbyMobs = victim.level().getEntitiesOfClass(Mob.class, searchBox);

        for (Mob potentialAlly : nearbyMobs) {
            if (potentialAlly == attacker || potentialAlly == victim || !potentialAlly.isAlive()) continue;

            String allyFaction = ConfigManager.getFaction(ConfigManager.getEntityIdString(potentialAlly));
            if (ConfigManager.getRelationship(victimFaction, allyFaction) == ConfigManager.Relationship.FRIENDLY) {
                potentialAlly.setTarget(attacker);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingTickEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide) return;

        // --- Speed Control Logic ---
        String mobId = ConfigManager.getEntityIdString(mob);
        Double attackSpeedMultiplier = ConfigManager.getAggressionSpeed(mobId);
        if (attackSpeedMultiplier != null) {
            AttributeInstance speedAttribute = mob.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speedAttribute != null) {
                AttributeModifier existingModifier = speedAttribute.getModifier(AGGRESSION_SPEED_MODIFIER_ID);
                if (mob.getTarget() != null && mob.getTarget().isAlive()) {
                    if (existingModifier == null) {
                        final AttributeModifier speedModifier = new AttributeModifier(
                                AGGRESSION_SPEED_MODIFIER_ID, "Aggression speed boost",
                                attackSpeedMultiplier - 1.0, AttributeModifier.Operation.MULTIPLY_BASE
                        );
                        speedAttribute.addPermanentModifier(speedModifier);
                    }
                } else {
                    if (existingModifier != null) {
                        speedAttribute.removeModifier(existingModifier);
                    }
                }
            }
        }

        // --- Fix Stuck Attack State Logic ---
        if (mob.getTarget() != null && !mob.getTarget().isAlive()) {
            mob.setTarget(null);
        }

        // --- Proactive Targeting & State Reset Logic ---
        if (mob.tickCount % 20 == 0) {
            if (mob.getTarget() == null || !mob.getTarget().isAlive()) {
                if (mob.isAggressive()) {
                    mob.setAggressive(false);
                }
                ConfigManager.MobRuleData rule = ConfigManager.getRuleForMob(mobId);
                if (rule != null && !rule.proactive().isEmpty()) {
                    AABB searchBox = mob.getBoundingBox().inflate(16.0);
                    List<LivingEntity> potentialTargets = mob.level().getEntitiesOfClass(LivingEntity.class, searchBox, e -> e.isAlive() && e != mob);
                    for (LivingEntity potentialTarget : potentialTargets) {
                        String potentialTargetId = ConfigManager.getEntityIdString(potentialTarget);
                        if (rule.proactive().contains(potentialTargetId)) {
                            mob.setTarget(potentialTarget);
                            break;
                        }
                    }
                }
            }
        }
    }
}
