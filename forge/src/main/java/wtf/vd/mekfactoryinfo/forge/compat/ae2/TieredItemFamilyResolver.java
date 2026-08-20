package wtf.vd.mekfactoryinfo.forge.compat.ae2;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
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
 * Resolves the family for items whose registry name is {@code "<tier>_<suffix>"}.
 */
public final class TieredItemFamilyResolver {

    private record TierEntry(BaseTier tier, Item item) {
    }

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
                return null;
            }
            TierEntry lowest = siblings.stream().min(Comparator.comparingInt(e -> TierRank.of(e.tier()))).orElseThrow();
            ResourceLocation lowestId = BuiltInRegistries.ITEM.getKey(lowest.item());
            String name = toTitleCase(suffix);
            String modId = lowestId == null ? id.getNamespace() : lowestId.getNamespace();
            return new SortFamily(name, modId, TierRank.of(tier));
        }
        return null;
    }

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
