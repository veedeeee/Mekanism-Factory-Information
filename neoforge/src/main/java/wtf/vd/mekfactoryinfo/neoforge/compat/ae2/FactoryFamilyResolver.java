package wtf.vd.mekfactoryinfo.neoforge.compat.ae2;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import mekanism.api.tier.BaseTier;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.attribute.AttributeUpgradeable;
import mekanism.common.block.interfaces.ITypeBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import wtf.vd.mekfactoryinfo.compat.mekanism.FactoryLinesHelper;

/**
 * Resolves the Mekanism machine family (if any) that an AE2 key belongs to, by walking the block's
 * Tier Installer upgrade chain ({@link AttributeUpgradeable}) rather than keying off Mekanism's own
 * {@code FactoryType}/{@code AttributeFactoryType}. Every regular machine and Factory tier that the
 * Tier Installer can upgrade between - Mekanism's own (e.g. Chemical Oxidizer -> Basic/Advanced/Elite/
 * Ultimate Oxidizing Factory) or a third-party addon's - carries {@link AttributeUpgradeable} pointing
 * forward to the next tier's block (see {@code Machine}/{@code Factory}/{@code MekanismBlockTypes}),
 * since that wiring is what {@code ItemTierInstaller#useOn} itself relies on to function at all. This
 * makes the family purely a property of that chain, so it groups correctly even for addon Factory
 * types that don't reuse (or extend) Mekanism's own {@code FactoryType} enum at all - e.g. an addon's
 * "Oxidizing Factory" family, which has no {@code FactoryType.OXIDIZING} to key off of.
 */
public final class FactoryFamilyResolver {

    private FactoryFamilyResolver() {
    }

    /** block -> the next tier's block it upgrades into, per {@link AttributeUpgradeable}. */
    private static Map<Block, Block> forwardChain;
    /** block -> the previous tier's block that upgrades into it (the reverse of {@link #forwardChain}). */
    private static Map<Block, Block> reverseChain;

    /**
     * Returns the family identity for {@code key}, or {@code null} if its block is not part of any
     * Tier Installer upgrade chain.
     */
    @Nullable
    public static SortFamily resolve(AEKey key) {
        Block block = blockOf(key);
        if (block == null) {
            return null;
        }
        buildChainIndexIfNeeded();
        if (!forwardChain.containsKey(block) && !reverseChain.containsKey(block)) {
            return null;
        }
        // Guard against a malformed (e.g. addon-induced) cycle in the upgrade graph, which would
        // otherwise hang the render thread in an infinite loop while trying to find a root/end.
        Set<Block> visited = new HashSet<>();
        Block root = block;
        while (reverseChain.containsKey(root) && visited.add(root)) {
            root = reverseChain.get(root);
        }
        List<Block> chain = new ArrayList<>();
        chain.add(root);
        visited.clear();
        Block cur = root;
        while (forwardChain.containsKey(cur) && visited.add(cur)) {
            cur = forwardChain.get(cur);
            chain.add(cur);
        }
        int rank = chain.indexOf(block);
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(root);
        String name = root.asItem().getDescription().getString();
        return new SortFamily(name, id.getNamespace(), rank);
    }

    private static void buildChainIndexIfNeeded() {
        if (forwardChain != null) {
            return;
        }
        Map<Block, Block> forward = new HashMap<>();
        Map<Block, Block> reverse = new HashMap<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            Block target;
            AttributeUpgradeable upgradeable = Attribute.get(block, AttributeUpgradeable.class);
            if (upgradeable != null) {
                // Standard Mekanism upgrade chain.
                try {
                    target = upgradeable.upgradeResult(block.defaultBlockState(), BaseTier.BASIC).getBlock();
                } catch (NullPointerException e) {
                    continue;
                }
            } else {
                // Addon mods (MekanismExtras, EvolvedMekanismExtras, etc.) use their own
                // attribute class with an upgradeBlock supplier.
                target = getAddonUpgradeTarget(block);
                if (target == null) {
                    continue;
                }
            }
            if (target == block) {
                continue;
            }
            // Guard against back-edges (e.g. stray AttributeUpgradeable pointing to Basic on the
            // highest tier's block) — a valid edge must move to a strictly higher rank.
            if (effectiveTierRank(target) <= effectiveTierRank(block)) {
                continue;
            }
            forward.put(block, target);
            reverse.put(target, block);
        }
        forwardChain = forward;
        reverseChain = reverse;
    }

    /**
     * Returns the tier rank used for upgrade-chain edge validation.
     * Standard Mekanism blocks use their {@link BaseTier} ordinal; addon blocks that don't expose a
     * standard {@link BaseTier} fall back to their {@code processes} count as a rank proxy.
     */
    private static int effectiveTierRank(Block block) {
        BaseTier tier = Attribute.getBaseTier(block.builtInRegistryHolder());
        if (tier != null) {
            return TierRank.of(tier);
        }
        Integer processes = FactoryLinesHelper.getLinesForBlock(block);
        return processes != null ? processes : -1;
    }

    /**
     * Attempts to find the upgrade-target block for addon mods that use a custom upgradeable
     * attribute (not Mekanism's {@link AttributeUpgradeable}). Looks for any attribute on the block
     * that exposes an {@code upgradeBlock} supplier — either as a record accessor (public method) or
     * as a field — then calls the supplier to obtain the target {@link Block}.
     */
    @Nullable
    private static Block getAddonUpgradeTarget(Block block) {
        if (!(block instanceof ITypeBlock typeBlock)) {
            return null;
        }
        for (var attr : typeBlock.getType().getAll()) {
            if (attr instanceof AttributeUpgradeable) {
                continue;
            }
            try {
                Supplier<?> supplier = getUpgradeBlockSupplier(attr);
                if (supplier == null) {
                    continue;
                }
                Object regObj = supplier.get();
                if (regObj == null) {
                    continue;
                }
                Method getBlock = regObj.getClass().getMethod("get");
                Object result = getBlock.invoke(regObj);
                if (result instanceof Block target) {
                    return target;
                }
            } catch (ReflectiveOperationException | ClassCastException e) {
                // not an upgradeable attribute
            }
        }
        return null;
    }

    /**
     * Retrieves the {@code upgradeBlock} supplier from a custom upgradeable attribute via reflection,
     * supporting both record-accessor style (public {@code upgradeBlock()} method) and field style
     * (private {@code upgradeBlock} field).
     */
    @Nullable
    @SuppressWarnings("unchecked")
    private static Supplier<?> getUpgradeBlockSupplier(Object attr) {
        try {
            Method m = attr.getClass().getMethod("upgradeBlock");
            return (Supplier<?>) m.invoke(attr);
        } catch (ReflectiveOperationException | ClassCastException e) {
            // not a public record accessor
        }
        try {
            Field f = attr.getClass().getDeclaredField("upgradeBlock");
            f.setAccessible(true);
            return (Supplier<?>) f.get(attr);
        } catch (ReflectiveOperationException | ClassCastException e) {
            // no upgradeBlock field
        }
        return null;
    }

    @Nullable
    private static Block blockOf(AEKey key) {
        if (!(key instanceof AEItemKey itemKey)) {
            return null;
        }
        if (!(itemKey.getItem() instanceof BlockItem blockItem)) {
            return null;
        }
        return blockItem.getBlock();
    }
}
