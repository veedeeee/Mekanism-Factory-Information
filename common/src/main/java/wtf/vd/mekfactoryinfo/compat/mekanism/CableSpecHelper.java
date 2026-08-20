package wtf.vd.mekfactoryinfo.compat.mekanism;

import mekanism.api.tier.BaseTier;
import mekanism.common.content.network.transmitter.BufferedTransmitter;
import mekanism.common.content.network.transmitter.Transmitter;
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
     */
    @Nullable
    public static TransmitterSpec getPreviewSpec(BlockState state, ItemStack heldItem) {
        if (!(heldItem.getItem() instanceof ItemAlloy alloy)) {
            return null;
        }
        int alloyTierLevel = alloy.getTier().getBaseTierLevel();

        CableTier cableTier = TierAttributeHelper.getTierSafely(state.getBlockHolder(), CableTier.class);
        if (cableTier != null && cableTier.getBaseTierLevel() + 1 == alloyTierLevel) {
            CableTier nextTier = CableTier.get(BaseTier.getTier(alloyTierLevel));
            if (nextTier == null) {
                return null;
            }
            return new TransmitterSpec(nextTier.getCableCapacity(), nextTier.getCableCapacity());
        }

        PipeTier pipeTier = TierAttributeHelper.getTierSafely(state.getBlockHolder(), PipeTier.class);
        if (pipeTier != null && pipeTier.getBaseTierLevel() + 1 == alloyTierLevel) {
            PipeTier nextTier = PipeTier.get(BaseTier.getTier(alloyTierLevel));
            if (nextTier == null) {
                return null;
            }
            return new TransmitterSpec((long) nextTier.getPipeCapacity(), (long) nextTier.getPipePullAmount());
        }

        TubeTier tubeTier = TierAttributeHelper.getTierSafely(state.getBlockHolder(), TubeTier.class);
        if (tubeTier != null && tubeTier.getBaseTierLevel() + 1 == alloyTierLevel) {
            TubeTier nextTier = TubeTier.get(BaseTier.getTier(alloyTierLevel));
            if (nextTier == null) {
                return null;
            }
            return new TransmitterSpec(nextTier.getTubeCapacity(), nextTier.getTubePullAmount());
        }

        TransporterTier transporterTier = TierAttributeHelper.getTierSafely(state.getBlockHolder(), TransporterTier.class);
        if (transporterTier != null && transporterTier.getBaseTierLevel() + 1 == alloyTierLevel) {
            TransporterTier nextTier = TransporterTier.get(BaseTier.getTier(alloyTierLevel));
            if (nextTier == null) {
                return null;
            }
            return new TransmitterSpec((long) nextTier.getPullAmount(), (long) nextTier.getSpeed());
        }

        return null;
    }
}
