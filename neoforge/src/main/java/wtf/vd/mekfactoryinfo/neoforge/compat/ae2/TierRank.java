package wtf.vd.mekfactoryinfo.neoforge.compat.ae2;

import mekanism.api.tier.BaseTier;

/**
 * Shared ordinal-ranking helper for {@link BaseTier}, used to order tier variants within a family
 * (ascending) and to validate upgrade-chain edges (see {@link FactoryFamilyResolver}).
 */
public final class TierRank {

    private TierRank() {
    }

    /**
     * Ranks tiers ascending. {@link BaseTier#CREATIVE} is always ranked last regardless of its enum
     * ordinal, since it conceptually sits above every power/throughput tier - including any
     * third-party tiers appended after it via {@code IExtensibleEnum} (e.g. Overclocked/Quantum/Dense/
     * Multiversal), whose ordinals would otherwise place them after Creative.
     */
    public static int of(BaseTier tier) {
        return tier == BaseTier.CREATIVE ? Integer.MAX_VALUE : tier.ordinal();
    }
}
