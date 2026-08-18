# Mekanism Factory Information

An add-on mod that exposes Mekanism (and Mekanism-family mod) Factory tier
information — specifically the resulting number of processing **Lines** —
to Applied Energistics 2 terminals, and adds a Lines-based sort option to
the AE2 terminal UI.

## Background

- Mekanism `Factory` blocks (Basic/Advanced/Elite/Ultimate, and any tiers
  added by add-ons such as MekanismExtras or EvolvedMekanism) each expose a
  fixed number of processing Lines via `FactoryTier#processes`.
- Tier Installer items report the resulting tier
  (`ItemTierInstaller#getToTier()`), from which the Lines count can be
  derived.
- AE2's terminal sort order (`appeng.api.config.SortOrder`) has no public
  extension point, but Mekanism add-ons (e.g. EvolvedMekanism) demonstrate
  that new enum constants can be injected into existing enums at runtime via
  Mixin. This project applies the same technique to add a `LINES` sort
  order to AE2.

## Supported targets

This is a multi-module Gradle project. Each module targets one combination
of Mod Loader / Minecraft version, mirroring the versions used by the
upstream mods this project integrates with:

| Module                | Mod Loader | Minecraft |
| --------------------- | ---------- | --------- |
| `neoforge-26.1.2`     | NeoForge   | 1.21.x    |
| `neoforge-1.21.1`     | NeoForge   | 1.21.1    |
| `forge-1.20.1`        | Forge      | 1.20.1    |

## License

Licensed under the GNU Lesser General Public License v3.0 (LGPL-3.0), the
same license used by Mekanism and Applied Energistics 2. See [LICENSE](LICENSE).
