package wtf.vd.mekfactoryinfo.compat.mekanism;

import mekanism.api.tier.BaseTier;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.attribute.AttributeUpgradeable;
import mekanism.common.item.ItemTierInstaller;
import mekanism.common.tier.FactoryTier;
import mekanism.common.tile.factory.TileEntityFactory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Shared, loader-agnostic logic for reading Mekanism Factory "processing Lines" counts and for
 * previewing what a held Tier Installer would change that count to.
 */
public final class FactoryLinesHelper {

    /**
     * The implicit processing Lines count for a regular (non-Factory) Mekanism machine, e.g. a plain
     * Enrichment Chamber. These machines aren't tiered themselves; a Tier Installer upgrades them
     * directly into the corresponding Basic Factory (see {@code mekanism.common.content.blocktype.Machine.FactoryMachine}).
     */
    private static final int SINGLE_MACHINE_LINES = 1;

    private FactoryLinesHelper() {
    }

    /**
     * Returns the number of processing Lines currently shown for the given block: the tier's process
     * count for an actual Factory block entity, {@code 1} for a regular Mekanism machine that could be
     * upgraded into a Factory, or {@code null} if neither applies (nothing to show).
     */
    @Nullable
    public static Integer getCurrentLines(BlockState state, @Nullable BlockEntity blockEntity) {
        if (blockEntity instanceof TileEntityFactory<?> factory) {
            FactoryTier tier = factory.tier;
            return tier == null ? null : tier.processes;
        }
        if (Attribute.has(state, AttributeUpgradeable.class)) {
            return SINGLE_MACHINE_LINES;
        }
        return null;
    }

    /**
     * Block-only variant of {@link #getCurrentLines(BlockState, BlockEntity)} for contexts with no
     * placed block or block entity to inspect, such as an {@code ItemStack} sitting in an AE2 network
     * (used by the Lines-grouping terminal sort feature). Reads the Factory tier (if any) directly
     * off the block type rather than a live {@code TileEntityFactory}.
     */
    @Nullable
    public static Integer getLinesForBlock(Block block) {
        FactoryTier tier = Attribute.getTier(block, FactoryTier.class);
        if (tier != null) {
            return tier.processes;
        }
        if (Attribute.has(block, AttributeUpgradeable.class)) {
            return SINGLE_MACHINE_LINES;
        }
        return null;
    }

    /**
     * If {@code heldItem} is a Tier Installer that could legally upgrade a Factory (or regular
     * machine) block currently in {@code state}, returns the resulting number of processing Lines
     * after that upgrade. Returns {@code null} if the held item is not a compatible installer for the
     * block's current tier (i.e. using it here would have no effect, mirroring {@code ItemTierInstaller}'s
     * own validation).
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
        // A regular (non-tiered) machine has no BaseTier attribute at all (currentBaseTier == null);
        // it is a valid target for the Basic Installer, whose fromTier is also null, mirroring
        // ItemTierInstaller#useOn's own equality check.
        if (currentBaseTier != installer.getFromTier() || currentBaseTier == installer.getToTier()) {
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
