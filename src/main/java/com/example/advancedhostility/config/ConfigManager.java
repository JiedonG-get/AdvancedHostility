package com.example.advancedhostility.config;

import com.example.advancedhostility.AdvancedHostility;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.stream.Collectors;

public class ConfigManager {
    private static final Map<String, FactionData> FACTIONS = new HashMap<>();
    private static final Map<String, MobRuleData> MOB_RULES = new HashMap<>();
    private static final Map<String, Set<String>> RAW_SETS = new HashMap<>();
    private static final Map<String, Set<String>> RESOLVED_SETS = new HashMap<>();
    private static final Map<String, Double> AGGRESSION_RULES = new HashMap<>();
    private static final Map<String, Double> AGGRESSION_SPEED_RULES = new HashMap<>();

    public static void parse() {
        AdvancedHostility.LOGGER.info("Starting Advanced Hostility configuration parsing with merge logic...");
        // 在解析前清空所有缓存
        RAW_SETS.clear();
        RESOLVED_SETS.clear();
        FACTIONS.clear();
        MOB_RULES.clear();
        AGGRESSION_RULES.clear();
        AGGRESSION_SPEED_RULES.clear();

        // --- ★ 全新的合并逻辑 ★ ---

        // 第1步: 加载通用配置 (common.toml) 作为基础规则
        parseSets(ModConfig.SETS.get());
        resolveAllSets(); // 加载完所有基础集合后，先解析一次
        parseFactions(ModConfig.FACTIONS.get());
        parseMobRules(ModConfig.MOB_RULES.get());
        parseAggressionRules(ModConfig.AGGRESSION_RULES.get());

        // 第2步: 加载世界专属配置 (server.toml) 作为“补丁”进行覆盖
        // 解析方法会自动用新的规则覆盖掉ID相同的旧规则
        parseSets(ServerConfig.SETS.get());
        resolveAllSets(); // 如果server.toml覆盖或新增了集合，需要重新解析
        parseFactions(ServerConfig.FACTIONS.get());
        parseMobRules(ServerConfig.MOB_RULES.get());
        parseAggressionRules(ServerConfig.AGGRESSION_RULES.get());

        AdvancedHostility.LOGGER.info("Configuration loaded. {} factions, {} mob rules, {} aggression rules, {} speed rules.", FACTIONS.size(), MOB_RULES.size(), AGGRESSION_RULES.size(), AGGRESSION_SPEED_RULES.size());
    }

    // 用于解析集合规则的辅助方法
    private static void parseSets(List<? extends String> rules) {
        for (String rule : rules) {
            try {
                String[] parts = rule.split(";", 2);
                if (parts.length < 2) continue;
                RAW_SETS.put(parts[0].trim(), Arrays.stream(parts[1].split(",")).map(String::trim).collect(Collectors.toSet()));
            } catch (Exception e) {
                AdvancedHostility.LOGGER.error("Failed to parse set definition: {}", rule, e);
            }
        }
    }

    // 用于解析所有当前已加载的原始集合的辅助方法
    private static void resolveAllSets() {
        RESOLVED_SETS.clear();
        for (String setName : RAW_SETS.keySet()) {
            getResolvedSetContents(setName, new HashSet<>());
        }
    }

    // 用于解析阵营规则的辅助方法
    private static void parseFactions(List<? extends String> rules) {
        for (String rule : rules) {
            try {
                String[] parts = Arrays.stream(rule.split(";")).map(String::trim).toArray(String[]::new);
                if (parts.length < 2 || parts[0].isEmpty() || parts[1].isEmpty()) continue;
                String factionId = parts[0];
                Set<String> members = getResolvedSetContents(parts[1], new HashSet<>());
                Set<String> friendly = new HashSet<>(), hostile = new HashSet<>(), neutral = new HashSet<>();
                Boolean factionFriendlyFire = null;
                for (int i = 2; i < parts.length; i++) {
                    if (parts[i].startsWith("friendly=")) friendly.addAll(getResolvedSetContents(parts[i].substring(9), new HashSet<>()));
                    else if (parts[i].startsWith("hostile=")) hostile.addAll(getResolvedSetContents(parts[i].substring(8), new HashSet<>()));
                    else if (parts[i].startsWith("neutral=")) neutral.addAll(getResolvedSetContents(parts[i].substring(8), new HashSet<>()));
                    else if (parts[i].startsWith("friendlyfire=")) factionFriendlyFire = Boolean.parseBoolean(parts[i].substring(13));
                }
                FACTIONS.put(factionId, new FactionData(members, friendly, hostile, neutral, factionFriendlyFire));
            } catch (Exception e) {
                AdvancedHostility.LOGGER.error("Failed to parse faction rule: {}", rule, e);
            }
        }
    }

    // 用于解析独立生物规则的辅助方法
    private static void parseMobRules(List<? extends String> rules) {
        for (String rule : rules) {
            try {
                String[] parts = Arrays.stream(rule.split(";")).map(String::trim).toArray(String[]::new);
                if (parts.length < 2 || parts[0].isEmpty()) continue;
                String mobId = parts[0];

                // ★ 1. 先解析出当前规则行的数据
                Set<String> newIgnore = new HashSet<>();
                Set<String> newProactive = new HashSet<>();
                Boolean newFriendlyFire = null;
                for (int i = 1; i < parts.length; i++) {
                    if (!parts[i].contains("=")) continue;
                    String[] rulePart = parts[i].split("=", 2);
                    if (rulePart.length < 2) continue;
                    String key = rulePart[0].trim();
                    String value = rulePart[1].trim();
                    if ("ignore".equals(key)) newIgnore.addAll(getResolvedSetContents(value, new HashSet<>()));
                    else if ("proactive".equals(key)) newProactive.addAll(getResolvedSetContents(value, new HashSet<>()));
                    else if ("friendlyfire".equals(key)) newFriendlyFire = Boolean.parseBoolean(value);
                }

                // ★ 2. 检查是否已存在该生物的规则
                MobRuleData existingRule = MOB_RULES.get(mobId);
                if (existingRule == null) {
                    // 如果不存在，直接添加新规则
                    MOB_RULES.put(mobId, new MobRuleData(newIgnore, newProactive, newFriendlyFire));
                } else {
                    // ★ 3. 如果已存在，则进行合并
                    // 合并 ignore 和 proactive 集合
                    existingRule.ignore().addAll(newIgnore);
                    existingRule.proactive().addAll(newProactive);

                    // friendlyFire 设置以后面的为准（即 server.toml 覆盖 common.toml）
                    Boolean finalFriendlyFire = (newFriendlyFire != null) ? newFriendlyFire : existingRule.friendlyFire();

                    // 用合并后的数据更新规则
                    MOB_RULES.put(mobId, new MobRuleData(existingRule.ignore(), existingRule.proactive(), finalFriendlyFire));
                }
            } catch (Exception e) {
                AdvancedHostility.LOGGER.error("Failed to parse mob rule: {}", rule, e);
            }
        }
    }

    // 用于解析攻击性改造规则的辅助方法
    private static void parseAggressionRules(List<? extends String> rules) {
        for (String rule : rules) {
            try {
                String[] parts = Arrays.stream(rule.split(";")).map(String::trim).toArray(String[]::new);
                if (parts.length < 2 || parts[0].isEmpty()) continue;
                String mobId = parts[0];
                double damage = 0.0;
                double speed = -1.0;
                for (int i = 1; i < parts.length; i++) {
                    if (parts[i].startsWith("damage=")) damage = Double.parseDouble(parts[i].substring(7));
                    else if (parts[i].startsWith("speed=")) speed = Double.parseDouble(parts[i].substring(6));
                }
                if (damage > 0) AGGRESSION_RULES.put(mobId, damage);
                if (speed > 0) AGGRESSION_SPEED_RULES.put(mobId, speed);
            } catch (Exception e) {
                AdvancedHostility.LOGGER.error("Failed to parse aggression rule: {}", rule, e);
            }
        }
    }

    // --- 以下是原有的方法，保持不变 ---
    private static Set<String> getResolvedSetContents(String identifierString, Set<String> recursionGuard) {
        if (RESOLVED_SETS.containsKey(identifierString)) return RESOLVED_SETS.get(identifierString);
        if (recursionGuard.contains(identifierString)) {
            AdvancedHostility.LOGGER.error("Circular reference detected in set definitions involving '{}'! Aborting resolution.", identifierString);
            return Collections.emptySet();
        }
        recursionGuard.add(identifierString);
        Set<String> rawMembers = RAW_SETS.getOrDefault(identifierString, Arrays.stream(identifierString.split(",")).map(String::trim).collect(Collectors.toSet()));
        Set<String> finalMembers = new HashSet<>();
        Set<String> exclusions = new HashSet<>();
        for (String member : rawMembers) {
            if (member.startsWith("!")) {
                exclusions.addAll(resolveSingleIdentifier(member.substring(1), recursionGuard));
            } else {
                finalMembers.addAll(resolveSingleIdentifier(member, recursionGuard));
            }
        }
        finalMembers.removeAll(exclusions);
        RESOLVED_SETS.put(identifierString, finalMembers);
        recursionGuard.remove(identifierString);
        return finalMembers;
    }
    private static Set<String> resolveSingleIdentifier(String id, Set<String> recursionGuard) {
        if (id.startsWith("@")) {
            return getResolvedSetContents(id.substring(1), recursionGuard);
        }
        if (id.endsWith(":*")) {
            String modId = id.substring(0, id.length() - 2);
            return ForgeRegistries.ENTITY_TYPES.getKeys().stream().filter(key -> key.getNamespace().equals(modId)).map(Object::toString).collect(Collectors.toSet());
        }
        return Collections.singleton(id);
    }

    // --- ★ 新增的公共Getters，供指令系统调用 ★ ---
    public static Set<String> getSetNames() { return RAW_SETS.keySet(); }
    public static Set<String> getFactionNames() { return FACTIONS.keySet(); }
    public static Set<String> getMobRuleNames() { return MOB_RULES.keySet(); }

    public static Set<String> getResolvedSetContents(String identifierString) {
        return getResolvedSetContents(identifierString, new HashSet<>());
    }
    public static String getFaction(String entityId) {
        for (Map.Entry<String, FactionData> entry : FACTIONS.entrySet()) {
            if (entry.getValue().members().contains(entityId)) return entry.getKey();
        }
        return null;
    }
    public static Relationship getRelationship(String faction1, String faction2) {
        if (faction1 == null || faction2 == null) return Relationship.VANILLA;
        if (faction1.equals(faction2)) return Relationship.FRIENDLY;
        FactionData data1 = FACTIONS.get(faction1);
        FactionData data2 = FACTIONS.get(faction2);
        if (data1 == null || data2 == null) return Relationship.VANILLA;
        if (data1.friendly().contains(faction2) || data2.friendly().contains(faction1)) return Relationship.FRIENDLY;
        if (data1.hostile().contains(faction2) || data2.hostile().contains(faction1)) return Relationship.HOSTILE;
        if (data1.neutral().contains(faction2) || data2.neutral().contains(faction1)) return Relationship.NEUTRAL;
        return Relationship.VANILLA;
    }
    public static MobRuleData getRuleForMob(String mobId) {
        return MOB_RULES.get(mobId);
    }
    public static FactionData getFactionData(String factionId) {
        return FACTIONS.get(factionId);
    }
    public static boolean isFriendlyFireAllowed(LivingEntity attacker) {
        String attackerId = getEntityIdString(attacker);
        MobRuleData attackerRule = MOB_RULES.get(attackerId);
        if (attackerRule != null && attackerRule.friendlyFire() != null) {
            return attackerRule.friendlyFire();
        }
        String attackerFactionId = getFaction(attackerId);
        if (attackerFactionId != null) {
            FactionData factionData = FACTIONS.get(attackerFactionId);
            if (factionData != null && factionData.friendlyFire() != null) {
                return factionData.friendlyFire();
            }
        }
        return ServerConfig.ENABLE_FRIENDLY_FIRE.get();
    }
    public static Double getAggressionDamage(String mobId) {
        return AGGRESSION_RULES.get(mobId);
    }
    public static Double getAggressionSpeed(String mobId) {
        return AGGRESSION_SPEED_RULES.get(mobId);
    }
    public static boolean isAllianceEnabled() {
        return ServerConfig.ALLIANCE_ENABLE.get();
    }
    public static double getAllianceResponseRange() {
        return ServerConfig.ALLIANCE_RESPONSE_RANGE.get();
    }
    public static String getEntityIdString(Entity entity) {
        if (entity instanceof Player) return "player";
        ResourceLocation loc = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return loc != null ? loc.toString() : "unknown";
    }
    /**
     * 计算两个实体ID之间的最终关系，会综合考虑 mob_rules 和 faction 规则。
     * 优先级: mob_rules > faction_rules > 默认
     * @param attackerId 攻击方实体ID
     * @param targetId 目标方实体ID
     * @return 最终的关系
     */
    public static Relationship getFinalRelationship(String attackerId, String targetId) {
        // 自身总是友好的
        if (attackerId.equals(targetId)) {
            return Relationship.FRIENDLY;
        }

        // --- 优先级 1: 攻击方的独立生物规则 (mob_rules) ---
        MobRuleData attackerRule = getRuleForMob(attackerId);
        if (attackerRule != null) {
            // 如果目标在攻击方的“忽略”列表里，关系为中立
            if (attackerRule.ignore().contains(targetId)) {
                return Relationship.NEUTRAL;
            }
            // 如果目标在攻击方的“主动攻击”列表里，关系为敌对
            if (attackerRule.proactive().contains(targetId)) {
                return Relationship.HOSTILE;
            }
        }

        // --- 优先级 2: 阵营关系 (faction_rules) ---
        // 如果没有 mob_rule 覆盖，则回退到检查阵营关系
        String attackerFaction = getFaction(attackerId);
        String targetFaction = getFaction(targetId);
        Relationship factionRelationship = getRelationship(attackerFaction, targetFaction);

        return factionRelationship; // getRelationship 已经包含了默认返回 VANILLA 的逻辑
    }

    public enum Relationship { FRIENDLY, NEUTRAL, HOSTILE, VANILLA }
    public record FactionData(Set<String> members, Set<String> friendly, Set<String> hostile, Set<String> neutral, Boolean friendlyFire) {}
    public record MobRuleData(Set<String> ignore, Set<String> proactive, Boolean friendlyFire) {}
}
