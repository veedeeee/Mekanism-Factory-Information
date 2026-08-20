package wtf.vd.mekfactoryinfo.compat.mekanism;

import java.util.Arrays;
import java.util.Comparator;
import mekanism.api.tier.BaseTier;

/**
 * Shared ordinal-ranking helper for {@link BaseTier}.
 * <p>
 * {@link BaseTier#CREATIVE} is declared last among Mekanism's own constants, but addon mods that
 * extend the enum via {@code IExtensibleEnum} (e.g. Evolved Mekanism's Overclocked/Quantum/Dense/
 * Multiversal tiers) always append their new constants <em>after</em> it, even though those tiers
 * conceptually sit <em>below</em> Creative in the progression (Ultimate &rarr; Overclocked &rarr;
 * ... &rarr; Multiversal &rarr; Creative). Left unranked, {@code CREATIVE.ordinal()} sits in the
 * middle of that chain and breaks any "+1 == next tier" adjacency check. Ranking Creative as
 * {@link Integer#MAX_VALUE} restores the intended order without any addon-specific knowledge.
 */
public final class TierRank {

    private TierRank() {
    }

    public static int of(BaseTier tier) {
        return tier == BaseTier.CREATIVE ? Integer.MAX_VALUE : tier.ordinal();
    }

    /**
     * Returns whether {@code candidate} is the tier immediately above {@code current} in the actual
     * progression order (i.e. by {@link #of(BaseTier)} rank, not raw ordinal), mirroring what
     * {@code IUpgradeableTransmitter#canUpgrade} intends. This is what lets an Alloy upgrade a
     * transmitter tier by exactly one step even when addon mods have inserted extra tiers between
     * Mekanism's own constants and {@link BaseTier#CREATIVE}.
     */
    public static boolean isImmediatelyAbove(BaseTier candidate, BaseTier current) {
        BaseTier[] sorted = BaseTier.values().clone();
        Arrays.sort(sorted, Comparator.comparingInt(TierRank::of));
        for (int i = 0; i < sorted.length - 1; i++) {
            if (sorted[i] == current) {
                return sorted[i + 1] == candidate;
            }
        }
        return false;
    }
}
