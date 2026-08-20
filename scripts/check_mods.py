#!/usr/bin/env python3
import registry_loader
from pathlib import Path

mods_dir = Path('D:\\curseforge\\minecraft\\Instances\\MFI - Forge 1.20.1\\mods')
registry = registry_loader.load_registry(mods_dir)

print('=== AstralMekanism Blocks ===')
items = sorted(registry['astral_mekanism'])
count = 0
for item in items:
  if 'factory' in item or 'cube' in item or 'tank' in item or 'conductor' in item:
    print(f'  astral_mekanism:{item}')
    count += 1

print(f'\nTotal AstralMekanism items: {len(registry["astral_mekanism"])}')

print('\n=== EMExtras (addon for Evolved) ===')
if 'emextras' in registry:
  items = sorted(registry['emextras'])
  count = 0
  for item in items:
    if 'factory' in item or 'cube' in item or 'tank' in item:
      print(f'  emextras:{item}')
      count += 1
  print(f'Total EMExtras items: {len(registry["emextras"])}')
else:
  print('EMExtras not loaded')
