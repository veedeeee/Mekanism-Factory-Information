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
 * Forge 1.20.1 version of TankSpecHelper. Uses {@code FloatingLong.longValue()} for energy
 * values (Mekanism 1.20.1 returns {@code FloatingLong} for {@code EnergyCubeTier} methods) and
 * {@code Attribute.getBaseTier(Block)} (the 1.21.1 {@code Holder<Block>} overload doesn't exist
 * in 1.20.1 Mekanism).
 */
public final class TankSpecHelper {

    private TankSpecHelper() {
    }

    /**
     * A storage device's capacity and per-tick transfer rate in the device's base unit (mB for
     * fluid/chemical tanks, Joules for energy cubes), normalised to {@code long}.
     */
    public record TankSpec(long storage, long output) {
    }

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
            return new TankSpec(energyTier.getMaxEnergy().longValue(), energyTier.getOutput().longValue());
        }
        return null;
    }

    public static boolean isEnergyCube(BlockState state) {
        return TierAttributeHelper.getTierSafely(state.getBlockHolder(), EnergyCubeTier.class) != null;
    }

    @Nullable
    public static TankSpec getPreviewSpec(BlockState state, ItemStack heldItem) {
        if (!(heldItem.getItem() instanceof ItemTierInstaller installer)) {
            return null;
        }
        AttributeUpgradeable upgradeable = Attribute.get(state, AttributeUpgradeable.class);
        if (upgradeable == null) {
            return null;
        }
        // In Mekanism 1.20.1, getBaseTier takes Block (not Holder<Block>).
        BaseTier currentBaseTier = Attribute.getBaseTier(state.getBlock());
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
