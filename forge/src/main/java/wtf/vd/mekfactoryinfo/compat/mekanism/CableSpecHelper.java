package wtf.vd.mekfactoryinfo.compat.mekanism;

import mekanism.api.tier.AlloyTier;
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
 * Forge 1.20.1 version of CableSpecHelper. Uses {@code FloatingLong} for cable capacity
 * (Mekanism 1.20.1 returns {@code FloatingLong} rather than {@code long} for energy values) and
 * {@code AlloyTier.getBaseTier()} (no {@code IAlloyTier.getBaseTierLevel()} in 1.20.1).
 *
 * @see wtf.vd.mekfactoryinfo.compat.mekanism.TierAttributeHelper
 */
public final class CableSpecHelper {

    private CableSpecHelper() {
    }

    /**
     * A transmitter's specs. Semantics vary by transmitter type:
     * <ul>
     *   <li>Cable: capacity (Joules) + transfer rate (J/t)</li>
     *   <li>Pipe: capacity (mB) + pull amount (mB/t)</li>
     *   <li>Tube: capacity (mB) + pull amount (mB/t)</li>
     *   <li>Transporter: pull amount (items/tick) + speed (per half-second)</li>
     * </ul>
     */
    public record TransmitterSpec(long capacity, long rate) {
    }

    @Nullable
    public static TransmitterSpec getCurrentSpec(BlockState state) {
        return getCurrentSpec(state, null);
    }

    /**
     * Block-entity-aware variant that reads Cable/Pipe/Tube capacity from the live transmitter's
     * overridable {@code BufferedTransmitter#getCapacity()} rather than the tier enum directly, so
     * addon mods that override it (e.g. MekanismExtras reusing a base tier enum but reporting a
     * larger capacity) are reflected correctly without any mod-specific handling. Transporter has no
     * capacity concept, and none of the rate/pull/speed values have an equivalent override point in
     * Mekanism's transmitter base classes, so those always come from the tier enum (best effort).
     */
    @Nullable
    public static TransmitterSpec getCurrentSpec(BlockState state, @Nullable BlockEntity blockEntity) {
        CableTier cableTier = TierAttributeHelper.getTierSafely(state.getBlockHolder(), CableTier.class);
        if (cableTier != null) {
            long capacity = getLiveCapacity(blockEntity, cableTier.getCableCapacity().longValue());
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

    @Nullable
    public static TransmitterSpec getPreviewSpec(BlockState state, ItemStack heldItem) {
        if (!(heldItem.getItem() instanceof ItemAlloy alloy)) {
            return null;
        }
        AlloyTier alloyTier = alloy.getTier();
        BaseTier alloyBaseTier = alloyTier.getBaseTier();

        CableTier cableTier = TierAttributeHelper.getTierSafely(state.getBlockHolder(), CableTier.class);
        if (cableTier != null && cableTier.getBaseTier().ordinal() + 1 == alloyBaseTier.ordinal()) {
            CableTier nextTier = CableTier.get(alloyBaseTier);
            if (nextTier == null) {
                return null;
            }
            long capacity = nextTier.getCableCapacity().longValue();
            return new TransmitterSpec(capacity, capacity);
        }

        PipeTier pipeTier = TierAttributeHelper.getTierSafely(state.getBlockHolder(), PipeTier.class);
        if (pipeTier != null && pipeTier.getBaseTier().ordinal() + 1 == alloyBaseTier.ordinal()) {
            PipeTier nextTier = PipeTier.get(alloyBaseTier);
            if (nextTier == null) {
                return null;
            }
            return new TransmitterSpec((long) nextTier.getPipeCapacity(), (long) nextTier.getPipePullAmount());
        }

        TubeTier tubeTier = TierAttributeHelper.getTierSafely(state.getBlockHolder(), TubeTier.class);
        if (tubeTier != null && tubeTier.getBaseTier().ordinal() + 1 == alloyBaseTier.ordinal()) {
            TubeTier nextTier = TubeTier.get(alloyBaseTier);
            if (nextTier == null) {
                return null;
            }
            return new TransmitterSpec(nextTier.getTubeCapacity(), nextTier.getTubePullAmount());
        }

        TransporterTier transporterTier = TierAttributeHelper.getTierSafely(state.getBlockHolder(), TransporterTier.class);
        if (transporterTier != null && transporterTier.getBaseTier().ordinal() + 1 == alloyBaseTier.ordinal()) {
            TransporterTier nextTier = TransporterTier.get(alloyBaseTier);
            if (nextTier == null) {
                return null;
            }
            return new TransmitterSpec((long) nextTier.getPullAmount(), (long) nextTier.getSpeed());
        }

        return null;
    }
}
