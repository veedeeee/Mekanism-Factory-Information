package wtf.vd.mekfactoryinfo.neoforge.compat.ae2;

import java.lang.reflect.Method;
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
import wtf.vd.mekfactoryinfo.compat.mekanism.TierBridgeHelper;

/**
 * Resolves the family (if any) for items following Mekanism's own tiered-item registry-name
 * conventions: {@code "<tier>_control_circuit"}, {@code "<tier>_tier_installer"}, and
 * {@code "alloy_<tier>"}. Unlike Factory machines (see {@link FactoryFamilyResolver}), these items
 * have no {@code FactoryType}-shaped attribute to key off, so the family is derived purely from the
 * registry name.
 * <p>
 * The {@code <tier>} segment is <em>not</em> required to be one of Mekanism's own {@link BaseTier}
 * constants: several addons in this ecosystem define their own, entirely separate tier enum for
 * these items (e.g. MekanismExtras' {@code AdvancedTier}, EvolvedMekanismExtras'
 * {@code EMExtraTier}) rather than extending {@code BaseTier} itself the way Evolved Mekanism's
 * Overclocked/Quantum/Dense/Multiversal does. Grouping is still generic: any two items sharing the
 * same suffix (e.g. {@code control_circuit}) are siblings in one family regardless of which tier
 * enum backs them. Each sibling's rank within the family is resolved, in order of preference:
 * <ol>
 *   <li>A {@link BaseTier#values()} lower-name match -- the common, fast path covering Mekanism's
 *   own tiers and any addon that extends {@code BaseTier} itself.</li>
 *   <li>Otherwise, {@link TierBridgeHelper#rankOf}, reflectively reading the item's own tier object
 *   (a Tier Installer's {@code getToTier()}, an Alloy's {@code getTier()}) and cross-referencing it
 *   against Factory blocks built on that same tier for their already-canonical processing Lines
 *   count -- the same physical grounding {@link FactoryFamilyResolver} uses, just reached from the
 *   item side instead of the block side. Plain Control Circuit items expose no tier object of their
 *   own, so this falls back to whichever sibling Tier Installer shares the same tier-name segment.</li>
 * </ol>
 * If neither resolves (no addon Factory block anywhere is built on that tier), the item still joins
 * the family but sorts last within it, rather than being excluded from grouping altogether.
 */
public final class TieredItemFamilyResolver {

    private static final String CIRCUIT_SUFFIX = "control_circuit";
    private static final String INSTALLER_SUFFIX = "tier_installer";
    private static final String ALLOY_SUFFIX = "alloy";
    private static final String ALLOY_PREFIX = "alloy_";

    private record TierEntry(String tierName, Item item) {
    }

    /**
     * Lazily built, cached index from suffix (e.g. {@code "tier_installer"}) to every registered tier
     * variant of that suffix across all namespaces. Built once on first use since the item registry is
     * frozen well before any AE2 terminal screen can open.
     */
    private static Map<String, List<TierEntry>> familiesBySuffix;

    /** Tier-name segment (e.g. {@code "absolute"}) to its Tier Installer item, built alongside {@link #familiesBySuffix}. */
    private static Map<String, Item> installersByTierName;

    /** Memoized {@link #computeRank}, since resolving a rank can involve several reflective calls. */
    private static Map<Item, Integer> rankCache;

    private TieredItemFamilyResolver() {
    }

    @Nullable
    public static SortFamily resolve(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) {
            return null;
        }
        String[] parsed = parse(id.getPath());
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
        TierEntry lowest = siblings.stream().min(Comparator.comparingInt(e -> rankOf(e.item(), e.tierName()))).orElseThrow();
        ResourceLocation lowestId = BuiltInRegistries.ITEM.getKey(lowest.item());
        // Unlike FactoryFamilyResolver's root machine (whose own display name has no tier prefix at
        // all, e.g. "Chemical Infuser"), every item here IS tier-affixed by definition (that's how the
        // family was found), so using the lowest tier's own translated display name (e.g.
        // "Basic Control Circuit") as the sort key would anchor the whole family under "B" instead of
        // where the item's actual identity ("Control Circuit") alphabetically belongs. The registry
        // suffix itself is locale-independent and untiered, so it's used to build a tier-agnostic sort
        // name instead.
        String name = toTitleCase(suffix);
        String modId = lowestId == null ? id.getNamespace() : lowestId.getNamespace();
        return new SortFamily(name, modId, rankOf(item, tierName));
    }

    /**
     * Splits a registry path into {@code {suffix, tierName}} if it matches one of the known
     * conventions, or returns {@code null} otherwise.
     */
    @Nullable
    private static String[] parse(String path) {
        if (path.startsWith(ALLOY_PREFIX)) {
            return new String[] {ALLOY_SUFFIX, path.substring(ALLOY_PREFIX.length())};
        }
        if (path.endsWith("_" + CIRCUIT_SUFFIX)) {
            return new String[] {CIRCUIT_SUFFIX, path.substring(0, path.length() - CIRCUIT_SUFFIX.length() - 1)};
        }
        if (path.endsWith("_" + INSTALLER_SUFFIX)) {
            return new String[] {INSTALLER_SUFFIX, path.substring(0, path.length() - INSTALLER_SUFFIX.length() - 1)};
        }
        return null;
    }

    private static int rankOf(Item item, String tierName) {
        return rankCache().computeIfAbsent(item, i -> computeRank(i, tierName));
    }

    private static int computeRank(Item item, String tierName) {
        for (BaseTier tier : BaseTier.values()) {
            if (tier.getLowerName().equals(tierName)) {
                return TierRank.of(tier);
            }
        }
        Object tierObj = extractTierObject(item, tierName);
        if (tierObj != null) {
            Integer bridged = TierBridgeHelper.rankOf(tierObj);
            if (bridged != null) {
                return bridged;
            }
        }
        // No Factory block anywhere is built on this tier (or the item's tier object couldn't be
        // determined at all); keep the item in the family, but sort it last rather than guessing.
        return Integer.MAX_VALUE;
    }

    /**
     * Reflectively reads {@code item}'s own tier object -- a Tier Installer's {@code getToTier()}, or
     * an Alloy's {@code getTier()} -- following the exact same accessor names used elsewhere in this
     * codebase (see {@code CableSpecHelper}, {@code FactoryLinesHelper}). Plain items with neither
     * accessor (e.g. Control Circuit, which carries no tier field of its own) borrow the tier from
     * the sibling Tier Installer that shares the same tier-name segment, if one is registered.
     */
    @Nullable
    private static Object extractTierObject(Item item, String tierName) {
        Object tier = tryInvoke(item, "getToTier");
        if (tier != null) {
            return tier;
        }
        tier = tryInvoke(item, "getTier");
        if (tier != null) {
            return tier;
        }
        Item installerSibling = installerIndex().get(tierName);
        if (installerSibling != null && installerSibling != item) {
            return tryInvoke(installerSibling, "getToTier");
        }
        return null;
    }

    @Nullable
    private static Object tryInvoke(Item item, String methodName) {
        try {
            Method method = item.getClass().getMethod(methodName);
            return method.invoke(item);
        } catch (ReflectiveOperationException e) {
            return null;
        }
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
            Map<String, Item> installers = new HashMap<>();
            for (Item item : BuiltInRegistries.ITEM) {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                if (id == null) {
                    continue;
                }
                String[] parsed = parse(id.getPath());
                if (parsed == null) {
                    continue;
                }
                String suffix = parsed[0];
                String tierName = parsed[1];
                map.computeIfAbsent(suffix, s -> new ArrayList<>()).add(new TierEntry(tierName, item));
                if (INSTALLER_SUFFIX.equals(suffix)) {
                    installers.put(tierName, item);
                }
            }
            familiesBySuffix = map;
            installersByTierName = installers;
        }
        return familiesBySuffix;
    }

    private static Map<String, Item> installerIndex() {
        index();
        return installersByTierName;
    }

    private static Map<Item, Integer> rankCache() {
        if (rankCache == null) {
            rankCache = new HashMap<>();
        }
        return rankCache;
    }
}

