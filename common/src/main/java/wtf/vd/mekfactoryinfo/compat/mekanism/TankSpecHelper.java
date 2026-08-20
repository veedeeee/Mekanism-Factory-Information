package wtf.vd.mekfactoryinfo.compat.mekanism;

import mekanism.api.tier.BaseTier;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.attribute.AttributeUpgradeable;
import mekanism.common.item.ItemTierInstaller;
import mekanism.common.tier.ChemicalTankTier;
import mekanism.common.tier.EnergyCubeTier;
import mekanism.common.tier.FluidTankTier;
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
     * Mekanism Fluid Tank, Chemical Tank, or Energy Cube.
     */
    @Nullable
    public static TankSpec getCurrentSpec(BlockState state) {
        FluidTankTier fluidTier = TierAttributeHelper.getTierSafely(state.getBlockHolder(), FluidTankTier.class);
        if (fluidTier != null) {
            return new TankSpec(fluidTier.getStorage(), fluidTier.getOutput());
        }
        ChemicalTankTier chemicalTier = TierAttributeHelper.getTierSafely(state.getBlockHolder(), ChemicalTankTier.class);
        if (chemicalTier != null) {
            return new TankSpec(chemicalTier.getStorage(), chemicalTier.getOutput());
        }
        EnergyCubeTier energyTier = TierAttributeHelper.getTierSafely(state.getBlockHolder(), EnergyCubeTier.class);
        if (energyTier != null) {
            return new TankSpec(energyTier.getMaxEnergy(), energyTier.getOutput());
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
        if (!(heldItem.getItem() instanceof ItemTierInstaller installer)) {
            return null;
        }
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
