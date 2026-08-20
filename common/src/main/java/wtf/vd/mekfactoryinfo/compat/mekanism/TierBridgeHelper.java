package wtf.vd.mekfactoryinfo.compat.mekanism;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.attribute.AttributeTier;
import mekanism.common.block.interfaces.ITypeBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * Bridges an arbitrary Mekanism-style "tier" object -- {@link mekanism.api.tier.BaseTier}, or any
 * addon's own tier enum that isn't a {@code BaseTier} at all (e.g. MekanismExtras' {@code
 * AdvancedTier}, EvolvedMekanismExtras' {@code EMExtraTier}) -- to a cross-mod-comparable rank.
 * <p>
 * Every mod in this ecosystem already assigns each of its tiers a processing Lines count on its
 * Factory blocks (see {@link FactoryLinesHelper}), following the same structural convention: a
 * {@code FactoryTier}-shaped enum with a public {@code int processes} field and a {@code get*Tier()}
 * accessor pointing back at the "real" tier object (e.g. {@code FactoryTier#getBaseTier()},
 * MekanismExtras' {@code ExtraFactoryTier#getAdvanceTier()}). Reusing that already-canonical Lines
 * count as the rank for non-block items that reference the same tier object -- a Tier Installer's
 * {@code toTier}, an Alloy's {@code getTier()} -- lets them sort consistently with the Factory
 * blocks of the exact same tier without any addon-specific mapping table.
 */
public final class TierBridgeHelper {

    private static Map<Object, Integer> linesByTierObject;

    private TierBridgeHelper() {
    }

    /**
     * Returns the processing Lines count associated with {@code tierObj} (directly, or via one level
     * of {@code get*Tier()} unwrapping in either direction), or {@code null} if no Factory block
     * anywhere in the registry is built on that tier.
     */
    @Nullable
    public static Integer rankOf(@Nullable Object tierObj) {
        if (tierObj == null) {
            return null;
        }
        Map<Object, Integer> index = index();
        Integer direct = index.get(tierObj);
        if (direct != null) {
            return direct;
        }
        Object unwrapped = unwrapOneLevel(tierObj);
        return unwrapped == null ? null : index.get(unwrapped);
    }

    /**
     * Returns the tier object carried by {@code block}'s own tiered-storage/transmitter Attribute --
     * Mekanism's own {@code AttributeTier<?>#tier()}, or an addon's custom Attribute exposing the
     * same {@code tier()} accessor convention (see {@link #tierOf(Attribute)}) -- or {@code null} if
     * the block carries no such Attribute at all. Intended for callers that need to rank tiered
     * storage blocks (Tank, Energy Cube) which have no {@code processes} count of their own to index
     * by, so cannot use {@link #rankOf} directly without first obtaining this tier object.
     */
    @Nullable
    public static Object anyTierOf(Block block) {
        if (!(block instanceof ITypeBlock typeBlock)) {
            return null;
        }
        for (Attribute attr : typeBlock.getType().getAll()) {
            Object tierObj = tierOf(attr);
            if (tierObj != null) {
                return tierObj;
            }
        }
        return null;
    }

    private static Map<Object, Integer> index() {
        if (linesByTierObject == null) {
            Map<Object, Integer> map = new HashMap<>();
            for (Block block : BuiltInRegistries.BLOCK) {
                if (!(block instanceof ITypeBlock typeBlock)) {
                    continue;
                }
                for (Attribute attr : typeBlock.getType().getAll()) {
                    Object tierObj = tierOf(attr);
                    if (tierObj == null) {
                        continue;
                    }
                    Integer processes = processesOf(tierObj);
                    if (processes == null) {
                        continue;
                    }
                    map.putIfAbsent(tierObj, processes);
                    Object unwrapped = unwrapOneLevel(tierObj);
                    if (unwrapped != null) {
                        map.putIfAbsent(unwrapped, processes);
                    }
                }
            }
            linesByTierObject = map;
        }
        return linesByTierObject;
    }

    /**
     * Reads the tier object off a Factory-shaped {@link Attribute}, whether it's Mekanism's own
     * {@code AttributeTier<FactoryTier>} or an addon's custom record/class exposing the same {@code
     * tier()} accessor convention (see {@link FactoryLinesHelper#getProcessesFromAttribute}).
     */
    @Nullable
    private static Object tierOf(Attribute attr) {
        if (attr instanceof AttributeTier<?> attributeTier) {
            return attributeTier.tier();
        }
        try {
            Method tierMethod = attr.getClass().getMethod("tier");
            return tierMethod.invoke(attr);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    @Nullable
    private static Integer processesOf(Object tierObj) {
        try {
            return (Integer) tierObj.getClass().getField("processes").get(tierObj);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    /**
     * Calls the first no-arg {@code get*Tier()} method on {@code tierObj} that returns a different
     * object (e.g. {@code FactoryTier#getBaseTier()} -&gt; {@code BaseTier}, {@code
     * ExtraFactoryTier#getAdvanceTier()} -&gt; {@code AdvancedTier}, {@code AlloyTier#getBaseTier()} -&gt;
     * {@code BaseTier}), or {@code null} if there is none. Package-visible so {@link FactoryLinesHelper}
     * can reuse the same one-level unwrap when driving addon Tier Installer preview upgrades.
     */
    @Nullable
    static Object unwrapOneLevel(Object tierObj) {
        for (Method method : tierObj.getClass().getMethods()) {
            String name = method.getName();
            if (method.getParameterCount() != 0 || !name.startsWith("get") || !name.toLowerCase(Locale.ROOT).endsWith("tier")) {
                continue;
            }
            try {
                Object result = method.invoke(tierObj);
                if (result != null && result != tierObj) {
                    return result;
                }
            } catch (ReflectiveOperationException ignored) {
                // Try the next candidate accessor.
            }
        }
        return null;
    }
}
