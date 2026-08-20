#!/usr/bin/env python3
"""
Load block/item IDs from mod jar lang files.

Scans mod directory, extracts en_us.json from each jar,
and builds a registry of available block/item IDs.
"""

import json
import re
import sys
from pathlib import Path
from typing import Dict, Set
from zipfile import ZipFile


def load_registry(mods_dir: Path) -> Dict[str, Set[str]]:
  """
  Load all available block/item IDs from mod jar files.

  Returns:
    Dict mapping mod_id -> set of block/item names
    e.g. {"mekanism": {"basic_smelting_factory", "advanced_smelting_factory", ...}}
  """
  registry: Dict[str, Set[str]] = {}

  if not mods_dir.exists():
    print(f"[WARN] Mods directory not found: {mods_dir}")
    return registry

  for jar_file in mods_dir.glob("*.jar"):
    try:
      with ZipFile(jar_file, 'r') as jar:
        # Look for en_us.json in assets
        lang_files = [f for f in jar.namelist() if f.endswith("lang/en_us.json")]

        for lang_path in lang_files:
          # Extract mod_id from path: assets/{mod_id}/lang/en_us.json
          parts = lang_path.split("/")
          if len(parts) >= 3 and parts[0] == "assets":
            mod_id = parts[1]

            # Read and parse JSON
            with jar.open(lang_path) as f:
              lang_json = json.load(f)

            # Extract block/item IDs: (item|block).{mod_id}.{item_name}
            pattern = r"^(?:item|block)\.(\w+)\.(.+)$"

            for key in lang_json.keys():
              match = re.match(pattern, key)
              if match:
                key_mod_id = match.group(1)
                item_name = match.group(2)

                if key_mod_id not in registry:
                  registry[key_mod_id] = set()
                registry[key_mod_id].add(item_name)

    except Exception as e:
      print(f"[WARN] Error processing {jar_file.name}: {e}")

  return registry


def is_registered(registry: Dict[str, Set[str]], block_id: str) -> bool:
  """
  Check if a block ID is registered.

  Args:
    registry: Registry dict from load_registry()
    block_id: Block ID in format "mod_id:block_name"

  Returns:
    True if registered, False otherwise
  """
  if ":" not in block_id:
    return False

  mod_id, item_name = block_id.split(":", 1)
  return mod_id in registry and item_name in registry[mod_id]


if __name__ == "__main__":
  if len(sys.argv) < 2:
    print("Usage: registry_loader.py <mods_dir>")
    sys.exit(1)

  mods_dir = Path(sys.argv[1])
  registry = load_registry(mods_dir)

  print("=== Registry Summary ===")
  for mod_id, items in sorted(registry.items()):
    print(f"{mod_id}: {len(items)} items")

  print("\n=== Sample Blocks (Mekanism) ===")
  if "mekanism" in registry:
    for item in sorted(registry["mekanism"])[:10]:
      print(f"  mekanism:{item}")

