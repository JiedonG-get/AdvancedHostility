package com.example.advancedhostility.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.Collections;
import java.util.List;

/**
 * Defines the SERVER-side configuration options.
 * These settings are world-specific and will override the COMMON config.
 * The file is located in: saves/<world_name>/serverconfig/advancedhostility-server.toml
 */
public class ServerConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // --- Rule Definition Categories ---
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SETS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> FACTIONS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> MOB_RULES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> AGGRESSION_RULES;

    // --- Global Mechanics ---
    public static final ForgeConfigSpec.BooleanValue ENABLE_FRIENDLY_FIRE;

    // --- Alliance Mechanics ---
    public static final ForgeConfigSpec.BooleanValue ALLIANCE_ENABLE;
    public static final ForgeConfigSpec.DoubleValue ALLIANCE_RESPONSE_RANGE;


    static {
        BUILDER.comment(
                "====================================================================================",
                " Advanced Hostility - World-Specific Settings (Server Config)",
                "====================================================================================",
                " These settings are specific to THIS world save and will OVERRIDE the global settings",
                " found in the main 'config/advancedhostility-common.toml' file.",
                " If a list below is empty, the values from the common config will be used as a default.",
                " ",
                " 这些设置仅对此世界存档生效，并且会覆盖主配置文件 'config/advancedhostility-common.toml' 中的全局设置。",
                " 如果下面的列表为空，则会使用通用配置文件中的设置作为默认值。"
        ).push("documentation");
        BUILDER.pop();


        BUILDER.push("sets_override");
        SETS = BUILDER.comment(
                " ",
                "World-Specific Mob Sets / 世界专属生物集合",
                "If this list is not empty, it will COMPLETELY REPLACE the sets from the common config.",
                "如果此列表不为空，它将完全替换通用配置中的生物集合。"
        ).defineList("sets", Collections.emptyList(), obj -> obj instanceof String);
        BUILDER.pop();


        BUILDER.push("rules_override");
        FACTIONS = BUILDER.comment(
                " ",
                "World-Specific Faction Rules / 世界专属阵营规则",
                "If this list is not empty, it will COMPLETELY REPLACE the factions from the common config.",
                "如果此列表不为空，它将完全替换通用配置中的阵营规则。"
        ).defineList("factions", Collections.emptyList(), obj -> obj instanceof String);

        MOB_RULES = BUILDER.comment(
                " ",
                "World-Specific Mob Rules / 世界专属独立生物规则",
                "If this list is not empty, it will COMPLETELY REPLACE the mob rules from the common config.",
                "如果此列表不为空，它将完全替换通用配置中的独立生物规则。"
        ).defineList("mob_rules", Collections.emptyList(), obj -> obj instanceof String);

        AGGRESSION_RULES = BUILDER.comment(
                " ",
                "World-Specific Aggression Rules / 世界专属攻击性改造规则",
                "If this list is not empty, it will COMPLETELY REPLACE the aggression rules from the common config.",
                "如果此列表不为空，它将完全替换通用配置中的攻击性改造规则。"
        ).defineList("aggression_rules", Collections.emptyList(), obj -> obj instanceof String);
        BUILDER.pop();


        BUILDER.push("global_settings_override");
        ENABLE_FRIENDLY_FIRE = BUILDER.comment(
                " ",
                "World-Specific Global Settings / 世界专属全局设置",
                "This value will override the common config's setting for this world.",
                "此设置将覆盖此世界中的通用配置。",
                "enable_friendly_fire: Globally allow (true) or disallow (false) friendly fire.",
                "全局允许(true)或禁止(false)友军伤害。"
        ).define("enable_friendly_fire", false); // ★ FIX: Provide a hardcoded default value
        BUILDER.pop();


        BUILDER.push("alliance_mechanics_override");
        ALLIANCE_ENABLE = BUILDER.comment(
                " ",
                "World-Specific Alliance Mechanics / 世界专属同盟机制",
                "enable: Enable or disable the alliance support system for this world.",
                "是否为此世界启用同盟支援系统。"
        ).define("enable", true); // ★ FIX: Provide a hardcoded default value
        ALLIANCE_RESPONSE_RANGE = BUILDER.comment(
                "response_range: The maximum range (in blocks) for allies to respond in this world.",
                "此世界中盟友能响应支援请求的最大范围（方块）。"
        ).defineInRange("response_range", 40.0, 0.0, 256.0); // ★ FIX: Provide a hardcoded default value
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
