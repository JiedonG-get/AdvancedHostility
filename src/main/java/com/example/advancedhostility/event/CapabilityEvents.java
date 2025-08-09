package com.example.advancedhostility.event;

import com.example.advancedhostility.AdvancedHostility;
import com.example.advancedhostility.capability.HateCapability;
import com.example.advancedhostility.capability.IHate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * 这个类处理所有和能力（Capability）以及实体属性相关的事件。
 */
@Mod.EventBusSubscriber(modid = AdvancedHostility.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CapabilityEvents {

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(IHate.class);
    }

    /**
     * ★ 关键修复 ★
     * 使用 EntityAttributeModificationEvent 来为原版和平生物动态添加攻击属性。
     * 这是Forge推荐的标准做法，确保它们在逻辑上能够造成伤害。
     */
    @SubscribeEvent
    public static void onAttributeModify(EntityAttributeModificationEvent event) {
        AdvancedHostility.LOGGER.info("Adding custom attributes to vanilla entities for aggression rules...");

        // 将所有可能被 aggression_rules 影响的和平生物或非标准攻击生物列在这里
        List<EntityType<? extends LivingEntity>> entitiesToModify = List.of(
                EntityType.SHEEP, EntityType.COW, EntityType.PIG, EntityType.CHICKEN,
                EntityType.RABBIT, EntityType.BAT, EntityType.VILLAGER, EntityType.WANDERING_TRADER,
                EntityType.HORSE, EntityType.DONKEY, EntityType.MULE, EntityType.LLAMA,
                EntityType.COD, EntityType.SALMON, EntityType.TROPICAL_FISH, EntityType.SQUID,
                EntityType.GLOW_SQUID, EntityType.SLIME // 史莱姆的攻击力比较特殊，确保它有标准属性
        );

        for (EntityType<? extends LivingEntity> entityType : entitiesToModify) {
            // 检查这个实体类型是否已经有攻击属性，没有才添加
            if (!event.has(entityType, Attributes.ATTACK_DAMAGE)) {
                // 为该实体类型添加 ATTACK_DAMAGE 属性，基础值为 0.0 或 1.0。
                // 具体的攻击力数值将在 onLivingHurt 事件中根据配置实时设置。
                event.add(entityType, Attributes.ATTACK_DAMAGE, 1.0D);
                AdvancedHostility.LOGGER.debug("Added ATTACK_DAMAGE attribute to: {}", entityType.getDescriptionId());
            }
        }
        AdvancedHostility.LOGGER.info("Custom attribute addition complete.");
    }

    // 这个内部类处理Forge事件总线上的事件
    @Mod.EventBusSubscriber(modid = AdvancedHostility.MOD_ID)
    public static class ForgeBusEvents {
        @SubscribeEvent
        public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
            if (event.getObject() instanceof LivingEntity) {
                event.addCapability(HateCapability.ID, new HateCapability.Provider());
            }
        }
    }
}