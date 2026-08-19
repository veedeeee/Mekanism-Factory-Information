package wtf.vd.mekfactoryinfo.neoforge.compat.ae2;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import mekanism.api.tier.BaseTier;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.attribute.AttributeUpgradeable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

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
            AttributeUpgradeable upgradeable = Attribute.get(block, AttributeUpgradeable.class);
            if (upgradeable == null) {
                continue;
            }
            // The tier argument is unused by every known AttributeUpgradeable implementation (it just
            // resolves its own fixed upgrade target block), so any BaseTier value works here. The
            // highest tier of a chain (e.g. Ultimate/Creative Energy Cube/Bin/Tank) is registered with
            // a literal null upgrade-target supplier rather than one that returns null, which NPEs
            // inside upgradeResult itself - that's expected here and simply means "no forward edge".
            Block target;
            try {
                target = upgradeable.upgradeResult(block.defaultBlockState(), BaseTier.BASIC).getBlock();
            } catch (NullPointerException e) {
                continue;
            }
            if (target == block) {
                continue;
            }
            // Guard against Mekanism's own leftover default AttributeUpgradeable: Machine.FactoryMachine
            // unconditionally sets "upgrade to Basic" for every Factory tier's blocktype before Factory's
            // constructor overwrites it with the real "upgrade to next tier" edge for every tier except
            // the last (see Factory.java: "tier.ordinal() < FACTORY_TIERS.length - 1") - so the highest
            // Factory tier (e.g. Ultimate) keeps a stray edge pointing BACK to Basic, which would
            // otherwise form a cycle (Basic -> Advanced -> Elite -> Ultimate -> Basic) and corrupt every
            // tier's family resolution. A valid upgrade edge must always move to a strictly higher tier
            // (or from an untiered base machine, rank -1, to the lowest tier), so any edge that doesn't
            // is rejected here rather than trusted blindly.
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
     * The block's own {@link BaseTier} rank (see {@link TierRank}), or {@code -1} if it has no
     * {@code AttributeTier} at all (e.g. a regular, non-Factory base machine).
     */
    private static int effectiveTierRank(Block block) {
        BaseTier tier = Attribute.getBaseTier(block.builtInRegistryHolder());
        return tier == null ? -1 : TierRank.of(tier);
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
