#!/usr/bin/env python3
"""
Mekanism Factory Information - Test Environment Datapack Generator v2

Generates a Minecraft datapack for a comprehensive test environment with:
  - AE2 Area: ME network infrastructure + large chest for items
  - Mekanism Area: organized by machine type (Smelter, Energy Cube, Fluid Tank, 
                   Chemical Tank, Thermodynamic Conductor), with Mekanism base
                   tier + addon tiers in separate rows

Layout (top-down):
  Z = -10..0: AE2 Area (north) — ME blocks + chest
  Z = 5+:     Mekanism Area (south) — organized test machines

Each machine family (Smelter, Energy Cube, etc.) occupies multiple Z rows:
  - Row 0: Mekanism basic → ultimate
  - Row 1 (2n): Air gap
  - Row 2 (2n+1): First addon's machines
  - Row 3 (2n+2): Air gap
  - Row 4 (2n+3): Second addon's machines
  - etc.
"""

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
EVOLVED_MEKANISM_EXTRAS_TIERS = ["absoluite_overclocked", "supreme_quantum", "cosmic_dense", "infinite_multiversal"]
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

BLOCK_SPACING = 2  # X spacing between blocks in a row
MOD_GAP = 3        # X gap between mods

AE2_Z_OFFSET = -2     # AE2 area at negative Z
MEKANISM_Z_START = 2   # Mekanism area starts at positive Z


# ─────────────────────────────────────────────────────────────────────────────
#  DATAPACK GENERATOR
# ─────────────────────────────────────────────────────────────────────────────

PACK_FORMAT = 26  # 1.20.1


def generate(output_dir: Path, mods_dir: Path) -> None:
  _init_registry(mods_dir)
  
  ns = "mfi_test"
  func_dir = output_dir / "data" / ns / "functions"
  func_dir.mkdir(parents=True, exist_ok=True)

  # pack.mcmeta
  _write_json(output_dir / "pack.mcmeta", {
    "pack": {
      "description": "MFI test environment (v2)",
      "pack_format": PACK_FORMAT,
    }
  })

  # build setup function
  cmds: list[str] = [
    "# MFI Test Environment Setup (v2)",
    "# Generated by scripts/gen_test_datapack_v2.py",
    "gamerule doDaylightCycle false",
    "gamerule doWeatherCycle false",
    "time set day",
    "weather clear",
  ]

  # calculate dimensions
  mekanism_z_end = MEKANISM_Z_START + (len(MACHINES) * 3)
  total_z = mekanism_z_end + 5

  max_x = max(
    max(sum(len(line.factory_block_ids) for line in family.machine_lines) for family in MACHINES),
    20,
  ) * BLOCK_SPACING

  cmds += _build_platform(max_x, total_z)
  cmds += _build_ae2_section()
  cmds += _build_mekanism_section()

  _write_function(func_dir / "setup.mcfunction", cmds)

  print(f"OK  Datapack written to: {output_dir.resolve()}")
  print(f"    Copy to <world>/datapacks/, then run /reload and /function {ns}:setup")


def _build_platform(total_x: int, total_z: int) -> list[str]:
  return [
    "# === Platform ===",
    f"fill ~-5 ~-1 ~{AE2_Z_OFFSET - 5} ~{total_x + 5} ~-1 ~{total_z + 5} minecraft:stone",
    f"fill ~-5 ~ ~{AE2_Z_OFFSET - 5} ~{total_x + 5} ~20 ~{total_z + 5} minecraft:air",
    "",
  ]


def _build_ae2_section() -> list[str]:
  cmds = ["# === AE2 Area ==="]

  z = AE2_Z_OFFSET
  # ME infrastructure in a line
  cmds.append(f"setblock ~0 ~ ~{z} ae2:creative_energy_cell")
  cmds.append(f"setblock ~0 ~ ~{z+1} ae2:controller")
  cmds.append(f"setblock ~0 ~1 ~{z+1} ae2:drive")
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
        z += 2  # 1 for blocks + 1 for air gap

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
    print("Usage: gen_test_datapack_v2.py <output_dir> [mods_dir]")
    print("  output_dir: where to write the datapack")
    print("  mods_dir:   path to mods folder (default: ../mods)")
    sys.exit(1)
  
  out = Path(sys.argv[1])
  mods = Path(sys.argv[2]) if len(sys.argv) > 2 else Path("../mods")
  
  if not mods.exists():
    print(f"ERROR: Mods directory not found: {mods.resolve()}")
    sys.exit(1)
  
  generate(out, mods)
