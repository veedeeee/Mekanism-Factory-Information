# User Test Checklist Template

This is a reusable template for release-readiness user testing. It is **not** meant to be
copied into a version-specific file and committed to the repository — instead:

1. Copy the checklist body below into the release PR description (the PR created for
   `develop` or `release/vX.Y.Z` → `master`), filling in the "Changes in this release" section
   with the actual features/fixes being validated.
2. Have the user run through the checklist and record results directly in the PR body/comments.
3. Once the release PR is merged, the filled-in checklist lives in the PR history — no
   separate file needs to be kept in the repository.

---

## User Test Checklist — vX.Y.Z

### Changes in this release

- (list the features/fixes being validated in this release)

Target loaders: **Forge 1.20.1** / **NeoForge 1.21.1**
(verify on both unless a change is clearly loader-specific)

---

### Prerequisites

- [ ] Mod jar deployed to the test environment for the target loader/version
- [ ] Mekanism (core) installed and matching the target loader/version
- [ ] Applied Energistics 2 installed (required for AE2 terminal tests)
- [ ] Jade installed (required for Jade tooltip tests)
- [ ] Sufficient items available: one Factory block of each tier (Basic → Ultimate), matching Tier
      Installers, Alloy items (Infused / Reinforced / Atomic), Fluid/Chemical Tanks of multiple
      tiers, Energy Cubes of multiple tiers, Universal Cables, Mechanical Pipes, Pressurized Tubes,
      Logistical Transporters

---

### 1. Factory Lines — Item Tooltip

- [ ] Mousing over a **Basic Factory** shows a tooltip with `Lines: 3`
- [ ] Mousing over an **Advanced Factory** shows `Lines: 5`
- [ ] Mousing over an **Elite Factory** shows `Lines: 7`
- [ ] Mousing over an **Ultimate Factory** shows `Lines: 9`
- [ ] Mousing over a **Tier Installer** shows the resulting Lines count for the target factory tier

---

### 2. Factory Lines — Jade HUD (block look)

- [ ] Looking at a **Basic Factory** in the world shows `Lines: 3` in the Jade HUD
- [ ] Looking at an **Advanced Factory** shows `Lines: 5`
- [ ] Holding a compatible **Tier Installer** (Sneak+Target) while looking at a factory shows a
      before → after preview (e.g. `Lines: 3 → 5`)
- [ ] Holding an incompatible Tier Installer shows no preview (no crash)

---

### 3. Tank / Energy Cube — Jade HUD spec preview

#### Fluid Tank / Chemical Tank

- [ ] Looking at a **Basic Fluid Tank** shows correct capacity and output in mB / mB/t
- [ ] Looking at a **Basic Chemical Tank** shows correct capacity and output
- [ ] Holding a compatible **Tier Installer** (Sneak+Target) shows before → after preview for
      capacity and output rate
- [ ] Preview is absent when holding an incompatible or wrong-tier installer

#### Energy Cube

- [ ] Looking at a **Basic Energy Cube** shows capacity and output in FE units (e.g. `1.6 MFE`,
      `800 kFE/t`) — **not** raw numbers
- [ ] Looking at a **Ultimate Energy Cube** shows correspondingly higher values
- [ ] Holding a compatible **Tier Installer** (Sneak+Target) shows before → after preview in FE
      units
- [ ] Values match the Mekanism item tooltip for the same tier

---

### 4. Transmitter — Jade HUD spec preview

#### Universal Cable

- [ ] Looking at a **Basic Universal Cable** shows capacity in FE units (e.g. `3.2 kFE`) and rate
      in `FE/t` — **not** raw Joule values
- [ ] Holding **Infused Alloy** (Sneak+Target) on a Basic Cable shows preview:
      Basic → Advanced values
- [ ] Values match the in-game item tooltip for the cable tier

#### Mechanical Pipe

- [ ] Looking at a **Basic Mechanical Pipe** shows capacity in mB and rate in mB/t
- [ ] Holding **Infused Alloy** (Sneak+Target) shows upgrade preview

#### Pressurized Tube

- [ ] Looking at a **Basic Pressurized Tube** shows capacity in mB and rate in mB/t
- [ ] Holding **Infused Alloy** (Sneak+Target) shows upgrade preview

#### Logistical Transporter

- [ ] Looking at a **Basic Logistical Transporter** shows `Speed: 1.0 m/s` and `Pump Rate: 2.0/s`
- [ ] Looking at an **Advanced Logistical Transporter** shows higher speed and pump rate values
- [ ] Holding **Infused Alloy** (Sneak+Target) on a Basic Transporter shows upgrade preview

#### Thermodynamic Conductor

- [ ] Conductor is **excluded** — no Jade spec panel appears (conductors use conduction/insulation
      stats, not a capacity tier)

---

### 5. AE2 Terminal — Lines grouping sort

- [ ] Opening the AE2 ME Terminal shows the **Lines grouping toggle button** in the toolbar
- [ ] Button tooltip describes the enabled/disabled state in the appropriate language
- [ ] Clicking the button toggles Lines grouping on/off without crash
- [ ] With Lines grouping **enabled**, Factory blocks are grouped by their Lines count
      (Basic/Advanced/Elite/Ultimate ordered together)
- [ ] Tier Installer items are similarly grouped with their target factory tier
- [ ] Items unrelated to Mekanism Factories (e.g. torches, ingots) appear without crash and are
      not mis-grouped
- [ ] **Persistence — world exit:** enable Lines grouping, exit the world (return to title), re-open
      the world → Lines grouping state is preserved ✓
- [ ] **Persistence — client exit:** enable Lines grouping, close Minecraft entirely, relaunch,
      re-enter the world → Lines grouping state is preserved ✓

---

### 6. Compatibility

- [ ] No crash log when Mekanism is installed without AE2 or Jade
- [ ] No crash log when Mekanism + AE2 is installed without Jade
- [ ] No crash log when Mekanism + Jade is installed without AE2

---

### Result summary

| Section | Forge 1.20.1 | NeoForge 1.21.1 | Notes |
| --- | --- | --- | --- |
| 1. Factory Lines (tooltip) | OK / NG | OK / NG | |
| 2. Factory Lines (Jade) | OK / NG | OK / NG | |
| 3. Tank / Energy Cube (Jade) | OK / NG | OK / NG | |
| 4. Transmitter (Jade) | OK / NG | OK / NG | |
| 5. AE2 Lines grouping | OK / NG | OK / NG | |
| 6. Compatibility | OK / NG | OK / NG | |

If any item is NG, attach the relevant environment's `logs/latest.log`.

