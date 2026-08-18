# neoforge-26.1.2

Mirrors [`GlodBlock/ExtendedAE`](../../../YUKI.N/Research%20Targets/ExtendedAE)
(branch `26.1.2-neoforge`) for Minecraft/NeoForge version alignment. This is
the primary target for the AE2 `SortOrder.LINES` Mixin (Q2), since it runs
against the current AE2 release line.

## Status

- [x] Gradle module skeleton
- [ ] Confirm a Mekanism build compatible with Minecraft 26.1.2 (Mekanism's
      `main` branch source is currently pinned to `minecraft_version=1.21.1`;
      check for a newer release/branch before wiring the dependency)
- [ ] Add `META-INF/neoforge.mods.toml`
- [ ] Add Mixin config (`mek_factory_info.mixins.json`) + `SortOrderMixin`
