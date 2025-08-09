[Read in English (阅读英文版本)](README.md)

# Advanced Hostility 进阶仇恨

厌倦了可预测的怪物行为？进阶仇恨 是一个强大的配置工具，让您完全掌控世界生态。无论您是想创造独特PvE体验的服务器服主，还是想编织复杂生物交互的整合包作者，亦或是寻求新挑战的单人玩家，这个模组都能为您提供所需的工具。

## 核心功能

- **自定义派系**：将原版或其他模组中的任何怪物分组成派系，如"亡灵军团"或"森林守护者"。

- **复杂关系**：设置派系之间友好、敌对或中立的关系。在生物之间创建全球联盟或引发世界末日战争。

- **AI改造**：赋予绵羊和村民等被动生物攻击性AI，自定义伤害和速度。或者让爬行者完全忽略玩家！

- **高度兼容性**：设计为智能补充而非替代原版AI。这允许与来自其他模组的生物进行动态和突发的互动。

- **服务器友好管理**：所有配置都可以通过游戏内命令进行编辑，并且无需重启服务器即可重新加载，非常适合服务器管理员和模组包制作者。

## 配置指南

Advanced Hostility 通过 advancedhostility-common.toml 文件（用于全局设置）和 advancedhostility-server.toml 文件（用于世界特定覆盖）进行配置。

配置分为四种主要规则类型，按以下优先级顺序处理：生物规则 > 派系规则 > 原版行为。

### 1. 集合

集合是实体的可重用组。这是管理大量生物的便捷方式。

**格式**：`set_id;member1,member2,@another_set,!exclude_mob`

- `@` 引用另一个集合。
- `!` 排除一个实体或集合。
- `modid:*` 包含特定模组的所有生物。

**示例**：

toml sets = [ "undead;minecraft:zombie,minecraft:skeleton,minecraft:zombie_villager", "monsters;minecraft:*,!minecraft:creeper,!@undead" ]

### 2. 派系

派系是共享共同关系的实体组。

**格式**：`faction_id;member_list;relationship1;relationship2`

`member_list` 可以包含实体ID或集合引用（例如 `@undead`）。

**示例**：

toml factions = [ "illagers;minecraft:pillager,minecraft:vindicator;hostile=players,villagers", "players;player;friendly=wolf_pack" ]

### 3. 生物规则

生物规则定义单个实体类型的特定行为，覆盖任何派系规则。

**格式**：`mob_id;rule1;rule2`

- `proactive=<target>`：使生物主动追击目标。
- `ignore=<target>`：使生物忽略目标。

**示例**：

toml mob_rules = [ "minecraft:creeper;ignore=player", "minecraft:wolf;proactive=@undead" ]

### 4. 攻击规则

这个强大的功能为非敌对生物提供了完整的战斗AI改造。

**格式**：`mob_id;damage=<value>;speed=<multiplier>`

- `damage`：造成的最终伤害（1.0 = 半颗心）。
- [speed](file://net\minecraft\world\entity\LivingEntity.java#L146-L146)：追逐目标时的移动速度倍数。

**示例**：

toml aggression_rules = [ "minecraft:sheep;damage=8.0;speed=1.8", "minecraft:villager;damage=10.0" ]

## 游戏内命令

无需重启即可动态管理服务器配置。基础命令是 `/advancedhostility`。

### 检查配置

- `/advancedhostility check sets`：列出所有定义的集合。
- `/advancedhostility check set <set_name>`：显示特定集合的成员。
- `/advancedhostility check factions`：列出所有定义的派系。
- `/advancedhostility check faction <faction_name>`：显示特定派系的详细信息。
- `/advancedhostility check mobrules`：列出所有具有特定规则的生物。
- `/advancedhostility check mobrule <mob_id>`：显示特定生物的规则。
- `/advancedhostility check relation <mob1_id> <mob2_id>`：检查两个特定生物之间的最终关系。
- `/advancedhostility check relations <mob_id>`：显示特定生物的所有定义关系。

### 编辑服务器配置

这些命令修改世界的 `serverconfig/advancedhostility-server.toml` 文件。
需要执行 `/advancedhostility reload` 才能使更改生效。

- `/advancedhostility mobrule set <mob_id> [proactive|ignore] <target>`：为生物设置主动或忽略规则。
- `/advancedhostility mobrule clear <mob_id>`：清除生物的所有特定规则。
- `/advancedhostility faction create <faction_name>`：创建一个新的空派系。
- `/advancedhostility faction addmember <faction_name> <member>`：向派系添加实体或集合。
- `/advancedhostility faction removemember <faction_name> <member>`：从派系中移除实体或集合。
- `/advancedhostility faction setrelation <faction1> [friendly|hostile|neutral] <faction2>`：设置两个派系之间的关系。

## 安装

1. 确保已安装正确版本的 Minecraft Forge。
2. 从发布页面下载 .jar 文件。
3. 将 .jar 文件放入您的 mods 文件夹。
4. 启动游戏！

## 许可证

该项目采用 MIT 许可证。有关详细信息，请参阅 LICENSE 文件。您可以自由地在自己的项目和模组包中使用、修改和分发此模组及其源代码。