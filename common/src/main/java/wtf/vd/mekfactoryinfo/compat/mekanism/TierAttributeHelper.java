package wtf.vd.mekfactoryinfo.compat.mekanism;

import mekanism.api.tier.ITier;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.attribute.AttributeTier;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * Safe replacement for {@link Attribute#getTier}, which does an unchecked {@code tierClass.cast(...)}
 * on whatever {@link ITier} the block's {@link AttributeTier} actually holds. Calling it with the
 * "wrong" tier class (e.g. asking a {@code CableTier}-tiered block for its {@code FactoryTier}) throws
 * a {@link ClassCastException} instead of returning {@code null} — this bit us in production: a
 * broad {@code ItemTooltipEvent} handler crashed the game the moment it hovered a Universal Cable
 * item while checking for {@code FactoryTier}. These helpers check {@code instanceof} first so
 * mismatched tier types simply resolve to {@code null}, matching the "not applicable" contract every
 * caller here actually expects.
 */
public final class TierAttributeHelper {

    private TierAttributeHelper() {
    }

    @Nullable
    public static <TIER extends ITier> TIER getTierSafely(Holder<Block> block, Class<TIER> tierClass) {
        return getTierSafely(block.value(), tierClass);
    }

    @Nullable
    public static <TIER extends ITier> TIER getTierSafely(Block block, Class<TIER> tierClass) {
        AttributeTier<?> attr = Attribute.get(block, AttributeTier.class);
        if (attr == null) {
            return null;
        }
        ITier tier = attr.tier();
        return tierClass.isInstance(tier) ? tierClass.cast(tier) : null;
    }
}
