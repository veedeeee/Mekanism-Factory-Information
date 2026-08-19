package wtf.vd.mekfactoryinfo.neoforge.compat.ae2;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * derives the family purely from the item's own {@code ResourceLocation} path, ignoring namespace:
 * addon mods commonly register their own extra tiers (e.g. Overclocked/Quantum/Dense/Multiversal)
 * under a different mod id than Mekanism's own Basic/Advanced/Elite/Ultimate items, so the sibling
 * lookup below scans the whole item registry rather than assuming a single shared namespace. If at
 * least one sibling tier variant of the same suffix is found (confirming this is a genuine tiered
 * family and not a coincidental name), the family anchors its sort position on the lowest registered
 * tier variant - e.g. "Basic Tier Installer"/"Advanced Tier Installer"/... all sort together at "Tier
 * Installer"'s alphabetical position rather than being scattered under "B"/"A"/"E"/"U".
 */
public final class TieredItemFamilyResolver {

    private record TierEntry(BaseTier tier, Item item) {
    }

    /**
     * Lazily built, cached index from suffix (e.g. {@code "tier_installer"}) to every registered tier
     * variant of that suffix across all namespaces. Built once on first use since the item registry is
     * frozen well before any AE2 terminal screen can open.
     */
    private static Map<String, List<TierEntry>> familiesBySuffix;

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
            List<TierEntry> siblings = index().get(suffix);
            if (siblings == null || siblings.size() < 2) {
                // Coincidental "<tier>_" prefix with no other tier variants registered; not a family.
                return null;
            }
            TierEntry lowest = siblings.stream().min(Comparator.comparingInt(e -> TierRank.of(e.tier()))).orElseThrow();
            ResourceLocation lowestId = BuiltInRegistries.ITEM.getKey(lowest.item());
            // Unlike FactoryFamilyResolver's root machine (whose own display name has no tier prefix
            // at all, e.g. "Chemical Infuser"), every item here IS tier-prefixed by definition (that's
            // how the family was found), so using the lowest tier's own translated display name
            // (e.g. "Basic Control Circuit") as the sort key would anchor the whole family under "B"
            // instead of where the item's actual identity ("Control Circuit") alphabetically belongs.
            // The registry path suffix (e.g. "control_circuit") is locale-independent and untiered, so
            // it's used to build a tier-agnostic sort name instead.
            String name = toTitleCase(suffix);
            String modId = lowestId == null ? id.getNamespace() : lowestId.getNamespace();
            return new SortFamily(name, modId, TierRank.of(tier));
        }
        return null;
    }

    /** Converts a snake_case registry path suffix (e.g. {@code "control_circuit"}) to {@code "Control Circuit"}. */
    private static String toTitleCase(String suffix) {
        StringBuilder result = new StringBuilder(suffix.length());
        for (String word : suffix.split("_")) {
            if (word.isEmpty()) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }

    private static Map<String, List<TierEntry>> index() {
        if (familiesBySuffix == null) {
            Map<String, List<TierEntry>> map = new HashMap<>();
            for (Item item : BuiltInRegistries.ITEM) {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                if (id == null) {
                    continue;
                }
                for (BaseTier tier : BaseTier.values()) {
                    String prefix = tier.getLowerName() + "_";
                    if (id.getPath().startsWith(prefix)) {
                        String suffix = id.getPath().substring(prefix.length());
                        map.computeIfAbsent(suffix, s -> new ArrayList<>()).add(new TierEntry(tier, item));
                        break;
                    }
                }
            }
            familiesBySuffix = map;
        }
        return familiesBySuffix;
    }
}
