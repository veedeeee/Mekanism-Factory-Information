package wtf.vd.mekfactoryinfo.compat.mekanism;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import mekanism.api.tier.BaseTier;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.attribute.AttributeTier;
import mekanism.common.block.attribute.AttributeUpgradeable;
import mekanism.common.block.interfaces.ITypeBlock;
import mekanism.common.item.ItemTierInstaller;
import mekanism.common.tier.FactoryTier;
import mekanism.common.tile.factory.TileEntityFactory;
import mekanism.common.tile.prefab.TileEntityConfigurableMachine;
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
        // Addon mods (MekanismExtras, EvolvedMekanismExtras, Astral Mekanism, etc.) use their own
        // factory BE classes that extend TileEntityConfigurableMachine with a public `tier` field
        // whose type has a public int `processes` field — same structural convention, different types.
        // Any other block entity built on this same shared Mekanism base class (used across the whole
        // ecosystem for single-recipe, energy-driven processing machines, as opposed to Tanks/Cables/
        // Pipes) is -- by construction -- a genuine single processing line, whether or not it happens
        // to also expose the (unrelated) Tier Installer upgrade mechanic.
        if (blockEntity instanceof TileEntityConfigurableMachine) {
            Integer lines = getProcessesFromBE(blockEntity);
            return lines != null ? lines : SINGLE_MACHINE_LINES;
        }
        if (hasUpgradeableAttribute(state.getBlockHolder().value()) && !hasAnyTierAttribute(state.getBlockHolder().value())) {
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
        FactoryTier tier = TierAttributeHelper.getTierSafely(block, FactoryTier.class);
        if (tier != null) {
            return tier.processes;
        }
        // Addon mods use custom Attribute subclasses (not AttributeTier) that are Java records with
        // a tier() method; the tier object has a public int processes field.
        if (block instanceof ITypeBlock typeBlock) {
            for (Attribute attr : typeBlock.getType().getAll()) {
                Integer lines = getProcessesFromAttribute(attr);
                if (lines != null) {
                    return lines;
                }
            }
        }
        // Same TileEntityConfigurableMachine recognition as getCurrentLines above, but without a live
        // block entity to inspect. Loader-specific callers that have a way to build a throwaway
        // block entity for this block (e.g. via Mekanism's own createDummyBlockEntity() mechanism)
        // should call {@link #getLinesForBlockEntity(BlockEntity)} with it as an additional fallback;
        // this method has no loader-agnostic way to construct one itself.
        if (hasUpgradeableAttribute(block) && !hasAnyTierAttribute(block)) {
            return SINGLE_MACHINE_LINES;
        }
        return null;
    }

    /**
     * {@code BlockEntity}-only counterpart to {@link #getLinesForBlock(Block)}: recognizes any block
     * entity built on Mekanism's shared {@link TileEntityConfigurableMachine} base class (used across
     * the whole ecosystem for single-recipe, energy-driven processing machines, as opposed to
     * Tanks/Cables/Pipes) as a genuine processing-line machine, regardless of whether it also exposes
     * the (unrelated) Tier Installer upgrade mechanic. Intended for callers that can only obtain a
     * throwaway/dummy block entity (no live world), e.g. via Mekanism's own
     * {@code IHasTileEntity#createDummyBlockEntity()}.
     */
    @Nullable
    public static Integer getLinesForBlockEntity(@Nullable BlockEntity blockEntity) {
        if (blockEntity instanceof TileEntityConfigurableMachine) {
            Integer lines = getProcessesFromBE(blockEntity);
            return lines != null ? lines : SINGLE_MACHINE_LINES;
        }
        return null;
    }

    /**
     * Returns {@code true} if the block carries an upgrade-via-Tier-Installer attribute: the base
     * {@link AttributeUpgradeable}, or an addon's custom equivalent (e.g. MekanismExtras'
     * {@code ExtraAttributeUpgradeable}) -- detected structurally by declaring an
     * {@code upgradeResult} method, the same convention the base attribute uses, rather than
     * requiring the addon class to actually implement {@link AttributeUpgradeable}.
     */
    private static boolean hasUpgradeableAttribute(Block block) {
        if (Attribute.has(block, AttributeUpgradeable.class)) {
            return true;
        }
        if (block instanceof ITypeBlock typeBlock) {
            for (Attribute attr : typeBlock.getType().getAll()) {
                for (Method method : attr.getClass().getMethods()) {
                    if (method.getName().equals("upgradeResult")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if the block already carries some other tier attribute (base
     * {@link AttributeTier} or an addon's custom tier-carrying {@link Attribute}, e.g. a Fluid Tank's,
     * Cable's, or Pipe's tier). Such blocks are themselves tiered storage/transmitter devices that
     * happen to reuse the same {@code AttributeUpgradeable} upgrade mechanic as Factories -- they are
     * <em>not</em> a plain single-line machine, so {@code SINGLE_MACHINE_LINES} must not apply to them.
     * This guard is what makes it safe to register the Jade Factory Lines provider against a broad
     * common ancestor block class that also covers Tanks/Energy Cubes (see
     * {@code MekFactoryInfoJadePlugin}).
     */
    private static boolean hasAnyTierAttribute(Block block) {
        if (Attribute.get(block, AttributeTier.class) != null) {
            return true;
        }
        if (block instanceof ITypeBlock typeBlock) {
            for (Attribute attr : typeBlock.getType().getAll()) {
                if (getAddonTierObject(attr) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Attempts to read an addon's custom tier object off an {@link Attribute} via its {@code tier()}
     * accessor (the same convention as {@link #getProcessesFromAttribute}), without requiring a
     * {@code processes} field. Returns {@code null} on any mismatch.
     */
    @Nullable
    private static Object getAddonTierObject(Attribute attr) {
        try {
            Method tierMethod = attr.getClass().getMethod("tier");
            return tierMethod.invoke(attr);
        } catch (ReflectiveOperationException e) {
            return null;
        }
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
        AttributeTier<?> tierAttribute = Attribute.get(state, AttributeTier.class);
        BaseTier currentBaseTier = tierAttribute == null ? null : tierAttribute.tier().getBaseTier();
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
        FactoryTier afterTier = TierAttributeHelper.getTierSafely(upgraded.getBlockHolder(), FactoryTier.class);
        return afterTier == null ? null : afterTier.processes;
    }

    /**
     * Attempts to read a {@code processes} count from an addon factory block entity via reflection.
     * Addon mods follow the convention of exposing a public {@code tier} field on their factory BE,
     * whose type has a public {@code int processes} field. Returns {@code null} on any mismatch.
     */
    @Nullable
    private static Integer getProcessesFromBE(BlockEntity be) {
        try {
            Field tierField = be.getClass().getField("tier");
            Object tierObj = tierField.get(be);
            if (tierObj == null) {
                return null;
            }
            Field processesField = tierObj.getClass().getField("processes");
            return (Integer) processesField.get(tierObj);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    /**
     * Attempts to read a {@code processes} count from an addon factory block's custom tier attribute
     * via reflection. Addon mods implement {@link Attribute} as a Java record or class with a
     * {@code tier()} or {@code tier} accessor; the returned tier object has a public {@code int processes} field.
     * Returns {@code null} on any mismatch (e.g. cable tiers, non-factory attributes).
     */
    @Nullable
    private static Integer getProcessesFromAttribute(Attribute attr) {
        try {
            Method tierMethod = attr.getClass().getMethod("tier");
            Object tierObj = tierMethod.invoke(attr);
            if (tierObj == null) {
                return null;
            }
            Field processesField = tierObj.getClass().getField("processes");
            return (Integer) processesField.get(tierObj);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
