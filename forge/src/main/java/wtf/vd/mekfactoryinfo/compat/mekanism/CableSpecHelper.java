package wtf.vd.mekfactoryinfo.compat.mekanism;

import mekanism.api.tier.AlloyTier;
import mekanism.api.tier.BaseTier;
import mekanism.common.item.ItemAlloy;
import mekanism.common.tier.CableTier;
import mekanism.common.tier.PipeTier;
import mekanism.common.tier.TransporterTier;
import mekanism.common.tier.TubeTier;
import net.minecraft.world.item.ItemStack;
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
        CableTier cableTier = TierAttributeHelper.getTierSafely(state.getBlockHolder(), CableTier.class);
        if (cableTier != null) {
            long capacity = cableTier.getCableCapacity().longValue();
            return new TransmitterSpec(capacity, capacity);
        }
        PipeTier pipeTier = TierAttributeHelper.getTierSafely(state.getBlockHolder(), PipeTier.class);
        if (pipeTier != null) {
            return new TransmitterSpec((long) pipeTier.getPipeCapacity(), (long) pipeTier.getPipePullAmount());
        }
        TubeTier tubeTier = TierAttributeHelper.getTierSafely(state.getBlockHolder(), TubeTier.class);
        if (tubeTier != null) {
            return new TransmitterSpec(tubeTier.getTubeCapacity(), tubeTier.getTubePullAmount());
        }
        TransporterTier transporterTier = TierAttributeHelper.getTierSafely(state.getBlockHolder(), TransporterTier.class);
        if (transporterTier != null) {
            return new TransmitterSpec((long) transporterTier.getPullAmount(), (long) transporterTier.getSpeed());
        }
        return null;
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
