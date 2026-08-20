package wtf.vd.mekfactoryinfo.neoforge.compat.ae2;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import java.lang.reflect.Method;
import mekanism.api.providers.IBlockProvider;
import mekanism.api.text.IHasTranslationKey;
import mekanism.common.block.attribute.Attribute;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import wtf.vd.mekfactoryinfo.compat.mekanism.FactoryLinesHelper;

/**
 * Resolves the Mekanism machine family (if any) that an AE2 key belongs to, based on the recipe
 * type a Factory (or its single-machine counterpart) processes, e.g. Smelting, Crushing,
 * Enriching. Every mod in this ecosystem exposes that concept via a small, structurally identical
 * pattern: a Mekanism {@link Attribute} on the block whose accessor returns some {@code *FactoryType}
 * enum implementing {@link IHasTranslationKey} with a {@code getBaseBlock()} method pointing back
 * at the "root" (Basic-tier) block of that family. Mekanism itself uses
 * {@code AttributeFactoryType}/{@code FactoryType} for this; addons that don't literally reuse
 * those base classes (e.g. EvolvedMekanismExtras' {@code EMExtraAttributeFactoryType}/
 * {@code EMExtraFactoryType}) still follow the exact same shape because they mirror Mekanism's own
 * convention. Detecting the pattern structurally (by method shape, not by hardcoding any of these
 * class names) means every mod's variant of e.g. the Smelting line -- with its own,
 * otherwise-unconnected tier ladder -- lands in one shared sort family, ordered purely by its
 * processing Lines count (ascending). No addon-specific handling is required; any future addon
 * following the same convention is picked up automatically.
 *
 * <p>Addons with no such attribute at all (e.g. Astral Mekanism, which only tags its machines with
 * a plain tier attribute and never exposes a FactoryType-shaped concept) cannot be grouped by this
 * mechanism, since there is no shared, reflectable signal to key off -- those items simply fall
 * back to the terminal's normal name/mod sort (or to {@link AstralMekanismFamilyResolver}'s
 * explicit carve-out).
 */
public final class FactoryFamilyResolver {

    private FactoryFamilyResolver() {
    }

    @Nullable
    public static SortFamily resolve(AEKey key) {
        Block block = blockOf(key);
        if (block == null) {
            return null;
        }
        // Unlike Forge's Mekanism API (whose Attribute.getAll accepts a plain Block), NeoForge's
        // Mekanism API only exposes Attribute.getAll(Holder<Block>); the vanilla built-in registry
        // holder is the loader-agnostic way to get one for any registered block.
        for (Attribute attr : Attribute.getAll(block.builtInRegistryHolder())) {
            SortFamily family = tryResolveFromAttribute(block, attr);
            if (family != null) {
                return family;
            }
        }
        return null;
    }

    @Nullable
    private static SortFamily tryResolveFromAttribute(Block block, Attribute attr) {
        for (Method method : attr.getClass().getMethods()) {
            if (method.getParameterCount() != 0 || method.getReturnType() == void.class) {
                continue;
            }
            Class<?> returnType = method.getReturnType();
            if (!returnType.getSimpleName().endsWith("FactoryType") || !IHasTranslationKey.class.isAssignableFrom(returnType)) {
                continue;
            }
            Object factoryType;
            try {
                factoryType = method.invoke(attr);
            } catch (ReflectiveOperationException e) {
                continue;
            }
            if (factoryType == null) {
                continue;
            }
            Block baseBlock = extractBaseBlock(factoryType);
            if (baseBlock == null) {
                continue;
            }
            Integer lines = FactoryLinesHelper.getLinesForBlock(block);
            if (lines == null) {
                continue;
            }
            String name = Component.translatable(((IHasTranslationKey) factoryType).getTranslationKey()).getString();
            ResourceLocation baseBlockId = BuiltInRegistries.BLOCK.getKey(baseBlock);
            return new SortFamily(name, baseBlockId.getNamespace(), lines);
        }
        return null;
    }

    /**
     * Reads the {@code getBaseBlock()} accessor every {@code *FactoryType} enum in this ecosystem
     * exposes, and unwraps its result down to a concrete {@link Block}. Forge's Mekanism API returns
     * an {@link IBlockProvider} there; NeoForge's returns a {@code BlockRegistryObject} (a
     * {@code DeferredHolder} wrapper) instead, so both {@code getBlock()} (Forge) and
     * {@code get()}/{@code value()} (NeoForge's {@code DeferredHolder}) accessors are tried via
     * reflection rather than hardcoding either loader's wrapper type. A plain {@link Block} result is
     * also accepted directly. Returns {@code null} on any shape mismatch.
     */
    @Nullable
    private static Block extractBaseBlock(Object factoryType) {
        Object result;
        try {
            Method getBaseBlock = factoryType.getClass().getMethod("getBaseBlock");
            result = getBaseBlock.invoke(factoryType);
        } catch (ReflectiveOperationException e) {
            return null;
        }
        if (result instanceof Block block) {
            return block;
        }
        if (result instanceof IBlockProvider provider) {
            return provider.getBlock();
        }
        for (String accessor : new String[] {"getBlock", "get", "value"}) {
            try {
                Method unwrap = result.getClass().getMethod(accessor);
                Object unwrapped = unwrap.invoke(result);
                if (unwrapped instanceof Block block) {
                    return block;
                }
            } catch (ReflectiveOperationException e) {
                // try the next accessor name
            }
        }
        return null;
    }

    @Nullable
    static Block blockOf(AEKey key) {
        if (!(key instanceof AEItemKey itemKey)) {
            return null;
        }
        if (!(itemKey.getItem() instanceof BlockItem blockItem)) {
            return null;
        }
        return blockItem.getBlock();
    }
}
