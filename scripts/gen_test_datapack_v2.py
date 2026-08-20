#!/usr/bin/env python3

from __future__ import annotations

import json
import sys
from dataclasses import dataclass, field
from pathlib import Path
from typing import Literal

import registry_loader

# ─────────────────────────────────────────────────────────────────────────────
#  BLOCK REGISTRY
# ─────────────────────────────────────────────────────────────────────────────

# Registry of actual block/item IDs loaded from mod jars
REGISTRY: dict[str, set[str]] = {}

def is_block_registered(block_id: str) -> bool:
  """Check if a block ID exists in the loaded mod registry."""
  return registry_loader.is_registered(REGISTRY, block_id)

# ─────────────────────────────────────────────────────────────────────────────
#  DATA MODEL
# ─────────────────────────────────────────────────────────────────────────────

MachineType = Literal["factory", "energy_cube", "fluid_tank", "chemical_tank", "thermodynamic_conductor"]

@dataclass
class MachineLine:
  mod_id: str
  factory_block_ids: list[tuple[str, str]]  # [(tier_label, block_id), ...]
  non_factory_block_id: str | None = None

@dataclass
class MachineFamily:
  """Set of blocks across all tiers for one machine type (e.g. smelter factories)."""
  family_id: str        # "smelting", "enriching", etc.
  machine_type: MachineType
  label: str            # display name
  machine_lines: list[MachineLine]
  connectable_cable: str | None = None  # e.g. "logistical_transporter"

@dataclass
class ModMachines:
  """All machine families for one mod."""
  mod_id: str
  label: str
  machine_families: list[MachineFamily]


# ─────────────────────────────────────────────────────────────────────────────
#  MOD DEFINITIONS
# ─────────────────────────────────────────────────────────────────────────────

MEKANISM_TIERS = ["basic", "advanced", "elite", "ultimate"]
MEKANISM_EXTRAS_TIERS = ["absolute", "supreme", "cosmic", "infinite"]
EVOLVED_MEKANISM_TIERS = ["overclocked", "quantum", "dense", "multiversal"]
EVOLVED_MEKANISM_EXTRAS_TIERS = ["absolute_overclocked", "supreme_quantum", "cosmic_dense", "infinite_multiversal"]
ASTRAL_NONASTRAL_MEKANISM_TIERS = ["essential_energized", "basic_standard_energized", "advanced_energized", "elite_energized", "enchanted_ultimate_energized", "absolute_overclocked_energized", "supreme_quantum_energized", "cosmic_dense_energized", "infinite_multiversal_energized"]
ASTRAL_ASTRAL_MEKANISM_TIERS = [x.replace("_energized", "_astral_energized") for x in ASTRAL_NONASTRAL_MEKANISM_TIERS]

# Factory types and their Mekanism block IDs
def _make_smelter_family() -> MachineFamily:
  return MachineFamily(
    family_id="smelting_factory",
    machine_type="factory",
    label="Smelter",
    connectable_cable="logistical_transporter",
    machine_lines=[
      MachineLine(
        mod_id="mekanism",
        factory_block_ids=[
          (tier.title(), f"{tier}_smelting_factory") for tier in MEKANISM_TIERS
        ],
        non_factory_block_id="energized_smelter",
      ),
      MachineLine(
        mod_id="mekanism_extras",
        factory_block_ids=[
          (tier.title(), f"{tier}_smelting_factory") for tier in MEKANISM_EXTRAS_TIERS
        ],
      ),
      MachineLine(
        mod_id="evolvedmekanism",
        factory_block_ids=[
          (tier.title(), f"{tier}_smelting_factory") for tier in EVOLVED_MEKANISM_TIERS
        ],
      ),
      MachineLine(
        mod_id="emextras",
        factory_block_ids=[
          (tier.title(), f"{tier}_smelting_factory") for tier in EVOLVED_MEKANISM_EXTRAS_TIERS
        ],
      ),
      MachineLine(
        mod_id="astral_mekanism",
        factory_block_ids=[
          (tier.title(), f"{tier}_smelting_factory") for tier in ASTRAL_NONASTRAL_MEKANISM_TIERS
        ],
        non_factory_block_id="essential_energized_smelter",
      ),
      MachineLine(
        mod_id="astral_mekanism",
        factory_block_ids=[
          (tier.title(), f"{tier}_smelting_factory") for tier in ASTRAL_ASTRAL_MEKANISM_TIERS
        ],
        non_factory_block_id="astral_energized_smelter",
      ),
    ],
  )

def _make_energy_cube_family() -> MachineFamily:
  return MachineFamily(
    family_id="energy_cube",
    machine_type="energy_cube",
    label="Energy Cube",
    connectable_cable="universal_cable",
    machine_lines=[
      MachineLine(
        mod_id="mekanism",
        factory_block_ids=[
          (tier.title(), f"{tier}_energy_cube") for tier in MEKANISM_TIERS
        ] + [("Creative", "mekanism:creative_energy_cube")],
      ),
      MachineLine(
        mod_id="mekanism_extras",
        factory_block_ids=[
          (tier.title(), f"{tier}_energy_cube") for tier in MEKANISM_EXTRAS_TIERS
        ],
      ),
      MachineLine(
        mod_id="evolvedmekanism",
        factory_block_ids=[
          (tier.title(), f"{tier}_energy_cube") for tier in EVOLVED_MEKANISM_TIERS
        ],
      ),
    ],
  )

def _make_fluid_tank_family() -> MachineFamily:
  return MachineFamily(
    family_id="fluid_tank",
    machine_type="fluid_tank",
    label="Fluid Tank",
    connectable_cable="mechanical_pipe",
    machine_lines=[
      MachineLine(
        mod_id="mekanism",
        factory_block_ids=[
          (tier.title(), f"{tier}_fluid_tank") for tier in MEKANISM_TIERS
        ] + [("Creative", "creative_fluid_tank")],
      ),
      MachineLine(
        mod_id="mekanism_extras",
        factory_block_ids=[
          (tier.title(), f"{tier}_fluid_tank") for tier in MEKANISM_EXTRAS_TIERS
        ],
      ),
      MachineLine(
        mod_id="evolvedmekanism",
        factory_block_ids=[
          (tier.title(), f"{tier}_fluid_tank") for tier in EVOLVED_MEKANISM_TIERS
        ],
      ),
    ],
  )

def _make_chemical_tank_family() -> MachineFamily:
  return MachineFamily(
    family_id="chemical_tank",
    machine_type="chemical_tank",
    label="Chemical Tank",
    connectable_cable="pressurized_tube",
    machine_lines=[
      MachineLine(
        mod_id="mekanism",
        factory_block_ids=[
          (tier.title(), f"{tier}_chemical_tank") for tier in MEKANISM_TIERS
        ] + [("Creative", "creative_chemical_tank")],
      ),
      MachineLine(
        mod_id="mekanism_extras",
        factory_block_ids=[
          (tier.title(), f"{tier}_chemical_tank") for tier in MEKANISM_EXTRAS_TIERS
        ],
      ),
      MachineLine(
        mod_id="evolvedmekanism",
        factory_block_ids=[
          (tier.title(), f"{tier}_chemical_tank") for tier in EVOLVED_MEKANISM_TIERS
        ],
      ),
    ],
  )

def _make_thermodynamic_conductor_family() -> MachineFamily:
  return MachineFamily(
    family_id="thermodynamic_conductor",
    machine_type="thermodynamic_conductor",
    label="Thermodynamic Conductor",
    machine_lines=[
      MachineLine(
        mod_id="mekanism",
        factory_block_ids=[
          (tier.title(), f"{tier}_thermodynamic_conductor") for tier in MEKANISM_TIERS
        ],
      ),
      MachineLine(
        mod_id="mekanism_extras",
        factory_block_ids=[
          (tier.title(), f"{tier}_thermodynamic_conductor") for tier in MEKANISM_EXTRAS_TIERS
        ],
      ),
      MachineLine(
        mod_id="evolvedmekanism",
        factory_block_ids=[
          (tier.title(), f"{tier}_thermodynamic_conductor") for tier in EVOLVED_MEKANISM_TIERS
        ],
      ),
      MachineLine(
        mod_id="emextras",
        factory_block_ids=[
          (tier.title(), f"{tier}_thermodynamic_conductor") for tier in EVOLVED_MEKANISM_EXTRAS_TIERS
        ],
      ),
    ],
  )

AE2_BLOCKS = {
    "controller":    "ae2:controller",
    "cable":         "ae2:cable_bus",
    "creative_cell": "ae2:creative_energy_cell",
    "drive":         "ae2:drive",
}

MACHINES: list[MachineFamily] = [
  _make_smelter_family(),
  _make_energy_cube_family(),
  _make_fluid_tank_family(),
  _make_chemical_tank_family(),
  _make_thermodynamic_conductor_family(),
]

# Initialize block registry from MACHINES definitions
def _init_registry(mods_dir: Path) -> None:
  """Load registry from mod jar files."""
  global REGISTRY
  REGISTRY = registry_loader.load_registry(mods_dir)
  print(f"[INFO] Loaded registry: {len(REGISTRY)} mods, {sum(len(v) for v in REGISTRY.values())} total items")

# ─────────────────────────────────────────────────────────────────────────────
#  LAYOUT CONSTANTS
# ─────────────────────────────────────────────────────────────────────────────

BLOCK_SPACING = 1  # X spacing between blocks in a row
MOD_GAP = 3        # X gap between mods

AE2_Z_OFFSET = -2     # AE2 area at negative Z
MEKANISM_Z_START = 2   # Mekanism area starts at positive Z


# ─────────────────────────────────────────────────────────────────────────────
#  DATAPACK GENERATOR
# ─────────────────────────────────────────────────────────────────────────────

# Data pack format per Minecraft version (see https://minecraft.wiki/w/Pack_format).
# Only the versions this project's test instances actually target are listed; add an
# entry here whenever a new target Minecraft version is introduced (see gradle.properties).
PACK_FORMAT_BY_MC_VERSION = {
  "1.20.1": 15,
  "1.21.1": 48,
}

# Fallback used when the target Minecraft version can't be detected (see
# _detect_pack_format) or isn't in the table above. Kept at the previous hardcoded
# value for backward compatibility, but callers are warned so a stale/wrong pack_format
# doesn't silently cause "Unknown function" errors in-game (an incompatible/too-old
# pack_format can prevent the datapack from being loaded at all).
DEFAULT_PACK_FORMAT = 26


def _detect_pack_format(world_dir: Path) -> int:
  """
  Detects the correct data pack_format for the Minecraft version of the CurseForge
  instance that owns world_dir (world_dir.parent.parent), by reading its
  minecraftinstance.json's "gameVersion" field. Falls back to DEFAULT_PACK_FORMAT with a
  warning if that file is missing or its version isn't in PACK_FORMAT_BY_MC_VERSION,
  since a wrong pack_format is a common cause of a generated datapack silently failing to
  load (manifesting as e.g. "Unknown function" when running the setup function).
  """
  instance_file = world_dir.parent.parent / "minecraftinstance.json"
  if not instance_file.exists():
    print(f"[WARN] {instance_file} not found; assuming pack_format {DEFAULT_PACK_FORMAT}")
    return DEFAULT_PACK_FORMAT
  try:
    game_version = json.loads(instance_file.read_text(encoding="utf-8")).get("gameVersion")
  except (OSError, json.JSONDecodeError) as e:
    print(f"[WARN] Failed to read {instance_file}: {e}; assuming pack_format {DEFAULT_PACK_FORMAT}")
    return DEFAULT_PACK_FORMAT
  pack_format = PACK_FORMAT_BY_MC_VERSION.get(game_version)
  if pack_format is None:
    print(f"[WARN] No known pack_format for Minecraft {game_version!r}; "
          f"assuming {DEFAULT_PACK_FORMAT}. Add an entry to PACK_FORMAT_BY_MC_VERSION.")
    return DEFAULT_PACK_FORMAT
  print(f"[INFO] Detected Minecraft {game_version} -> pack_format {pack_format}")
  return pack_format


# Minecraft 1.21 (pack_format 48) renamed several legacy datapack folders to singular form,
# including "functions" -> "function" (see https://minecraft.wiki/w/Pack_format). Below this
# pack_format, the old plural folder name must be used instead.
FUNCTION_FOLDER_SINGULAR_SINCE_PACK_FORMAT = 48


def generate(output_dir: Path, mods_dir: Path, world_dir: Path) -> None:
  _init_registry(mods_dir)

  pack_format = _detect_pack_format(world_dir)
  function_folder = "function" if pack_format >= FUNCTION_FOLDER_SINGULAR_SINCE_PACK_FORMAT else "functions"

  ns = "mfi_test"
  func_dir = output_dir / "data" / ns / function_folder
  func_dir.mkdir(parents=True, exist_ok=True)

  # pack.mcmeta
  _write_json(output_dir / "pack.mcmeta", {
    "pack": {
      "description": "MFI test environment (v2)",
      "pack_format": pack_format,
    }
  })

  # build setup function
  cmds: list[str] = [
    "# MFI Test Environment Setup (v2)",
    "# Generated by scripts/gen_test_datapack_v2.py",
    "gamerule doDaylightCycle false",
    "gamerule doWeatherCycle false",
    "gamerule doMobSpawning false",
    "time set day",
    "weather clear",
  ]

  # calculate dimensions
  mekanism_z_end = MEKANISM_Z_START + sum(len(family.machine_lines) * 3 for family in MACHINES)
  total_z = mekanism_z_end + 5

  max_x = max(
    max(sum(len(line.factory_block_ids) for line in family.machine_lines) for family in MACHINES),
    20,
  ) * BLOCK_SPACING

  cmds += _build_ae2_section()
  cmds += _build_mekanism_section()

  _write_function(func_dir / "setup.mcfunction", cmds)

  print(f"OK  Datapack written to: {output_dir.resolve()}")
  print(f"    Copy to <world>/datapacks/, then run /reload and /function {ns}:setup")


def _build_ae2_section() -> list[str]:
  cmds = ["# === AE2 Area ==="]

  z = AE2_Z_OFFSET
  # ME infrastructure in a line
  cmds.append(f"setblock ~0 ~ ~{z} ae2:creative_energy_cell")
  cmds.append(f"setblock ~1 ~ ~{z} ae2:controller")
  cmds.append(f"setblock ~1 ~1 ~{z} ae2:drive")
  cmds.append("give @p ae2:fluix_glass_cable 64")
  cmds.append("give @p ae2:pattern_encoding_terminal")
  cmds.append("give @p ae2:crafting_terminal")
  cmds.append("give @p ae2:item_storage_cell_256k")
  cmds.append("say AE2 setup complete. See inventory for manual setup instructions.")

  return cmds + [""]


def _build_mekanism_section() -> list[str]:
  cmds = ["# === Mekanism Area ===", ""]

  z = MEKANISM_Z_START

  for family in MACHINES:
    cmds += [f"# {family.label} ({family.family_id})"]

    # Iterate through machine lines
    for line in family.machine_lines:
      x = 0
      has_valid_block = False
      
      # Add non-factory block if specified
      if line.non_factory_block_id is not None:
        full_id = f"{line.mod_id}:{line.non_factory_block_id}"
        if is_block_registered(full_id):
          cmds.append(f"setblock ~{x} ~ ~{z} {full_id}")
          has_valid_block = True
          if family.connectable_cable is not None:
            cable_id = f"{line.mod_id}:basic_{family.connectable_cable}"
            if is_block_registered(cable_id):
              cmds.append(f"setblock ~{x} ~1 ~{z} {cable_id}")
        else:
          cmds.append(f"# [SKIPPED] {full_id} not found (mod not loaded)")
      x += BLOCK_SPACING

      for tier_label, block_id in line.factory_block_ids:
        full_id = f"{line.mod_id}:{block_id}"
        if is_block_registered(full_id):
          cmds.append(f"setblock ~{x} ~ ~{z} {full_id}")
          has_valid_block = True
          # Add cable/pipe above if specified
          if family.connectable_cable is not None:
            tier = block_id.split("_")[0]  # e.g. "basic" from "basic_smelting_factory"
            cable_id = f"{line.mod_id}:{tier}_{family.connectable_cable}"
            if is_block_registered(cable_id):
              cmds.append(f"setblock ~{x} ~1 ~{z} {cable_id}")
        else:
          cmds.append(f"# [SKIPPED] {full_id} not found (mod not loaded)")
        x += BLOCK_SPACING
      
      if has_valid_block:
        z += 3  # 1 for blocks + 2 for air gap

    cmds.append("")

  return cmds


def _write_json(path: Path, data: object) -> None:
  path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def _write_function(path: Path, lines: list[str]) -> None:
  path.write_text("\n".join(lines) + "\n", encoding="utf-8", newline="\n")


# ─────────────────────────────────────────────────────────────────────────────
#  ENTRY POINT
# ─────────────────────────────────────────────────────────────────────────────

if __name__ == "__main__":
  if len(sys.argv) < 2:
    print("Usage: gen_test_datapack_v2.py <world_dir>")
    print("  world_dir: Minecraft world directory (e.g. .../saves/New World)")
    print("  Automatically finds mods in parent directory and outputs to world/datapacks/")
    sys.exit(1)

  world_dir = Path(sys.argv[1]).resolve()

  if not world_dir.exists():
    print(f"ERROR: World directory not found: {world_dir}")
    sys.exit(1)

  # Infer mods dir: world is at .../Instances/{instance_name}/saves/{world_name}
  # So parent.parent is the instance root (or Instance directory)
  mods_dir = world_dir.parent.parent / "mods"

  if not mods_dir.exists():
    print(f"ERROR: Mods directory not found: {mods_dir}")
    print(f"       Expected at: {mods_dir.resolve()}")
    sys.exit(1)

  # Output to world/datapacks/mfi-test-setup
  output_dir = world_dir / "datapacks" / "mfi-test-setup"

  generate(output_dir, mods_dir, world_dir)
