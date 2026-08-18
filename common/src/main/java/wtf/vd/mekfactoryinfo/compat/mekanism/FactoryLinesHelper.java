package wtf.vd.mekfactoryinfo.compat.mekanism;

import mekanism.api.tier.BaseTier;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.attribute.AttributeUpgradeable;
import mekanism.common.item.ItemTierInstaller;
import mekanism.common.tier.FactoryTier;
import mekanism.common.tile.factory.TileEntityFactory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Shared, loader-agnostic logic for reading Mekanism Factory "processing Lines" counts and for
 * previewing what a held Tier Installer would change that count to.
 */
public final class FactoryLinesHelper {

    private FactoryLinesHelper() {
    }

    /**
     * Returns the number of processing Lines for the given Factory block entity's current tier,
     * or {@code null} if its tier is unknown.
     */
    @Nullable
    public static Integer getCurrentLines(TileEntityFactory<?> factory) {
        FactoryTier tier = factory.tier;
        return tier == null ? null : tier.processes;
    }

    /**
     * If {@code heldItem} is a Tier Installer that could legally upgrade a Factory block currently in
     * {@code state}, returns the resulting number of processing Lines after that upgrade. Returns
     * {@code null} if the held item is not a compatible installer for the block's current tier (i.e.
     * using it here would have no effect, mirroring {@code ItemTierInstaller}'s own validation).
     */
    @Nullable
    public static Integer getPreviewLines(BlockState state, ItemStack heldItem) {
        if (!(heldItem.getItem() instanceof ItemTierInstaller installer)) {
            return null;
        }
        AttributeUpgradeable upgradeable = Attribute.get(state, AttributeUpgradeable.class);
        if (upgradeable == null) {
            return null;
        }
        BaseTier currentBaseTier = Attribute.getBaseTier(state.getBlockHolder());
        if (currentBaseTier == null || currentBaseTier != installer.getFromTier() || currentBaseTier == installer.getToTier()) {
            // Not a valid upgrade path for this installer, same check ItemTierInstaller#useOn performs.
            return null;
        }
        BlockState upgraded = upgradeable.upgradeResult(state, installer.getToTier());
        if (upgraded == state) {
            return null;
        }
        FactoryTier afterTier = Attribute.getTier(upgraded.getBlockHolder(), FactoryTier.class);
        return afterTier == null ? null : afterTier.processes;
    }
}
