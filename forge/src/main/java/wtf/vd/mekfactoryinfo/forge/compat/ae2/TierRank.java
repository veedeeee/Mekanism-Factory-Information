package wtf.vd.mekfactoryinfo.forge.compat.ae2;

import mekanism.api.tier.BaseTier;

/**
 * Shared ordinal-ranking helper for {@link BaseTier}.
 */
public final class TierRank {

    private TierRank() {
    }

    public static int of(BaseTier tier) {
        return tier == BaseTier.CREATIVE ? Integer.MAX_VALUE : tier.ordinal();
    }
}
