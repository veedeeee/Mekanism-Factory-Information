
# Copilot Instructions

## Project Overview
- The user-facing overview is in the `README.md`. The assistant should read that file to understand the project and its goals.

## Mod features and dependencies
- If the player also installed Applied Energistics 2, the mod will add additional sort order control for AE2 terminals.
- If the player also installed Jade, the mod will add additional tooltip information for Mekanism Factory blocks and Installer items.
- The player can have add-on mods for Mekanism installed. This mod does not require any of them, but it will work with them if they are present.

## Development Commands
- Build all modules: `.\gradlew.bat build --console=plain`
- Compile common + NeoForge only: `.\gradlew.bat :common:compileJava :neoforge:compileJava --console=plain`
- Run NeoForge client: `.\gradlew.bat :neoforge:runClient --console=plain`
- Run Forge client: `.\gradlew.bat :forge:runClient --console=plain`

### Test Environments
- `D:\curseforge\minecraft\Instances`
  - `MFI - Forge 1.20.1\`
  - `MFI - NeoForge 1.21.1\`
