package wtf.vd.mekfactoryinfo.compat.mekanism;

import mekanism.api.tier.BaseTier;
import mekanism.api.tier.ITier;
import mekanism.common.content.network.transmitter.BufferedTransmitter;
import mekanism.common.content.network.transmitter.LogisticalTransporter;
import mekanism.common.content.network.transmitter.MechanicalPipe;
import mekanism.common.content.network.transmitter.Transmitter;
import mekanism.common.content.network.transmitter.UniversalCable;
import mekanism.common.item.ItemAlloy;
import mekanism.common.tier.CableTier;
import mekanism.common.tier.PipeTier;
import mekanism.common.tier.TransporterTier;
import mekanism.common.tier.TubeTier;
import mekanism.common.tile.transmitter.TileEntityTransmitter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Shared, loader-agnostic logic for reading Mekanism transmitter specs and for previewing what a
 * held Alloy would change them to. Supports:
 * <ul>
 *   <li>Universal Cable (CableTier): capacity + transfer rate</li>
 *   <li>Mechanical Pipe (PipeTier): capacity + pull amount</li>
 *   <li>Pressurized Tube (TubeTier): capacity + pull amount</li>
 *   <li>Logistical/Restrictive/Diversion Transporter (TransporterTier): pull amount + speed</li>
 * </ul>
 * <p>
 * Note: Thermodynamic Conductor is intentionally excluded; it uses ConductorTier which is not a
 * "capacity" tier (uses conduction and insulation stats instead), so no meaningful Jade tooltip
 * would apply.
 */
public final class CableSpecHelper {

    private CableSpecHelper() {
    }

    /**
     * A transmitter's specs. Meaning varies by transmitter type:
     * <ul>
     *   <li>Cable: capacity (Joules) + transfer rate (J/t)</li>
     *   <li>Pipe: capacity (mB) + pull amount (mB/t)</li>
     *   <li>Tube: capacity (mB) + pull amount (mB/t)</li>
     *   <li>Transporter: pull amount (items/tick) + speed (per half-second)</li>
     * </ul>
     */
    public record TransmitterSpec(long capacity, long rate) {
    }

    /**
     * Returns the current specs for the given block state, or {@code null} if it isn't a Mekanism
     * tiered transmitter (Cable, Pipe, Tube, or Transporter). Reads the tier enum's stock capacity;
     * prefer {@link #getCurrentSpec(BlockState, BlockEntity)} when a block entity is available, since
     * addon mods (e.g. MekanismExtras) can override a transmitter's live capacity to a different value
     * than its (reused) tier enum reports.
     */
    @Nullable
    public static TransmitterSpec getCurrentSpec(BlockState state) {
        return getCurrentSpec(state, null);
    }

    /**
     * Block-entity-aware variant of {@link #getCurrentSpec(BlockState)}. For Cable/Pipe/Tube, the
     * capacity is read from the live transmitter object's overridable {@code getCapacity()} (declared
     * on {@code BufferedTransmitter}) rather than the tier enum directly, so addon mods that override
     * it (e.g. MekanismExtras reusing a base tier enum but reporting a larger capacity) are reflected
     * correctly without any mod-specific handling. The transfer rate (Cable) / pull amount (Pipe,
     * Tube, Transporter) / speed (Transporter) have no such override point in Mekanism's transmitter
     * base classes, so those remain read from the tier enum (best effort; addons that swap the tier's
     * values via a private static lookup, rather than overriding a virtual method, cannot be reflected
     * generically).
     */
    @Nullable
    public static TransmitterSpec getCurrentSpec(BlockState state, @Nullable BlockEntity blockEntity) {
        CableTier cableTier = TierAttributeHelper.getTierSafely(state.getBlockHolder(), CableTier.class);
        if (cableTier != null) {
            long capacity = getLiveCapacity(blockEntity, cableTier.getCableCapacity());
            return new TransmitterSpec(capacity, capacity);
        }
        PipeTier pipeTier = TierAttributeHelper.getTierSafely(state.getBlockHolder(), PipeTier.class);
        if (pipeTier != null) {
            long capacity = getLiveCapacity(blockEntity, (long) pipeTier.getPipeCapacity());
            return new TransmitterSpec(capacity, (long) pipeTier.getPipePullAmount());
        }
        TubeTier tubeTier = TierAttributeHelper.getTierSafely(state.getBlockHolder(), TubeTier.class);
        if (tubeTier != null) {
            long capacity = getLiveCapacity(blockEntity, tubeTier.getTubeCapacity());
            return new TransmitterSpec(capacity, tubeTier.getTubePullAmount());
        }
        TransporterTier transporterTier = TierAttributeHelper.getTierSafely(state.getBlockHolder(), TransporterTier.class);
        if (transporterTier != null) {
            return new TransmitterSpec((long) transporterTier.getPullAmount(), (long) transporterTier.getSpeed());
        }
        return null;
    }

    /**
     * Reads the live capacity off {@code blockEntity}'s {@code Transmitter} via the overridable
     * {@code BufferedTransmitter#getCapacity()} virtual method, falling back to {@code tierCapacity}
     * (the stock tier enum's value) if no block entity is available or it isn't a buffered
     * transmitter (e.g. Transporter, which has no capacity concept).
     */
    private static long getLiveCapacity(@Nullable BlockEntity blockEntity, long tierCapacity) {
        if (blockEntity instanceof TileEntityTransmitter te) {
            Transmitter<?, ?, ?> transmitter = te.getTransmitter();
            if (transmitter instanceof BufferedTransmitter<?, ?, ?, ?> buffered) {
                return buffered.getCapacity();
            }
        }
        return tierCapacity;
    }

    /**
     * If {@code heldItem} is an Alloy that could legally upgrade the transmitter currently in
     * {@code state} (i.e. its tier is exactly one level above the transmitter's current tier,
     * mirroring {@code IUpgradeableTransmitter#canUpgrade}), returns the resulting specs after that
     * upgrade. Returns {@code null} otherwise.
     *
     * @deprecated prefer {@link #getPreviewSpec(BlockState, BlockEntity, ItemStack)}: without a
     * block entity, this cannot distinguish a genuine vanilla transmitter from an addon's Java
     * subclass that merely reuses one of Mekanism's {@code BaseTier} constants for unrelated
     * bookkeeping (see MekanismExtras' {@code CTier}/{@code PTier}/etc, which remap
     * {@code BaseTier.BASIC/ADVANCED/ELITE/ULTIMATE} to Absolute/Supreme/Cosmic/Infinite) but is not
     * actually upgradeable by a plain Alloy.
     */
    @Deprecated
    @Nullable
    public static TransmitterSpec getPreviewSpec(BlockState state, ItemStack heldItem) {
        return getPreviewSpec(state, null, heldItem);
    }

    /**
     * Block-entity-aware variant of {@link #getPreviewSpec(BlockState, ItemStack)}. Requires that
     * {@code blockEntity}'s live {@code Transmitter} object is a genuine instance of one of
     * Mekanism's own transmitter classes ({@link UniversalCable}, {@link MechanicalPipe},
     * PressurizedTube/BoxedPressurizedTube, {@link LogisticalTransporter}) -- i.e. its own package is
     * Mekanism's, not an addon's -- before returning a preview.
     * <p>
     * This is what filters out MekanismExtras/EvolvedMekanismExtras's transmitters (e.g.
     * {@code ExtraUniversalCable extends UniversalCable}): they nominally reuse one of Mekanism's own
     * {@code BaseTier} constants (see the class doc above), which would otherwise make {@link
     * TierRank#isImmediatelyAbove} think a plain vanilla Alloy could upgrade them one tier -- but
     * those addons wire actual upgrades through a completely separate, addon-specific
     * upgradeable-transmitter interface and tier system that a vanilla Alloy can never trigger
     * in-game, so showing a preview here would be a false positive. Evolved Mekanism, by contrast,
     * Mixins its extra tiers directly onto Mekanism's own transmitter classes (no Java subclass), so
     * its transmitters still pass this check and correctly continue to show previews.
     */
    @Nullable
    public static TransmitterSpec getPreviewSpec(BlockState state, @Nullable BlockEntity blockEntity, ItemStack heldItem) {
        if (!(heldItem.getItem() instanceof ItemAlloy alloy)) {
            return null;
        }
        if (!isGenuineVanillaTransmitter(blockEntity)) {
            return null;
        }
        // NeoForge's ItemAlloy#getTier() returns IAlloyTier (no getBaseTier()), only
        // getBaseTierLevel(); round-trip through BaseTier.getTier(int) to recover the enum constant.
        BaseTier alloyBaseTier = BaseTier.getTier(alloy.getTier().getBaseTierLevel());

        CableTier cableTier = TierAttributeHelper.getTierSafely(state.getBlockHolder(), CableTier.class);
        if (cableTier != null && TierRank.isImmediatelyAbove(alloyBaseTier, cableTier.getBaseTier())) {
            CableTier nextTier = findByBaseTier(CableTier.values(), alloyBaseTier);
            if (nextTier == null) {
                return null;
            }
            return new TransmitterSpec(nextTier.getCableCapacity(), nextTier.getCableCapacity());
        }

        PipeTier pipeTier = TierAttributeHelper.getTierSafely(state.getBlockHolder(), PipeTier.class);
        if (pipeTier != null && TierRank.isImmediatelyAbove(alloyBaseTier, pipeTier.getBaseTier())) {
            PipeTier nextTier = findByBaseTier(PipeTier.values(), alloyBaseTier);
            if (nextTier == null) {
                return null;
            }
            return new TransmitterSpec((long) nextTier.getPipeCapacity(), (long) nextTier.getPipePullAmount());
        }

        TubeTier tubeTier = TierAttributeHelper.getTierSafely(state.getBlockHolder(), TubeTier.class);
        if (tubeTier != null && TierRank.isImmediatelyAbove(alloyBaseTier, tubeTier.getBaseTier())) {
            TubeTier nextTier = findByBaseTier(TubeTier.values(), alloyBaseTier);
            if (nextTier == null) {
                return null;
            }
            return new TransmitterSpec(nextTier.getTubeCapacity(), nextTier.getTubePullAmount());
        }

        TransporterTier transporterTier = TierAttributeHelper.getTierSafely(state.getBlockHolder(), TransporterTier.class);
        if (transporterTier != null && TierRank.isImmediatelyAbove(alloyBaseTier, transporterTier.getBaseTier())) {
            TransporterTier nextTier = findByBaseTier(TransporterTier.values(), alloyBaseTier);
            if (nextTier == null) {
                return null;
            }
            return new TransmitterSpec((long) nextTier.getPullAmount(), (long) nextTier.getSpeed());
        }

        return null;
    }

    /**
     * Returns {@code true} if {@code blockEntity} is a {@link TileEntityTransmitter} whose live
     * {@code Transmitter} object is a genuine, unmodified instance of one of Mekanism's own
     * transmitter classes ({@link UniversalCable}, {@link MechanicalPipe},
     * PressurizedTube/BoxedPressurizedTube, {@link LogisticalTransporter}) -- i.e. its runtime class' package
     * is Mekanism's own {@code mekanism.common.content.network.transmitter} package, not some addon's
     * package.
     * <p>
     * Evolved Mekanism adds its extra tiers via Mixin directly onto these same vanilla classes (no
     * Java subclass), so its transmitters still pass. MekanismExtras/EvolvedMekanismExtras instead
     * define actual Java subclasses (e.g. {@code ExtraUniversalCable extends UniversalCable}) that
     * wire real upgrades through a completely separate, addon-specific mechanism, so those
     * transmitters correctly fail this check even though their block still nominally reuses one of
     * Mekanism's {@code BaseTier} constants (see the class doc above).
     */
    private static boolean isGenuineVanillaTransmitter(@Nullable BlockEntity blockEntity) {
        if (!(blockEntity instanceof TileEntityTransmitter te)) {
            return false;
        }
        Transmitter<?, ?, ?> transmitter = te.getTransmitter();
        if (transmitter == null) {
            return false;
        }
        return transmitter.getClass().getPackageName().equals(UniversalCable.class.getPackageName());
    }

    /**
     * Finds the tier in {@code tiers} whose {@link ITier#getBaseTier()} equals {@code target}, or
     * {@code null} if none matches. Deliberately scans a freshly-obtained {@code values()} array
     * (always reflecting the enum's current, possibly Mixin-extended {@code $VALUES}) rather than
     * calling the tier class's own static {@code get(BaseTier)} helper: that helper loops over a
     * {@code EnumUtils.XXX_TIERS} array that is cached once, the first time it's referenced, as a
     * {@code static final} field -- if that happens to run before an addon's Mixin (e.g. Evolved
     * Mekanism's {@code CableTierMixin}) has appended its own tiers to {@code $VALUES}, the cached
     * array is permanently stale and {@code get(BaseTier)} can never find those addon tiers,
     * silently falling back to the lowest tier's stats instead. Calling {@code values()} directly
     * avoids that caching race entirely.
     */
    @Nullable
    private static <T extends ITier> T findByBaseTier(T[] tiers, BaseTier target) {
        for (T tier : tiers) {
            if (tier.getBaseTier() == target) {
                return tier;
            }
        }
        return null;
    }
}

