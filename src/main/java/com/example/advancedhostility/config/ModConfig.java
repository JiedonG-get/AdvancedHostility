package com.example.advancedhostility.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.Collections;
import java.util.List;

public class ModConfig {
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
                " Advanced Hostility & Faction System - Configuration",
                "====================================================================================",
                " Welcome! This file allows you to completely customize mob relationships and AI.",
                " For support, please visit our mod page.",
                " NOTE: This is the COMMON config. It provides default values for all worlds.",
                " You can create a SERVER config in 'saves/<world_name>/serverconfig/' to override",
                " these settings for a specific world.",
                " ",
                " 欢迎！此文件允许您完全自定义生物关系和AI。",
                " 如需支持，请访问我们的Mod主页。",
                " 注意：这是通用配置文件，为所有世界提供默认值。",
                " 你可以在 'saves/<世界存档名>/serverconfig/' 文件夹中创建一个服务器配置文件，",
                " 以便为特定世界覆盖这些设置。"
        ).push("documentation");
        BUILDER.pop();


        BUILDER.push("sets");
        SETS = BUILDER.comment(
                " ",
                "====================================================================================",
                " 1. Mob Sets / 生物集合",
                "====================================================================================",
                " Define reusable groups of mobs here, which can be referenced later using '@'.",
                " 在这里定义可复用的生物分组，方便在后续规则中通过'@'符号调用。",
                " ",
                " Format / 格式: 'set_id;member1,member2,!exclude_member1,@exclude_set1'",
                " ",
                " Parameters / 参数说明:",
                "   - set_id:           A unique ID for the set, e.g., 'undead'.",
                "                     自定义的唯一ID, 例如 'undead'。",
                "   - member:           A mob's ID (e.g., 'minecraft:zombie') or another set's ID (e.g., '@undead').",
                "                     生物的ID (例如 'minecraft:zombie') 或另一个集合的ID (例如 '@undead')。",
                "   - exclude_member:   Prefix with '!' to exclude a mob or a set from this set.",
                "                     在成员前加上'!'符号，意味着从这个集合中排除该生物或集合。",
                "   - wildcard:         Use 'modid:*' to include all mobs from a specific mod.",
                "                     使用 'modid:*' 可以包含一个mod的所有生物。",
                " ",
                " Example / 示例:",
                "   'undead;minecraft:zombie,minecraft:skeleton,minecraft:zombie_villager'",
                "   'all_monsters;minecraft:*,!minecraft:creeper,!@undead' -> Includes all vanilla mobs except creepers and members of the '@undead' set."
        ).defineList("sets", Collections.emptyList(), obj -> obj instanceof String);
        BUILDER.pop();


        BUILDER.push("rules");
        FACTIONS = BUILDER.comment(
                " ",
                "====================================================================================",
                " 2. Faction Rules / 阵营规则",
                "====================================================================================",
                " Define mob factions and their relationships.",
                " 定义生物阵营及其相互关系。",
                " 格式: 'faction_id;member_list;[relationship_setting1];[relationship_setting2]...'",
                " ",
                " Parameters / 参数说明:",
                "   - faction_id:       A unique ID for the faction, e.g., 'illagers'.",
                "                     自定义的唯一ID, 例如 'illagers'。",
                "   - member_list:      A list of mob IDs or set IDs that belong to this faction. Prefix set IDs with '@'. The player's ID is 'player'.",
                "                     属于该阵营的生物ID或集合ID列表。集合ID前需加'@'。玩家的ID是 'player'。",
                "   - relationship_setting:",
                "     - friendly=<faction_id>:  Friendly towards the target faction.",
                "                                与目标阵营友好。",
                "     - hostile=<faction_id>:   Hostile towards the target faction.",
                "                                与目标阵营敌对。",
                "     - neutral=<faction_id>:   Neutral towards the target faction.",
                "                                与目标阵营中立。",
                "     - friendlyfire=<true/false>: (Optional) Allow or disallow friendly fire. Overrides the global setting.",
                "                                (可选) 是否允许友军伤害。会覆盖全局设置。",
                " ",
                " Example / 示例:",
                "   'illagers;minecraft:pillager,minecraft:vindicator;hostile=players,villagers;friendlyfire=false'",
                "   'players;player;friendly=wolf_pack;hostile=illagers'"
        ).defineList("factions", Collections.emptyList(), obj -> obj instanceof String);

        MOB_RULES = BUILDER.comment(
                " ",
                "====================================================================================",
                " 3. Individual Mob Rules / 独立生物规则",
                "====================================================================================",
                " Set specific behavior rules for individual mob types, which override faction rules.",
                " 为单个生物类型设置特殊的行为规则，其优先级高于阵营规则。",
                " 格式: 'mob_id;[rule1];[rule2]...'",
                " ",
                " Parameters / 参数说明:",
                "   - mob_id:    The ID of the mob to apply rules to, e.g., 'minecraft:creeper'.",
                "                要应用规则的生物ID, 例如 'minecraft:creeper'。",
                "   - rule:",
                "     - ignore=<target>:     Ignores a specific mob or faction. e.g., 'ignore=player,@undead'.",
                "                            无视特定的生物或阵营。例如 'ignore=player,@undead'。",
                "     - proactive=<target>:  Proactively attacks a specific mob or faction. e.g., 'proactive=minecraft:sheep'.",
                "                            主动攻击特定的生物或阵营。例如 'proactive=minecraft:sheep'。",
                "     - friendlyfire=<true/false>: Individually set friendly fire allowance for this mob.",
                "                            单独为此生物设置是否允许友军伤害。",
                " ",
                " Example / 示例:",
                "   'minecraft:creeper;ignore=player' -> Creepers will no longer target players.",
                "   'minecraft:wolf;proactive=minecraft:skeleton' -> Wolves will actively hunt skeletons."
        ).defineList("mob_rules", Collections.emptyList(), obj -> obj instanceof String);

        AGGRESSION_RULES = BUILDER.comment(
                " ",
                "====================================================================================",
                " 4. Aggression Rules / 攻击性改造规则",
                "====================================================================================",
                " Grant passive mobs aggressive AI and custom attributes. This is a core feature of the mod.",
                " 让和平生物获得攻击性AI和自定义属性。这是本模组的核心功能。",
                " 格式: 'mob_id;damage=<value>;[speed=<multiplier>]'",
                " ",
                " Parameters / 参数说明:",
                "   - mob_id:           The ID of the passive mob to modify, e.g., 'minecraft:sheep'.",
                "                       要改造的和平生物ID, 例如 'minecraft:sheep'。",
                "   - damage=<value>:     Sets the final damage dealt by the mob (1.0 = half a heart).",
                "                       设置生物造成的最终伤害值 (1.0 = 半颗心)。",
                "   - speed=<multiplier>: (Optional) Sets the speed multiplier when chasing a target. e.g., 'speed=1.5' means a 50% speed increase.",
                "                       (可选) 设置生物在追击目标时的速度乘数。例如 'speed=1.5' 意味着速度提升50%。",
                " ",
                " Example / 示例:",
                "   'minecraft:sheep;damage=8.0;speed=1.8' -> Sheep will deal 4 hearts of damage and move at 1.8x speed when chasing.",
                "   'minecraft:villager;damage=10.0' -> Villagers will attack hostile targets for 5 hearts of damage with normal speed."
        ).defineList("aggression_rules", Collections.emptyList(), obj -> obj instanceof String);
        BUILDER.pop();


        BUILDER.push("global_settings");
        ENABLE_FRIENDLY_FIRE = BUILDER.comment(
                " ",
                "====================================================================================",
                " 5. Global Settings / 全局设置",
                "====================================================================================",
                " Global toggles for basic mechanics. Can be overridden by faction or individual mob rules.",
                " 全局开关，控制一些基础的游戏机制。可以被阵营或独立生物规则覆盖。",
                " ",
                " enable_friendly_fire: Globally allow (true) or disallow (false) friendly fire.",
                "                       全局允许(true)或禁止(false)友军伤害。"
        ).define("enable_friendly_fire", false);
        BUILDER.pop();


        BUILDER.push("alliance_mechanics");
        ALLIANCE_ENABLE = BUILDER.comment(
                " ",
                "====================================================================================",
                " 6. Alliance Mechanics / 同盟机制",
                "====================================================================================",
                " Controls how allies respond when a friend is attacked.",
                " 控制盟友在同伴受攻击时的支援行为。",
                " ",
                " enable: Enable or disable the alliance support system.",
                "         是否启用同盟支援系统。"
        ).define("enable", true);
        ALLIANCE_RESPONSE_RANGE = BUILDER.comment(
                " response_range: The maximum range (in blocks) from which an ally can respond to a call for help.",
                "                 盟友能响应支援请求的最大范围（方块）。"
        ).defineInRange("response_range", 40.0, 0.0, 256.0);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
