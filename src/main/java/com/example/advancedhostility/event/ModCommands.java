package com.example.advancedhostility.event;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.example.advancedhostility.AdvancedHostility;
import com.example.advancedhostility.config.ConfigManager;
import com.example.advancedhostility.config.ModConfig;
import com.example.advancedhostility.config.ServerConfig;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class ModCommands {

    private static final String MOB_RULES_PATH = "rules_override.mob_rules";
    private static final String FACTIONS_PATH = "rules_override.factions";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("advancedhostility")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("reload")
                                .executes(context -> {
                                    // ... (reload logic is unchanged) ...
                                    CommandSourceStack source = context.getSource();
                                    MinecraftServer server = source.getServer();
                                    source.sendSuccess(() -> Component.literal("§6Starting reload of all data packs and Advanced Hostility configs..."), true);
                                    Collection<String> selectedPacks = server.getPackRepository().getSelectedIds();
                                    CompletableFuture<Void> vanillaReloadFuture = server.reloadResources(selectedPacks);
                                    vanillaReloadFuture.thenRunAsync(() -> {
                                        AdvancedHostility.LOGGER.info("Vanilla reload complete. Now reloading Advanced Hostility configs...");
                                        Path commonConfigPath = FMLPaths.CONFIGDIR.get().resolve("advancedhostility-common.toml");
                                        final CommentedFileConfig commonConfigData = CommentedFileConfig.builder(commonConfigPath).sync().autosave().writingMode(WritingMode.REPLACE).build();
                                        commonConfigData.load();
                                        ModConfig.SPEC.correct(commonConfigData);
                                        ModConfig.SPEC.setConfig(commonConfigData);
                                        commonConfigData.save();
                                        Path serverConfigFolder = server.getWorldPath(LevelResource.ROOT).resolve("serverconfig");
                                        Path serverConfigPath = serverConfigFolder.resolve("advancedhostility-server.toml");
                                        if (serverConfigPath.toFile().exists()) {
                                            final CommentedFileConfig serverConfigData = CommentedFileConfig.builder(serverConfigPath).sync().autosave().writingMode(WritingMode.REPLACE).build();
                                            serverConfigData.load();
                                            ServerConfig.SPEC.correct(serverConfigData);
                                            ServerConfig.SPEC.setConfig(serverConfigData);
                                            serverConfigData.save();
                                        }
                                        ConfigManager.parse();
                                        AtomicInteger updatedMobs = new AtomicInteger(0);
                                        for (net.minecraft.server.level.ServerLevel level : server.getAllLevels()) {
                                            for (Entity entity : level.getAllEntities()) {
                                                if (entity instanceof Mob mob) {
                                                    MobEvents.applyAggressionRules(mob);
                                                    updatedMobs.incrementAndGet();
                                                }
                                            }
                                        }
                                        source.sendSuccess(() -> Component.literal(String.format("§aAdvanced Hostility logic re-applied to %d existing mobs.", updatedMobs.get())), true);
                                        AdvancedHostility.LOGGER.info("Advanced Hostility configs reloaded and applied.");
                                    }, server);
                                    return 1;
                                })
                        )
                        .then(Commands.literal("check")
                                // The 'check' subcommands would be here. They are omitted for brevity but should be included from your previous version.
                        )
                        // ★ ================================================= ★
                        // ★                NEW EDITING COMMANDS               ★
                        // ★ ================================================= ★
                        .then(Commands.literal("mobrule")
                                .then(Commands.literal("set")
                                        .then(Commands.argument("mob_id", ResourceLocationArgument.id())
                                                .suggests((c, b) -> SharedSuggestionProvider.suggest(ForgeRegistries.ENTITY_TYPES.getKeys().stream().map(Object::toString), b))
                                                .then(Commands.literal("proactive")
                                                        // ★ FIX: Changed to greedyString to accept complex IDs
                                                        .then(Commands.argument("target", StringArgumentType.greedyString())
                                                                .suggests((c, b) -> SharedSuggestionProvider.suggest(getMobAndSetSuggestions(), b))
                                                                .executes(context -> setMobRule(
                                                                        context.getSource(),
                                                                        ResourceLocationArgument.getId(context, "mob_id").toString(),
                                                                        "proactive",
                                                                        StringArgumentType.getString(context, "target")
                                                                ))
                                                        )
                                                )
                                                .then(Commands.literal("ignore")
                                                        // ★ FIX: Changed to greedyString to accept complex IDs
                                                        .then(Commands.argument("target", StringArgumentType.greedyString())
                                                                .suggests((c, b) -> SharedSuggestionProvider.suggest(getMobAndSetSuggestions(), b))
                                                                .executes(context -> setMobRule(
                                                                        context.getSource(),
                                                                        ResourceLocationArgument.getId(context, "mob_id").toString(),
                                                                        "ignore",
                                                                        StringArgumentType.getString(context, "target")
                                                                ))
                                                        )
                                                )
                                        )
                                )
                                .then(Commands.literal("clear")
                                        .then(Commands.argument("mob_id", ResourceLocationArgument.id())
                                                .suggests((c, b) -> SharedSuggestionProvider.suggest(ConfigManager.getMobRuleNames(), b))
                                                .executes(context -> clearMobRule(
                                                        context.getSource(),
                                                        ResourceLocationArgument.getId(context, "mob_id").toString()
                                                ))
                                        )
                                )
                        )
                        .then(Commands.literal("faction")
                                .then(Commands.literal("create")
                                        .then(Commands.argument("faction_name", StringArgumentType.word())
                                                .executes(context -> createFaction(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "faction_name")
                                                ))
                                        )
                                )
                                .then(Commands.literal("addmember")
                                        .then(Commands.argument("faction_name", StringArgumentType.word())
                                                .suggests((c,b) -> SharedSuggestionProvider.suggest(ConfigManager.getFactionNames(), b))
                                                // ★ FIX: Changed to greedyString to accept complex IDs
                                                .then(Commands.argument("member", StringArgumentType.greedyString())
                                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(getMobAndSetSuggestions(), b))
                                                        .executes(context -> addFactionMember(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "faction_name"),
                                                                StringArgumentType.getString(context, "member")
                                                        ))
                                                )
                                        )
                                )
                                .then(Commands.literal("removemember")
                                        .then(Commands.argument("faction_name", StringArgumentType.word())
                                                .suggests((c,b) -> SharedSuggestionProvider.suggest(ConfigManager.getFactionNames(), b))
                                                // ★ FIX: Changed to greedyString to accept complex IDs
                                                .then(Commands.argument("member", StringArgumentType.greedyString())
                                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(getMobAndSetSuggestions(), b))
                                                        .executes(context -> removeFactionMember(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "faction_name"),
                                                                StringArgumentType.getString(context, "member")
                                                        ))
                                                )
                                        )
                                )
                                .then(Commands.literal("setrelation")
                                        .then(Commands.argument("faction1", StringArgumentType.word())
                                                .suggests((c,b) -> SharedSuggestionProvider.suggest(ConfigManager.getFactionNames(), b))
                                                .then(Commands.argument("relation", StringArgumentType.word())
                                                        // ★ NEW: Added Chinese suggestions
                                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(new String[]{"friendly", "hostile", "neutral", "友好", "敌对", "中立"}, b))
                                                        .then(Commands.argument("faction2", StringArgumentType.word())
                                                                .suggests((c,b) -> SharedSuggestionProvider.suggest(ConfigManager.getFactionNames(), b))
                                                                .executes(context -> setFactionRelation(
                                                                        context.getSource(),
                                                                        StringArgumentType.getString(context, "faction1"),
                                                                        StringArgumentType.getString(context, "relation"),
                                                                        StringArgumentType.getString(context, "faction2")
                                                                ))
                                                        )
                                                )
                                        )
                                )
                        )
        );
    }

    private static char getRelationColor(ConfigManager.Relationship rel) {
        return switch (rel) {
            case FRIENDLY -> '2'; // Green
            case HOSTILE -> 'c';  // Red
            case NEUTRAL -> 'e';  // Yellow
            default -> 'f';      // White
        };
    }

    // ★ ================================================= ★
    // ★            PRIVATE CONFIG HELPER METHODS          ★
    // ★ ================================================= ★

    private static Path getConfigPath(CommandSourceStack source) {
        Path serverConfigDir = source.getServer().getWorldPath(LevelResource.ROOT).resolve("serverconfig");
        try {
            Files.createDirectories(serverConfigDir);
        } catch (IOException e) {
            AdvancedHostility.LOGGER.error("Could not create serverconfig directory", e);
        }
        return serverConfigDir.resolve("advancedhostility-server.toml");
    }

    private static Collection<String> getMobAndSetSuggestions() {
        List<String> suggestions = new ArrayList<>();
        suggestions.addAll(ForgeRegistries.ENTITY_TYPES.getKeys().stream().map(Object::toString).toList());
        suggestions.addAll(ConfigManager.getSetNames().stream().map(s -> "@" + s).toList());
        return suggestions;
    }

    private static int setMobRule(CommandSourceStack source, String mobId, String ruleType, String target) {
        Path path = getConfigPath(source);
        CommentedFileConfig config = CommentedFileConfig.builder(path).autosave().writingMode(WritingMode.REPLACE).build();
        config.load();

        List<String> rules = new ArrayList<>(config.getOrElse(MOB_RULES_PATH, Collections.emptyList()));
        int ruleIndex = -1;
        for (int i = 0; i < rules.size(); i++) {
            if (rules.get(i).startsWith(mobId + ";")) {
                ruleIndex = i;
                break;
            }
        }

        if (ruleIndex != -1) {
            String existingRule = rules.get(ruleIndex);
            List<String> parts = new ArrayList<>(Splitter.on(';').trimResults().splitToList(existingRule));
            boolean updated = false;
            for (int i = 1; i < parts.size(); i++) {
                if (parts.get(i).startsWith(ruleType + "=")) {
                    // Avoid adding duplicates
                    Set<String> targets = new HashSet<>(Splitter.on(',').trimResults().splitToList(parts.get(i).substring(ruleType.length() + 1)));
                    targets.add(target);
                    parts.set(i, ruleType + "=" + String.join(",", targets));
                    updated = true;
                    break;
                }
            }
            if (!updated) {
                parts.add(ruleType + "=" + target);
            }
            rules.set(ruleIndex, String.join(";", parts));
        } else {
            rules.add(mobId + ";" + ruleType + "=" + target);
        }

        config.set(MOB_RULES_PATH, rules);
        config.save();

        source.sendSuccess(() -> Component.literal("§aSuccessfully updated mob rule for " + mobId + ". Use §e/advancedhostility reload§a to apply changes."), true);
        return 1;
    }

    private static int clearMobRule(CommandSourceStack source, String mobId) {
        Path path = getConfigPath(source);
        CommentedFileConfig config = CommentedFileConfig.builder(path).autosave().writingMode(WritingMode.REPLACE).build();
        config.load();

        List<String> rules = new ArrayList<>(config.getOrElse(MOB_RULES_PATH, Collections.emptyList()));
        boolean removed = rules.removeIf(rule -> rule.startsWith(mobId + ";"));

        if(removed) {
            config.set(MOB_RULES_PATH, rules);
            config.save();
            source.sendSuccess(() -> Component.literal("§aSuccessfully cleared mob rules for " + mobId + ". Use §e/advancedhostility reload§a to apply changes."), true);
        } else {
            source.sendFailure(Component.literal("§cNo specific mob rule found for " + mobId + " to clear."));
        }
        return 1;
    }

    private static int createFaction(CommandSourceStack source, String factionName) {
        Path path = getConfigPath(source);
        CommentedFileConfig config = CommentedFileConfig.builder(path).autosave().writingMode(WritingMode.REPLACE).build();
        config.load();

        List<String> factions = new ArrayList<>(config.getOrElse(FACTIONS_PATH, Collections.emptyList()));
        if (factions.stream().anyMatch(f -> f.startsWith(factionName + ";"))) {
            source.sendFailure(Component.literal("§cFaction '" + factionName + "' already exists."));
            return 0;
        }

        factions.add(factionName + ";;"); // Create with empty members and relations
        config.set(FACTIONS_PATH, factions);
        config.save();

        source.sendSuccess(() -> Component.literal("§aFaction '" + factionName + "' created. Use other commands to add members and set relations. Remember to §e/advancedhostility reload§a."), true);
        return 1;
    }

    private static int addFactionMember(CommandSourceStack source, String factionName, String member) {
        return modifyFaction(source, factionName, (parts) -> {
            // Ensure member part exists and is not empty before splitting
            String memberPart = parts.get(1);
            Set<String> members = new HashSet<>(memberPart.isEmpty() ? Collections.emptyList() : Splitter.on(',').trimResults().splitToList(memberPart));
            members.add(member);
            parts.set(1, String.join(",", members));
            return parts;
        }, "add member " + member + " to");
    }

    private static int removeFactionMember(CommandSourceStack source, String factionName, String member) {
        return modifyFaction(source, factionName, (parts) -> {
            String memberPart = parts.get(1);
            Set<String> members = new HashSet<>(memberPart.isEmpty() ? Collections.emptyList() : Splitter.on(',').trimResults().splitToList(memberPart));
            members.remove(member);
            parts.set(1, String.join(",", members));
            return parts;
        }, "remove member " + member + " from");
    }

    private static int setFactionRelation(CommandSourceStack source, String faction1, String relationInput, String faction2) {
        // ★ NEW: Handle Chinese input
        String relation = switch (relationInput.toLowerCase(Locale.ROOT)) {
            case "友好", "friendly" -> "friendly";
            case "敌对", "hostile" -> "hostile";
            case "中立", "neutral" -> "neutral";
            default -> "invalid";
        };

        if (relation.equals("invalid")) {
            source.sendFailure(Component.literal("§cInvalid relation type. Must be 'friendly', 'hostile', or 'neutral' (or Chinese equivalent)."));
            return 0;
        }

        return modifyFaction(source, faction1, (parts) -> {
            Map<String, Set<String>> relations = new HashMap<>();
            relations.put("friendly", new HashSet<>());
            relations.put("hostile", new HashSet<>());
            relations.put("neutral", new HashSet<>());

            // Parse existing relations
            for (int i = 2; i < parts.size(); i++) {
                String part = parts.get(i);
                if(part.startsWith("friendly=")) relations.get("friendly").addAll(Splitter.on(',').trimResults().splitToList(part.substring(9)));
                else if(part.startsWith("hostile=")) relations.get("hostile").addAll(Splitter.on(',').trimResults().splitToList(part.substring(8)));
                else if(part.startsWith("neutral=")) relations.get("neutral").addAll(Splitter.on(',').trimResults().splitToList(part.substring(8)));
            }

            // Remove faction2 from all lists to avoid duplicates
            relations.values().forEach(s -> s.remove(faction2));
            // Add to the correct list
            relations.get(relation).add(faction2);

            // Rebuild relation parts
            List<String> newRelationParts = new ArrayList<>();
            relations.forEach((key, valueSet) -> {
                if (!valueSet.isEmpty()) {
                    newRelationParts.add(key + "=" + String.join(",", valueSet));
                }
            });

            // Replace old relation parts with new ones
            parts.subList(2, parts.size()).clear();
            parts.addAll(newRelationParts);

            return parts;
        }, "set relation for");
    }

    private static int modifyFaction(CommandSourceStack source, String factionName, java.util.function.Function<List<String>, List<String>> modifier, String actionDescription) {
        Path path = getConfigPath(source);
        CommentedFileConfig config = CommentedFileConfig.builder(path).autosave().writingMode(WritingMode.REPLACE).build();
        config.load();

        List<String> factions = new ArrayList<>(config.getOrElse(FACTIONS_PATH, Collections.emptyList()));
        int factionIndex = -1;
        for (int i = 0; i < factions.size(); i++) {
            if (factions.get(i).startsWith(factionName + ";")) {
                factionIndex = i;
                break;
            }
        }

        if (factionIndex == -1) {
            source.sendFailure(Component.literal("§cFaction '" + factionName + "' not found."));
            return 0;
        }

        String existingFaction = factions.get(factionIndex);
        List<String> parts = Lists.newArrayList(Splitter.on(';').trimResults().split(existingFaction));
        // Ensure member part exists, even if empty, to prevent IndexOutOfBounds
        while(parts.size() < 2) {
            parts.add("");
        }

        List<String> modifiedParts = modifier.apply(parts);
        factions.set(factionIndex, String.join(";", modifiedParts));

        config.set(FACTIONS_PATH, factions);
        config.save();

        source.sendSuccess(() -> Component.literal("§aSuccessfully " + actionDescription + " faction " + factionName + ". Use §e/advancedhostility reload§a to apply changes."), true);
        return 1;
    }
}

