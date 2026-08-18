package wtf.vd.mekfactoryinfo.neoforge.compat.ae2;

import appeng.api.config.SortDir;
import appeng.api.config.SortOrder;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import java.util.Comparator;
import net.minecraft.world.item.BlockItem;
import wtf.vd.mekfactoryinfo.compat.mekanism.FactoryLinesHelper;

/**
 * Builds the composed comparator used by the AE2 terminal Lines-grouping sort toggle: clusters a
 * Mekanism machine family's tiers together (ordered ascending by Lines count within the group),
 * positioned at the family's base machine's identity, layered on top of AE2's own NAME/MOD key
 * comparator as the final tiebreaker.
 */
public final class FactoryLinesGroupingComparators {

    private FactoryLinesGroupingComparators() {
    }

    /**
     * Wraps {@code base} (AE2's own NAME or MOD key comparator) with family grouping. Only valid for
     * {@link SortOrder#NAME} and {@link SortOrder#MOD}; {@link SortOrder#AMOUNT} never reaches this
     * (handled earlier by {@code Repo.getComparator} before the key-level comparator is consulted).
     */
    public static Comparator<AEKey> wrap(Comparator<AEKey> base, SortOrder sortOrder, SortDir sortDir) {
        Comparator<AEKey> grouped = Comparator
                .comparing((AEKey key) -> groupSortKey(key, sortOrder), String.CASE_INSENSITIVE_ORDER)
                .thenComparingInt(FactoryLinesGroupingComparators::linesOf)
                .thenComparing(base);
        // Reversing the whole composed comparator keeps the group order and the within-group Lines
        // order consistent with each other for descending sorts, rather than only flipping one of them.
        return sortDir == SortDir.DESCENDING ? grouped.reversed() : grouped;
    }

    private static String groupSortKey(AEKey key, SortOrder sortOrder) {
        FactoryFamily family = FactoryFamilyResolver.resolve(key);
        if (family == null) {
            String name = key.getDisplayName().getString();
            return sortOrder == SortOrder.MOD ? key.getModId() + '\u0000' + name : name;
        }
        return sortOrder == SortOrder.MOD
                ? family.sortModId() + '\u0000' + family.sortName()
                : family.sortName();
    }

    private static int linesOf(AEKey key) {
        if (!(key instanceof AEItemKey itemKey) || !(itemKey.getItem() instanceof BlockItem blockItem)) {
            return 0;
        }
        Integer lines = FactoryLinesHelper.getLinesForBlock(blockItem.getBlock());
        return lines == null ? 0 : lines;
    }
}

