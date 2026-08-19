package wtf.vd.mekfactoryinfo.forge.compat.ae2;

import appeng.api.config.SortDir;
import appeng.api.config.SortOrder;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import java.util.Comparator;
import org.jetbrains.annotations.Nullable;

/**
 * Builds the composed comparator used by the AE2 terminal Lines-grouping sort toggle.
 */
public final class FactoryLinesGroupingComparators {

    private FactoryLinesGroupingComparators() {
    }

    public static Comparator<AEKey> wrap(Comparator<AEKey> base, SortOrder sortOrder, SortDir sortDir) {
        Comparator<AEKey> grouped = Comparator
                .comparing((AEKey key) -> groupSortKey(key, sortOrder), String.CASE_INSENSITIVE_ORDER)
                .thenComparingInt(FactoryLinesGroupingComparators::rankOf)
                .thenComparing(base);
        return sortDir == SortDir.DESCENDING ? grouped.reversed() : grouped;
    }

    private static String groupSortKey(AEKey key, SortOrder sortOrder) {
        SortFamily family = resolveFamily(key);
        if (family == null) {
            String name = key.getDisplayName().getString();
            return sortOrder == SortOrder.MOD ? key.getModId() + '\u0000' + name : name;
        }
        return sortOrder == SortOrder.MOD
                ? family.sortModId() + '\u0000' + family.sortName()
                : family.sortName();
    }

    private static int rankOf(AEKey key) {
        SortFamily family = resolveFamily(key);
        return family == null ? 0 : family.rank();
    }

    @Nullable
    private static SortFamily resolveFamily(AEKey key) {
        SortFamily family = FactoryFamilyResolver.resolve(key);
        if (family != null) {
            return family;
        }
        if (key instanceof AEItemKey itemKey) {
            return TieredItemFamilyResolver.resolve(itemKey.getItem());
        }
        return null;
    }
}
