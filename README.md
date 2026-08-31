# UnseenLight

[![bStats Servers](https://img.shields.io/bstats/servers/33769)](https://bstats.org/plugin/bukkit/UnseenLight/33769)
[![Paper](https://img.shields.io/badge/Paper-26.2-blue)](https://papermc.io/)

A Paper plugin that makes the vanilla **invisible light block** (`minecraft:light`) a survival
feature: craft it on a workbench, place it in any dark spot, take it back down with a right
click — no commands, no creative mode.

## Crafting

```
G G G
G T G        G = Glass Pane, T = Torch
G G G        -> 1x Light (level 15)
```

The shape, ingredients, yield and light level are all configurable. The recipe shows up in the
recipe book on join (also configurable).

## Using it

| Action | How |
|---|---|
| **Place** | Hold the light item and right-click any dark spot — plain vanilla placement |
| **Remove** | Hold **any block** and right-click the spot the light occupies; the light pops back as an item instead of your block being placed |
| **Remove behind chests/doors** | Sneak + right-click (a plain click opens the container, like vanilla) |
| **See placed lights** | Hold a light item — vanilla renders them — or run `/ul show` for a particle highlight |

A light block is invisible and has no hitbox, so your crosshair passes through it; aim so that
the block you are "placing" would land in the light's spot.

## What the plugin guards against

`minecraft:light` is replaceable, so in vanilla almost anything overwrites it silently. UnseenLight
intercepts every such path — the light either pops properly (permission checked, land protection
consulted, item returned) or stays protected:

- block placement into the light's spot, from either hand, in any game mode;
- lava and powder snow buckets (water just waterlogs the light and is left alone);
- falling blocks landing in the spot (the item is compensated);
- the vanilla break while holding a light item (lights have no loot table — drop-mode is honoured);
- Crafter blocks crafting the recipe with nobody's permission to check (optional, `recipe.allow-crafter`);
- `/minecraft:reload` wiping plugin recipes (the recipe re-registers itself).

Land protection plugins (WorldGuard, GriefPrevention, …) are consulted through a `BlockBreakEvent`
before any light is removed, and CoreProtect-style loggers see the removal.

## Commands

Alias: `/ul`

| Command | Description | Permission (default) |
|---|---|---|
| `/unseenlight reload` | Reload `config.yml` | `unseenlight.command.reload` (op) |
| `/unseenlight give <players> [level] [amount]` | Give light items | `unseenlight.command.give` (op) |
| `/unseenlight show` | Highlight nearby lights with particles | `unseenlight.command.show` (everyone) |

## Permissions

| Node | Default | Grants |
|---|---|---|
| `unseenlight.craft` | true | Crafting the recipe (and seeing it in the recipe book) |
| `unseenlight.place` | true | Placing light blocks |
| `unseenlight.remove` | true | Taking placed lights down |

## Configuration

Everything lives in `config.yml`: the recipe (shape, ingredients, yield, light level 0–15),
sounds and particles for placing/removing, the highlighter (radius, duration), and every player
message in [MiniMessage](https://docs.papermc.io/adventure/minimessage/format/) format.

The most consequential switch is what a removed light turns into:

```yaml
removal:
  drop-mode: GROUND     # GROUND: drops as an item
                        # INVENTORY: goes straight to the inventory
                        # DESTROY: disappears without a trace
```

## Installation

1. Requires **Paper 26.2+** and **Java 25+**.
2. Drop `UnseenLight-x.y.jar` into `plugins/` and restart.

## Building

```
./gradlew build        # -> build/libs/UnseenLight-1.0-SNAPSHOT.jar (shaded, ready to deploy)
./gradlew runServer    # spins up a local Paper 26.2 test server with the plugin
```

## Metrics

Anonymous usage stats are collected via [bStats](https://bstats.org/plugin/bukkit/UnseenLight/33769).
Server owners can opt out globally in `plugins/bStats/config.yml`.
