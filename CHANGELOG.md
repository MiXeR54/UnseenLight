# Changelog

All notable changes to UnseenLight are documented in this file. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0] - 2026-10-18

Support for Paper 26.3.

- Compiled against and published for Paper 26.3. The jar keeps loading on 26.2 as well.

## [1.0.0] - 2026-08-31

First public release for Paper 26.2.

- Craftable invisible light: 8 glass panes + 1 torch make one `minecraft:light` block, shown in the recipe book on join.
- Place lights with a plain right-click; take them down by right-clicking their spot with any block,
  sneak + right-click behind containers.
- Placed lights are protected from silent overwrites: block placement, lava and powder snow buckets,
  falling blocks, the vanilla break, Crafter blocks and `/minecraft:reload`.
- Land protection plugins are consulted before a light is removed, block loggers see the removal.
- Commands `/ul reload`, `/ul give` and `/ul show` (particle highlight of nearby lights).
- Recipe, light level, removal drop mode, sounds, particles and all MiniMessage texts configurable in `config.yml`.
- Anonymous usage statistics via bStats.
