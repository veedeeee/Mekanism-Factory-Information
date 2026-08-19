#!/usr/bin/env python3
"""
Mekanism Factory Information - Test Datapack Generator

Generates a Minecraft datapack that sets up a test environment for verifying:
  - Jade factory "Lines" tooltip
  - Jade cable / fluid-tank spec tooltip
  - AE2 terminal machine-family grouping

The setup is DYNAMIC: every /setblock and /item command is attempted for ALL
known mods. If a mod is not loaded the command fails silently (the block
position stays as air, or the chest slot stays empty). Only the blocks/items
that are actually registered appear in the world.

Adding support for a new addon mod: add a ModEntry to MODS below, rebuild.

Usage
-----
  python scripts/gen_test_datapack.py [output_dir]

  output_dir : where to write the datapack (default: ./mfi-test-setup/)

Installation
------------
  1. Run this script
  2. Copy the generated folder into your world's  datapacks/  directory
  3. In-game: /reload
  4. Stand anywhere and run:  /function mfi_test:setup

Notes
-----
- Coordinates are all relative (~) to the player position when the
  function is called.  Use a flat test world and call it at a clean spot.
- AE2 Terminal and Import Bus are cable-bus *parts* and cannot be placed
  with /setblock.  The script gives them to the player as items along with
  brief placement instructions in a written book.
- pack_format 61 = 1.21.x NeoForge  |  pack_format 61 also works on 1.20.1
  Forge with a data-pack compat shim; otherwise change to 26 for 1.20.1.
"""

from __future__ import annotations

import json
import sys
from dataclasses import dataclass, field
from pathlib import Path
from textwrap import dedent
from typing import Optional

# ─────────────────────────────────────────────────────────────────────────────
#  DATA MODEL
# ─────────────────────────────────────────────────────────────────────────────

@dataclass
class FactoryFamily:
    """One factory type (e.g. 'enriching') across all tiers for one mod."""
    family_id: str          # short key used for tag names, e.g. "enriching"
    tiers: list[tuple[str, str]]   # [(tier_label, block_id), ...]
    required: bool = True   # False → "required":false in item tags (addon mods)


@dataclass
class ModEntry:
    """
    One mod's contribution to the test setup.

    factory_families : list of FactoryFamily (all factory types for this mod)
    cable_tiers      : [(label, block_id), ...] – universal cables
    tank_tiers       : [(label, block_id), ...] – fluid tanks
    plain_machines   : [(label, block_id), ...] – non-factory single machines
    required         : if False, every block/item from this mod is treated as
                       optional (silent failure when mod is absent)
    """
    mod_id: str
    label: str
    factory_families: list[FactoryFamily] = field(default_factory=list)
    cable_tiers: list[tuple[str, str]] = field(default_factory=list)
    tank_tiers: list[tuple[str, str]] = field(default_factory=list)
    plain_machines: list[tuple[str, str]] = field(default_factory=list)
    required: bool = True

    def all_factory_tiers(self) -> list[tuple[str, str]]:
        """Flat list of (label, block_id) for every factory across all families."""
        result = []
        for fam in self.factory_families:
            result.extend(fam.tiers)
        return result


# ─────────────────────────────────────────────────────────────────────────────
#  MOD REGISTRY
#  Add new addon mods here.  Set required=False for optional addon blocks.
# ─────────────────────────────────────────────────────────────────────────────

# Factory types used as the Jade-test row selector.
# Each family is listed in MODS.factory_families.
FACTORY_TYPES = [
    "enriching",
    "smelting",
    "crushing",
    "compressing",
    "purifying",
    "injecting",
    "infusing",
    "combining",
    "sawing",
]

def _mek_factory_family(family_id: str, required: bool = True) -> FactoryFamily:
    """Build a FactoryFamily for standard Mekanism factory tiers."""
    rc = family_id.replace("ing", "ing")  # keeps name as-is
    return FactoryFamily(
        family_id=family_id,
        required=required,
        tiers=[
            ("Basic (3)",    f"mekanism:basic_{family_id}_factory"),
            ("Advanced (5)", f"mekanism:advanced_{family_id}_factory"),
            ("Elite (7)",    f"mekanism:elite_{family_id}_factory"),
            ("Ultimate (9)", f"mekanism:ultimate_{family_id}_factory"),
        ],
    )

def _extras_factory_family(family_id: str) -> FactoryFamily:
    return FactoryFamily(
        family_id=family_id,
        required=False,
        tiers=[
            ("Absolute (11)", f"mekanism_extras:absolute_{family_id}_factory"),
            ("Supreme (13)",  f"mekanism_extras:supreme_{family_id}_factory"),
            ("Cosmic (15)",   f"mekanism_extras:cosmic_{family_id}_factory"),
            ("Infinite (17)", f"mekanism_extras:infinite_{family_id}_factory"),
        ],
    )

# Plain (non-factory) Mekanism machines — their "Lines" count is always 1.
MEK_PLAIN_MACHINES: list[tuple[str, str]] = [
    ("Enrichment Chamber", "mekanism:enrichment_chamber"),
    ("Crusher",            "mekanism:crusher"),
    ("Energized Smelter",  "mekanism:energized_smelter"),
    ("Metallurgic Infuser","mekanism:metallurgic_infuser"),
    ("Purification Chamber","mekanism:purification_chamber"),
    ("Chemical Injection Chamber","mekanism:chemical_injection_chamber"),
    ("Precision Sawmill",  "mekanism:precision_sawmill"),
    ("Chemical Compressor","mekanism:chemical_compressor"),
    ("Combining Factory (standalone)",  "mekanism:combining_factory"),  # no plain equivalent
]

MODS: list[ModEntry] = [
    ModEntry(
        mod_id="mekanism",
        label="Mekanism",
        required=True,
        plain_machines=MEK_PLAIN_MACHINES,
        factory_families=[_mek_factory_family(ft) for ft in FACTORY_TYPES],
        cable_tiers=[
            ("Basic",    "mekanism:basic_universal_cable"),
            ("Advanced", "mekanism:advanced_universal_cable"),
            ("Elite",    "mekanism:elite_universal_cable"),
            ("Ultimate", "mekanism:ultimate_universal_cable"),
        ],
        tank_tiers=[
            ("Basic",    "mekanism:basic_fluid_tank"),
            ("Advanced", "mekanism:advanced_fluid_tank"),
            ("Elite",    "mekanism:elite_fluid_tank"),
            ("Ultimate", "mekanism:ultimate_fluid_tank"),
            ("Creative", "mekanism:creative_fluid_tank"),
        ],
    ),
    ModEntry(
        mod_id="mekanism_extras",
        label="MekanismExtras",
        required=False,
        factory_families=[_extras_factory_family(ft) for ft in FACTORY_TYPES],
        cable_tiers=[
            ("Absolute", "mekanism_extras:absolute_universal_cable"),
            ("Supreme",  "mekanism_extras:supreme_universal_cable"),
            ("Cosmic",   "mekanism_extras:cosmic_universal_cable"),
            ("Infinite", "mekanism_extras:infinite_universal_cable"),
        ],
    ),
    # ── EvolvedMekanismExtras ──────────────────────────────────────────────
    # TODO: verify exact tier names and mod_id in-game (use /give or F3+H)
    ModEntry(
        mod_id="emextras",
        label="EvolvedMekanismExtras",
        required=False,
        factory_families=[
            FactoryFamily(
                family_id=ft,
                required=False,
                tiers=[
                    # Placeholder — adjust tier names after checking in-game
                    ("EM-Absolute", f"emextras:absolute_{ft}_factory"),
                    ("EM-Supreme",  f"emextras:supreme_{ft}_factory"),
                    ("EM-Cosmic",   f"emextras:cosmic_{ft}_factory"),
                    ("EM-Infinite", f"emextras:infinite_{ft}_factory"),
                ],
            )
            for ft in FACTORY_TYPES
        ],
    ),
    # ── Astral Mekanism & Energistics ─────────────────────────────────────
    # TODO: verify exact block IDs (factory type name component and tier labels)
    ModEntry(
        mod_id="astral_mekanism",
        label="Astral Mekanism & Energistics",
        required=False,
        factory_families=[
            FactoryFamily(
                family_id=ft,
                required=False,
                tiers=[
                    # Placeholder — confirm tier names after checking in-game
                    ("AME-T1", f"astral_mekanism:essential_{ft}_factory"),
                    ("AME-T2", f"astral_mekanism:basic_astral_{ft}_factory"),
                    ("AME-T3", f"astral_mekanism:advanced_astral_{ft}_factory"),
                    ("AME-T4", f"astral_mekanism:elite_astral_{ft}_factory"),
                ],
            )
            for ft in FACTORY_TYPES
        ],
    ),
]


# ─────────────────────────────────────────────────────────────────────────────
#  LAYOUT CONSTANTS
# ─────────────────────────────────────────────────────────────────────────────

BLOCK_SPACING  = 2   # x spacing between blocks in a row
MOD_GAP        = 4   # extra x gap between mods in the same row
ROW_Z_SPACING  = 3   # z spacing between rows

# AE2 section origin (relative offsets from the /function call position)
AE2_Z_OFFSET   = 5   # z offset for the AE2 section (placed south of Jade rows)

# ─────────────────────────────────────────────────────────────────────────────
#  MCFUNCTION BUILDERS
# ─────────────────────────────────────────────────────────────────────────────

def _comment(text: str) -> str:
    return f"# {text}"


def _section(title: str) -> list[str]:
    bar = "─" * len(title)
    return ["", f"# ┌{bar}┐", f"# │ {title} │", f"# └{bar}┘"]


def build_platform(total_x: int, total_z: int) -> list[str]:
    return [
        *_section("Platform"),
        f"fill ~-1 ~-1 ~-1 ~{total_x + 1} ~-1 ~{total_z + 1} minecraft:smooth_stone",
        f"fill ~-1 ~ ~-1 ~{total_x + 1} ~10 ~{total_z + 1} minecraft:air",
    ]


def build_jade_rows(family_id: str) -> list[str]:
    """
    Build one row of setblock commands for a single factory family (e.g. enriching).
    All mods share the same row (z), separated by MOD_GAP.
    Returns (commands, max_x_used).
    """
    cmds: list[str] = []
    x = 0

    for mod in MODS:
        fam = next((f for f in mod.factory_families if f.family_id == family_id), None)
        if fam is None:
            continue
        for _label, block_id in fam.tiers:
            cmds.append(f"setblock ~{x} ~ ~0 {block_id}")
            x += BLOCK_SPACING
        x += MOD_GAP

    return cmds


def build_jade_section() -> list[str]:
    cmds = [*_section("Jade Test — Factory Lines")]
    cmds.append(_comment("One row per factory family. Tiers increase along +X."))
    cmds.append(_comment("Unknown block IDs (unloaded mods) produce a warning but are skipped."))

    # plain machines row (z=0)
    cmds += ["", _comment("Row: plain single machines (Lines = 1)")]
    x = 0
    for mod in MODS:
        for _label, block_id in mod.plain_machines:
            cmds.append(f"setblock ~{x} ~ ~0 {block_id}")
            x += BLOCK_SPACING
        if mod.plain_machines:
            x += MOD_GAP

    z = ROW_Z_SPACING
    for ft in FACTORY_TYPES:
        cmds += ["", _comment(f"Row z={z}: {ft} factories")]
        row = build_jade_rows(ft)
        cmds += [f"execute positioned ~ ~ ~{z} run {cmd}" for cmd in row]
        z += ROW_Z_SPACING

    # cables
    cmds += ["", _comment(f"Row z={z}: universal cables")]
    x = 0
    for mod in MODS:
        for _label, block_id in mod.cable_tiers:
            cmds.append(f"execute positioned ~{x} ~ ~{z} run setblock ~ ~ ~ {block_id}")
            x += BLOCK_SPACING
        if mod.cable_tiers:
            x += MOD_GAP
    z += ROW_Z_SPACING

    # fluid tanks
    cmds += ["", _comment(f"Row z={z}: fluid tanks")]
    x = 0
    for mod in MODS:
        for _label, block_id in mod.tank_tiers:
            cmds.append(f"execute positioned ~{x} ~ ~{z} run setblock ~ ~ ~ {block_id}")
            x += BLOCK_SPACING
        if mod.tank_tiers:
            x += MOD_GAP

    return cmds, z


def build_ae2_section(chest_slot_items: list[tuple[str, str]]) -> list[str]:
    """
    Builds the AE2 test area.

    Layout (all offsets relative to AE2_Z_OFFSET along Z, 0 along X):
      [0,0]  Creative Energy Cell  (infinite power)
      [1,0]  ME Controller
      [2,0]  ME Drive
      [3,0]  fluix_glass_cable  ── connects everything
      [4,0]  fluix_glass_cable
      [5,0]  (player places ME Terminal cable bus part here)
      [3,-2] Chest  (factory items for import)
      [3,-1] (player places ME Import Bus cable bus part here, facing +Z toward chest)

    Items given to player:
      - ME Terminal (part item)   ← attach to cable at [5,0]
      - ME Import Bus (part item) ← attach to cable at [3,-1], facing chest
      - 256k Storage Cell × 3    ← insert into ME Drive
    """
    z = AE2_Z_OFFSET
    cmds = [*_section("AE2 Test — ME Network Setup")]
    cmds.append(_comment("Infrastructure placed automatically."))
    cmds.append(_comment("See the written book in your inventory for manual steps."))

    # infrastructure blocks
    cmds += [""]
    infra = [
        (0, z, "ae2:creative_energy_cell"),
        (1, z, "ae2:controller"),
        (2, z, "ae2:drive"),
        (3, z, "ae2:cable_bus"),
        (4, z, "ae2:cable_bus"),
        (5, z, "ae2:cable_bus"),   # player attaches terminal here
        (3, z - 1, "ae2:cable_bus"),  # player attaches import bus here
        (3, z - 2, "minecraft:chest"),
    ]
    for (ix, iz, block_id) in infra:
        cmds.append(f"setblock ~{ix} ~ ~{iz} {block_id}")

    # fill chest with factory items
    cmds += ["", _comment("Fill chest with factory block items (one per slot).")]
    cmds.append(_comment("Unloaded mod items silently leave the slot empty."))
    for slot, (label, item_id) in enumerate(chest_slot_items[:27]):
        cmds.append(
            f"item replace block ~3 ~ ~{z - 2} container.{slot} with {item_id} 64"
        )

    # give items to player
    cmds += ["", _comment("Give player the cable bus parts and storage cells.")]
    cmds.append("give @p ae2:terminal 1")
    cmds.append("give @p ae2:import_bus 1")
    cmds.append("give @p ae2:storage_cell_256k 3")

    # written book with instructions
    cmds += ["", _comment("Instruction book")]
    pages = _ae2_instruction_pages()
    cmds.append(_give_book_command(pages))

    return cmds


def _ae2_instruction_pages() -> list[str]:
    """JSON strings for each page of the written instruction book."""
    return [
        json.dumps([
            {"text": "AE2 Test Setup\n\n", "bold": True, "color": "gold"},
            {"text": "3 manual steps remain:\n\n"},
            {"text": "1. "},
            {"text": "Insert"},
            {"text": " a 256k cell into the ME Drive.\n\n"},
            {"text": "2. "},
            {"text": "Right-click the cable bus at +X=5"},
            {"text": " with the ME Terminal item.\n\n"},
            {"text": "3. "},
            {"text": "Right-click the cable bus at +X=3, -Z=1"},
            {"text": " with the ME Import Bus item\n(face toward chest = south/+Z)."},
        ]),
        json.dumps([
            {"text": "Verifying grouping\n\n", "bold": True},
            {"text": "Once the network is online:\n"},
            {"text": "• Open the ME Terminal\n"},
            {"text": "• Enable grouping in AE2 sort options\n"},
            {"text": "• Factory families should each be sorted by tier order\n\n"},
            {"text": "Jade tooltips\n\n", "bold": True},
            {"text": "Hover over each factory block in the Jade test rows to verify the Lines count tooltip."},
        ]),
    ]


def _give_book_command(pages: list[str]) -> str:
    pages_nbt = "[" + ",".join(f"'{p}'" for p in pages) + "]"
    return (
        "give @p minecraft:written_book"
        f'{{title:"MFI Test Guide",author:"mfi_test",pages:{pages_nbt}}}'
        " 1"
    )


# ─────────────────────────────────────────────────────────────────────────────
#  ITEM TAG BUILDER (optional output — for reference and future use)
# ─────────────────────────────────────────────────────────────────────────────

def build_item_tag_all_factories() -> dict:
    """
    Builds a Minecraft item tag containing every factory block item across all
    known mods.  Items from addon mods have "required":false so missing entries
    are silently ignored at tag load time — no warnings for unloaded mods.
    """
    values = []
    for mod in MODS:
        for label, block_id in mod.all_factory_tiers():
            if mod.required:
                values.append(block_id)
            else:
                values.append({"id": block_id, "required": False})
    return {"values": values}


# ─────────────────────────────────────────────────────────────────────────────
#  DATAPACK GENERATOR
# ─────────────────────────────────────────────────────────────────────────────

PACK_FORMAT = 61  # 1.21.x; change to 26 for 1.20.1


def generate(output_dir: Path) -> None:
    ns = "mfi_test"
    func_dir = output_dir / "data" / ns / "function"
    tag_dir = output_dir / "data" / ns / "tags" / "item"
    func_dir.mkdir(parents=True, exist_ok=True)
    tag_dir.mkdir(parents=True, exist_ok=True)

    # pack.mcmeta
    _write_json(output_dir / "pack.mcmeta", {
        "pack": {
            "description": "MFI test setup — Mekanism Factory Information",
            "pack_format": PACK_FORMAT,
        }
    })

    # item tag
    _write_json(tag_dir / "all_factory_blocks.json", build_item_tag_all_factories())

    # collect chest items: enriching + smelting factories across all mods
    chest_items: list[tuple[str, str]] = []
    for mod in MODS:
        for fam in mod.factory_families:
            if fam.family_id in ("enriching", "smelting", "crushing"):
                chest_items.extend(fam.tiers)

    # build master setup function
    cmds: list[str] = [
        "# MFI test setup",
        "# Generated by scripts/gen_test_datapack.py — do not edit by hand.",
        "",
    ]

    jade_cmds, last_jade_z = build_jade_section()
    total_z = last_jade_z + AE2_Z_OFFSET + 10

    # estimate x width
    max_tiers = max(
        (sum(len(f.tiers) for f in mod.factory_families) + len(mod.plain_machines))
        for mod in MODS
    )
    total_x = max_tiers * BLOCK_SPACING + len(MODS) * MOD_GAP

    cmds += build_platform(total_x, total_z)
    cmds += jade_cmds
    cmds += build_ae2_section(chest_items)

    _write_function(func_dir / "setup.mcfunction", cmds)

    print(f"OK  Datapack written to: {output_dir.resolve()}")
    print(f"    Copy to <world>/datapacks/, then run /reload and /function {ns}:setup")


def _write_json(path: Path, data: object) -> None:
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def _write_function(path: Path, lines: list[str]) -> None:
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


# ─────────────────────────────────────────────────────────────────────────────
#  ENTRY POINT
# ─────────────────────────────────────────────────────────────────────────────

if __name__ == "__main__":
    out = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("mfi-test-setup")
    generate(out)
