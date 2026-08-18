package wtf.vd.mekfactoryinfo.neoforge.compat.ae2;

import mekanism.api.tier.BaseTier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves the family (if any) for items whose registry name is simply {@code "<tier>_<suffix>"}
 * (e.g. {@code basic_tier_installer}/{@code advanced_tier_installer}/..., or
 * {@code basic_control_circuit}/...) with no non-tiered base identity to anchor on, unlike Factory
 * machines which have {@link mekanism.common.content.blocktype.FactoryType#getBaseBlock()} (see
 * {@link FactoryFamilyResolver}).
 * <p>
 * Registry names (unlike translated display names) are stable and locale-independent, so this
 * derives the family purely from the item's own {@code ResourceLocation}: it strips a leading
 * {@code "<tier>_"} matching one of {@link BaseTier}'s values and, if at least one sibling tier
 * variant of the same suffix is also registered (confirming this is a genuine tiered family and not
 * a coincidental name), anchors the family's sort position on the lowest registered tier variant -
 * e.g. "Basic Tier Installer"/"Advanced Tier Installer"/... all sort together at "Tier Installer"'s
 * alphabetical position rather than being scattered under "B"/"A"/"E"/"U".
 */
public final class TieredItemFamilyResolver {

    private TieredItemFamilyResolver() {
    }

    @Nullable
    public static SortFamily resolve(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) {
            return null;
        }
        for (BaseTier tier : BaseTier.values()) {
            String prefix = tier.getLowerName() + "_";
            if (!id.getPath().startsWith(prefix)) {
                continue;
            }
            String suffix = id.getPath().substring(prefix.length());
            Item lowestSibling = null;
            BaseTier lowestTier = null;
            boolean hasSibling = false;
            for (BaseTier candidateTier : BaseTier.values()) {
                ResourceLocation candidateId = ResourceLocation.fromNamespaceAndPath(id.getNamespace(),
                        candidateTier.getLowerName() + "_" + suffix);
                Item candidate = BuiltInRegistries.ITEM.getOptional(candidateId).orElse(null);
                if (candidate == null) {
                    continue;
                }
                if (candidateTier != tier) {
                    hasSibling = true;
                }
                if (lowestTier == null || candidateTier.ordinal() < lowestTier.ordinal()) {
                    lowestTier = candidateTier;
                    lowestSibling = candidate;
                }
            }
            if (!hasSibling || lowestSibling == null) {
                // Coincidental "<tier>_" prefix with no other tier variants registered; not a family.
                return null;
            }
            String name = lowestSibling.getDescription().getString();
            return new SortFamily(name, id.getNamespace(), tier.ordinal());
        }
        return null;
    }
}
