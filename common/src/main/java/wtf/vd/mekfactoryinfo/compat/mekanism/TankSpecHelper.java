package wtf.vd.mekfactoryinfo.compat.mekanism;

import mekanism.api.tier.BaseTier;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.attribute.AttributeUpgradeable;
import mekanism.common.item.ItemTierInstaller;
import mekanism.common.tier.ChemicalTankTier;
import mekanism.common.tier.EnergyCubeTier;
import mekanism.common.tier.FluidTankTier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Shared, loader-agnostic logic for reading Mekanism Fluid Tank / Chemical Tank / Energy Cube
 * storage specs and for previewing what a held Tier Installer would change them to. Mirrors
 * {@link FactoryLinesHelper}'s approach, but for Tanks and Energy Cubes (which use the same
 * {@code AttributeUpgradeable}/{@code ItemTierInstaller} mechanic as Factories, just with a
 * storage capacity + output rate instead of a Lines count).
 */
public final class TankSpecHelper {

    private TankSpecHelper() {
    }

    /**
     * A storage device's capacity and per-tick transfer rate, both in the device content's base
     * unit (millibuckets). {@link ChemicalTankTier} uses {@code long} internally (much larger
     * tanks than {@link FluidTankTier}'s {@code int}), and {@link EnergyCubeTier} uses
     * {@code long} for Joules, so both are normalized to {@code long} here.
     */
    public record TankSpec(long storage, long output) {
    }

    /**
     * Returns the current storage spec for the given block state, or {@code null} if it isn't a
     * Mekanism Fluid Tank, Chemical Tank, or Energy Cube. Reads the tier enum's stock storage/output;
     * prefer {@link #getCurrentSpec(BlockState, Long)} when a live storage value is available, since
     * addon mods can report a different live storage capacity than the (possibly reused) tier enum.
     */
    @Nullable
    public static TankSpec getCurrentSpec(BlockState state) {
        return getCurrentSpec(state, null);
    }

    /**
     * Variant of {@link #getCurrentSpec(BlockState)} that accepts an already-read live storage value
     * (e.g. from the placed tank/container's own {@code getCapacity()}/{@code getMaxEnergy()}, read
     * by loader-specific code -- see each provider's {@code readLiveStorage}) to use instead of the
     * tier enum's stock value, when available. This is what lets addon mods that report a different
     * live capacity than their (possibly reused) tier enum be reflected correctly, without any
     * mod-specific handling here. The output rate has no equivalent generic accessor in Mekanism's
     * tile entity base class (it's read from the tier inline at use), so it always comes from the
     * tier enum (best effort).
     */
    @Nullable
    public static TankSpec getCurrentSpec(BlockState state, @Nullable Long liveStorage) {
        FluidTankTier fluidTier = TierAttributeHelper.getTierSafely(state.getBlockHolder(), FluidTankTier.class);
        if (fluidTier != null) {
            long storage = liveStorage != null ? liveStorage : fluidTier.getStorage();
            return new TankSpec(storage, fluidTier.getOutput());
        }
        ChemicalTankTier chemicalTier = TierAttributeHelper.getTierSafely(state.getBlockHolder(), ChemicalTankTier.class);
        if (chemicalTier != null) {
            long storage = liveStorage != null ? liveStorage : chemicalTier.getStorage();
            return new TankSpec(storage, chemicalTier.getOutput());
        }
        EnergyCubeTier energyTier = TierAttributeHelper.getTierSafely(state.getBlockHolder(), EnergyCubeTier.class);
        if (energyTier != null) {
            long storage = liveStorage != null ? liveStorage : energyTier.getMaxEnergy();
            return new TankSpec(storage, energyTier.getOutput());
        }
        return null;
    }

    /**
     * Returns {@code true} if the given block state is a Mekanism Energy Cube (uses energy units
     * rather than mB for storage display).
     */
    public static boolean isEnergyCube(BlockState state) {
        return TierAttributeHelper.getTierSafely(state.getBlockHolder(), EnergyCubeTier.class) != null;
    }

    /**
     * If {@code heldItem} is a Tier Installer that could legally upgrade the storage device
     * currently in {@code state}, returns the resulting storage spec after that upgrade. Returns
     * {@code null} if the held item is not a compatible installer for the device's current tier,
     * mirroring {@code ItemTierInstaller}'s own validation (see
     * {@link FactoryLinesHelper#getPreviewLines}).
     */
    @Nullable
    public static TankSpec getPreviewSpec(BlockState state, ItemStack heldItem) {
        Item item = heldItem.getItem();
        if (item instanceof ItemTierInstaller installer) {
            return previewViaVanillaInstaller(state, installer);
        }
        // Addon mods (MekanismExtras' ExtraItemTierInstaller and similarly-shaped future addons)
        // upgrade their own Tanks/Energy Cubes through the exact same ExtraAttributeUpgradeable
        // mechanic MekanismExtras' Factories use (see each block's own registration), so this reuses
        // FactoryLinesHelper's addon Tier Installer reflection to resolve the upgraded block instead
        // of requiring a separate, addon-specific mapping here.
        BlockState upgraded = FactoryLinesHelper.previewAddonInstallerUpgrade(state, item);
        return upgraded == null ? null : getCurrentSpec(upgraded);
    }

    @Nullable
    private static TankSpec previewViaVanillaInstaller(BlockState state, ItemTierInstaller installer) {
        AttributeUpgradeable upgradeable = Attribute.get(state, AttributeUpgradeable.class);
        if (upgradeable == null) {
            return null;
        }
        BaseTier currentBaseTier = Attribute.getBaseTier(state.getBlockHolder());
        if (currentBaseTier != installer.getFromTier() || currentBaseTier == installer.getToTier()) {
            return null;
        }
        BlockState upgraded = upgradeable.upgradeResult(state, installer.getToTier());
        if (upgraded == state) {
            return null;
        }
        return getCurrentSpec(upgraded);
    }
}
