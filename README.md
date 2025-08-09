[阅读中文版本 (Read in Chinese)](README_zh.md)

# Advanced Hostility

Tired of predictable mob behavior? Advanced Hostility is a powerful configuration tool that gives you total control over the world's ecosystem. Whether you are a server administrator creating a unique PvE experience, a modpack creator weaving intricate mob interactions, or a solo player looking for a fresh challenge, this mod provides the tools you need.

## Core Features

- **Custom Factions**: Group any mob from vanilla or other mods into factions like "The Undead Legion" or "Forest Guardians".

- **Complex Relationships**: Set factions to be Friendly, Hostile, or Neutral towards each other. Create global alliances or spark world-ending wars between creatures.

- **AI Overhaul**: Grant passive mobs like sheep and villagers aggressive AI with custom damage and speed. Or, make Creepers ignore players entirely!

- **High Compatibility**: Designed to intelligently supplement, not just replace, vanilla AI. This allows for dynamic and emergent interactions with mobs from other mods.

- **Server-Friendly Management**: All configurations can be edited in-game via commands and reloaded without a server restart, making it perfect for server administrators and modpack creators.

## Configuration Guide

Advanced Hostility is configured through the [advancedhostility-common.toml](file://E:\AI_KING\All_Target\run\config\advancedhostility-common.toml) file (for global settings) and the [advancedhostility-server.toml](file://E:\AI_KING\All_Target\run\saves\新的世界\serverconfig\advancedhostility-server.toml) file (for world-specific overrides).

The configuration is broken down into four main rule types, which are processed in the following order of priority: Mob Rules > Faction Rules > Vanilla Behavior.

### 1. Sets

Sets are reusable groups of entities. They are a convenient way to manage large numbers of mobs.

**Format**: `set_id;member1,member2,@another_set,!exclude_mob`

- `@` references another set.
- `!` excludes an entity or set.
- `modid:*` includes all mobs from a specific mod.

**Example**:

toml sets = [ "undead;minecraft:zombie,minecraft:skeleton,minecraft:zombie_villager", "monsters;minecraft:*,!minecraft:creeper,!@undead" ]

### 2. Factions

Factions are groups of entities that share common relationships.

**Format**: `faction_id;member_list;relationship1;relationship2`

`member_list` can contain entity IDs or set references (e.g., `@undead`).

**Example**:

toml factions = [ "illagers;minecraft:pillager,minecraft:vindicator;hostile=players,villagers", "players;player;friendly=wolf_pack" ]

### 3. Mob Rules

Mob Rules define specific behaviors for individual entity types, overriding any faction rules.

**Format**: `mob_id;rule1;rule2`

- `proactive=<target>`: Makes the mob actively hunt the target.
- `ignore=<target>`: Makes the mob ignore the target.

**Example**:

toml mob_rules = [ "minecraft:creeper;ignore=player", "minecraft:wolf;proactive=@undead" ]

### 4. Aggression Rules

This powerful feature gives non-hostile mobs a complete combat AI overhaul.

**Format**: `mob_id;damage=<value>;speed=<multiplier>`

- `damage`: The final damage dealt (1.0 = half a heart).
- [speed](file://net\minecraft\world\entity\LivingEntity.java#L146-L146): A multiplier for movement speed when chasing a target.

**Example**:

toml aggression_rules = [ "minecraft:sheep;damage=8.0;speed=1.8", "minecraft:villager;damage=10.0" ]

## In-Game Commands

Manage your server's configuration on the fly without ever needing to restart. The base command is `/advancedhostility`.

### Checking Configuration

- `/advancedhostility check sets`: Lists all defined sets.
- `/advancedhostility check set <set_name>`: Shows the members of a specific set.
- `/advancedhostility check factions`: Lists all defined factions.
- `/advancedhostility check faction <faction_name>`: Shows the details of a specific faction.
- `/advancedhostility check mobrules`: Lists all mobs with specific rules.
- `/advancedhostility check mobrule <mob_id>`: Shows the rules for a specific mob.
- `/advancedhostility check relation <mob1_id> <mob2_id>`: Checks the final relationship between two specific mobs.
- `/advancedhostility check relations <mob_id>`: Shows all defined relationships for a specific mob.

### Editing Server Configuration

These commands modify the world's `serverconfig/advancedhostility-server.toml` file.
A `/advancedhostility reload` is required for changes to take effect.

- `/advancedhostility mobrule set <mob_id> [proactive|ignore] <target>`: Sets a proactive or ignore rule for a mob.
- `/advancedhostility mobrule clear <mob_id>`: Clears all specific rules for a mob.
- `/advancedhostility faction create <faction_name>`: Creates a new, empty faction.
- `/advancedhostility faction addmember <faction_name> <member>`: Adds an entity or set to a faction.
- `/advancedhostility faction removemember <faction_name> <member>`: Removes an entity or set from a faction.
- `/advancedhostility faction setrelation <faction1> [friendly|hostile|neutral] <faction2>`: Sets the relationship between two factions.

## Installation

1. Ensure you have the correct version of Minecraft Forge installed.
2. Download the .jar file from the Releases page.
3. Place the .jar file into your mods folder.
4. Launch the game!

## License

This project is licensed under the MIT License. See the LICENSE file for details. You are free to use, modify, and distribute this mod and its source code in your own projects and modpacks.