# User Test Checklist Template

This is a reusable template for release-readiness user testing. It is **not** meant to be
copied into a version-specific file and committed to the repository — instead:

1. Copy the checklist body below into the release PR description (the PR created for
   `release/vX.Y.Z` -> `master`), filling in the "Fixed in this release" section with the
   actual bugs/changes being validated.
2. Have the user run through the checklist and record results directly in the PR body/comments.
3. Once the release PR is merged, the filled-in checklist lives in the PR history — no
   separate file needs to be kept in the repository.

---

## User Test Checklist — vX.Y.Z

### Fixed in this release
- (list the bugs/changes being validated in this release)

Target loaders: Forge 1.20.1 / NeoForge 1.21.1 / NeoForge 26.1.2 (verify on all three unless a
change is loader-specific)

### Prerequisites

- [ ] Installed Mekanism (and, for add-on-specific checks, MekanismExtras / EvolvedMekanism /
      EvolvedMekanismExtras) matching the target loader/version
- [ ] Installed Applied Energistics 2 (optional dependency)
- [ ] Installed Jade (optional dependency)
- [ ] Built at least one Mekanism Factory block of each tier, and obtained the corresponding
      Tier Installer item(s)

### 1. Basic functionality

- [ ] Jade tooltip on a Factory block shows the correct Lines count for its tier
- [ ] Jade tooltip on a Tier Installer item shows the correct resulting Lines count
- [ ] AE2 terminal sort button cycles to a "Lines" option without affecting NAME/AMOUNT/MOD
- [ ] Sorting by Lines orders Factory blocks / Installer items by their Lines count correctly
- [ ] Items unrelated to Mekanism tiers are handled gracefully when Lines-sorted (no crash)

### 2. Compatibility

- [ ] Works correctly with only Mekanism installed (no AE2, no Jade)
- [ ] Works correctly with Mekanism + AE2 installed (no Jade)
- [ ] Works correctly with Mekanism + Jade installed (no AE2)
- [ ] Works correctly with a Mekanism add-on (MekanismExtras / EvolvedMekanism /
      EvolvedMekanismExtras) installed

### 3. Result summary

| Environment | 1. Basic functionality | 2. Compatibility | Notes |
| --- | --- | --- | --- |
| Forge 1.20.1 | OK / NG | OK / NG | |
| NeoForge 1.21.1 | OK / NG | OK / NG | |
| NeoForge 26.1.2 | OK / NG | OK / NG | |

If any item is NG, attach the relevant environment's `logs/latest.log`.
