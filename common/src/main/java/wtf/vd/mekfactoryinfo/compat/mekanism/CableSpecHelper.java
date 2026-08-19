package wtf.vd.mekfactoryinfo.compat.mekanism;

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
     * tiered transmitter (Cable, Pipe, Tube, or Transporter).
     */
    @Nullable
    public static TransmitterSpec getCurrentSpec(BlockState state) {
        CableTier cableTier = TierAttributeHelper.getTierSafely(state.getBlockHolder(), CableTier.class);
        if (cableTier != null) {
            return new TransmitterSpec(cableTier.getCableCapacity(), cableTier.getCableCapacity());
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
