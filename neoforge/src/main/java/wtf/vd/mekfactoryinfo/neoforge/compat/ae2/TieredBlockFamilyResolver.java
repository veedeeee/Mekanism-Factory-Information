package wtf.vd.mekfactoryinfo.neoforge.compat.ae2;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mekanism.api.tier.BaseTier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import wtf.vd.mekfactoryinfo.compat.mekanism.TierBridgeHelper;
import wtf.vd.mekfactoryinfo.compat.mekanism.TierRank;

/**
 * Resolves the family (if any) for tiered storage blocks following Mekanism's own registry-name
 * convention: {@code "<tier>_energy_cube"}, {@code "<tier>_fluid_tank"}, {@code "<tier>_chemical_tank"}.
 * Unlike Factory machines (see {@link FactoryFamilyResolver}), these blocks have no
 * {@code FactoryType}-shaped attribute (they aren't recipe-processing machines) and no
 * {@code processes} count to key a Lines-based rank off of, so the family is derived purely from the
 * registry name, the same way {@link TieredItemFamilyResolver} handles Control Circuit/Tier
 * Installer/Alloy items.
 * <p>
 * Addon mods (MekanismExtras' Absolute/Supreme/Cosmic/Infinite Energy Cube/Fluid Tank/Chemical Tank,
 * Evolved Mekanism's Overclocked/Quantum/Dense/Multiversal/Creative variants) reuse these exact same
 * suffixes for their own tiers, so they join the same family automatically; only {@link #SUFFIXES}
 * needs extending if Mekanism itself ever introduces another tiered storage block type. Each
 * sibling's rank within the family is resolved, in order of preference:
 * <ol>
 *   <li>A {@link BaseTier#values()} lower-name match -- the common, fast path covering Mekanism's own
 *   tiers and any addon that extends {@code BaseTier} itself.</li>
 *   <li>Otherwise, {@link TierBridgeHelper#rankOf} on the block's own tier object (read via
 *   {@link TierBridgeHelper#anyTierOf}): addons that define their own tier enum for these blocks
 *   (e.g. MekanismExtras' {@code ECTier}) still wrap the same shared tier object
 *   (e.g. {@code AdvancedTier.ABSOLUTE}) their Tier Installer/Control Circuit/Alloy items use, which
 *   is already resolvable to a rank via the Factory blocks built on that same tier.</li>
 * </ol>
 * If neither resolves, the block still joins the family but sorts last within it, rather than being
 * excluded from grouping altogether.
 */
public final class TieredBlockFamilyResolver {

    /**
     * Known Mekanism tiered-storage-block registry-name suffixes. Extend this list if Mekanism
     * itself introduces another such block type; addon mods automatically follow suit by reusing the
     * same suffix for their own tiers, so no addon-specific entry is ever needed here.
     */
    private static final String[] SUFFIXES = {"energy_cube", "fluid_tank", "chemical_tank"};

    private record TierEntry(String tierName, Block block) {
    }

    /**
     * Lazily built, cached index from suffix (e.g. {@code "energy_cube"}) to every registered tier
     * variant of that suffix across all namespaces. Built once on first use since the block registry
     * is frozen well before any AE2 terminal screen can open.
     */
    private static Map<String, List<TierEntry>> familiesBySuffix;

    /** Memoized {@link #computeRank}, since resolving a rank can involve several reflective calls. */
    private static Map<Block, Integer> rankCache;

    private TieredBlockFamilyResolver() {
    }

    @Nullable
    public static SortFamily resolve(Block block, ResourceLocation blockId) {
        String[] parsed = parse(blockId.getPath());
        if (parsed == null) {
            return null;
        }
        String suffix = parsed[0];
        String tierName = parsed[1];
        List<TierEntry> siblings = index().get(suffix);
        if (siblings == null || siblings.size() < 2) {
            // Coincidental match with no other tier variants registered; not a family.
            return null;
        }
        TierEntry lowest = siblings.stream().min(Comparator.comparingInt(e -> rankOf(e.block(), e.tierName()))).orElseThrow();
        ResourceLocation lowestId = BuiltInRegistries.BLOCK.getKey(lowest.block());
        // Every block here IS tier-affixed by definition (that's how the family was found), so using
        // the lowest tier's own translated display name (e.g. "Basic Energy Cube") as the sort key
        // would anchor the whole family under "B" instead of where its actual identity ("Energy
        // Cube") alphabetically belongs. The registry suffix itself is locale-independent and
        // untiered, so it's used to build a tier-agnostic sort name instead.
        String name = toTitleCase(suffix);
        String modId = lowestId == null ? blockId.getNamespace() : lowestId.getNamespace();
        return new SortFamily(name, modId, rankOf(block, tierName));
    }

    /**
     * Splits a registry path into {@code {suffix, tierName}} if it matches one of {@link #SUFFIXES},
     * or returns {@code null} otherwise.
     */
    @Nullable
    private static String[] parse(String path) {
        for (String suffix : SUFFIXES) {
            String marker = "_" + suffix;
            if (path.length() > marker.length() && path.endsWith(marker)) {
                return new String[] {suffix, path.substring(0, path.length() - marker.length())};
            }
        }
        return null;
    }

    private static int rankOf(Block block, String tierName) {
        return rankCache().computeIfAbsent(block, b -> computeRank(b, tierName));
    }

    private static int computeRank(Block block, String tierName) {
        for (BaseTier tier : BaseTier.values()) {
            if (tier.getLowerName().equals(tierName)) {
                return TierRank.of(tier);
            }
        }
        Object tierObj = TierBridgeHelper.anyTierOf(block);
        if (tierObj != null) {
            Integer bridged = TierBridgeHelper.rankOf(tierObj);
            if (bridged != null) {
                return bridged;
            }
        }
        // No Factory block anywhere is built on this tier (or the block's tier object couldn't be
        // determined at all); keep the block in the family, but sort it last rather than guessing.
        return Integer.MAX_VALUE;
    }

    /** Converts a snake_case registry path suffix (e.g. {@code "energy_cube"}) to {@code "Energy Cube"}. */
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
            for (Block block : BuiltInRegistries.BLOCK) {
                ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
                if (id == null) {
                    continue;
                }
                String[] parsed = parse(id.getPath());
                if (parsed == null) {
                    continue;
                }
                map.computeIfAbsent(parsed[0], s -> new ArrayList<>()).add(new TierEntry(parsed[1], block));
            }
            familiesBySuffix = map;
        }
        return familiesBySuffix;
    }

    private static Map<Block, Integer> rankCache() {
        if (rankCache == null) {
            rankCache = new HashMap<>();
        }
        return rankCache;
    }
}

