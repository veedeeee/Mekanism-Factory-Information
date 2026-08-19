package wtf.vd.mekfactoryinfo.forge.compat.ae2;

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
import mekanism.common.block.attribute.AttributeTier;
import mekanism.common.block.attribute.AttributeUpgradeable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves the Mekanism machine family (if any) that an AE2 key belongs to, by walking the block's
 * Tier Installer upgrade chain ({@link AttributeUpgradeable}).
 */
public final class FactoryFamilyResolver {

    private FactoryFamilyResolver() {
    }

    private static Map<Block, Block> forwardChain;
    private static Map<Block, Block> reverseChain;

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
            Block target;
            try {
                target = upgradeable.upgradeResult(block.defaultBlockState(), BaseTier.BASIC).getBlock();
            } catch (NullPointerException e) {
                continue;
            }
            if (target == block) {
                continue;
            }
            if (effectiveTierRank(target) <= effectiveTierRank(block)) {
                continue;
            }
            forward.put(block, target);
            reverse.put(target, block);
        }
        forwardChain = forward;
        reverseChain = reverse;
    }

    private static int effectiveTierRank(Block block) {
        AttributeTier<?> tierAttr = Attribute.get(block, AttributeTier.class);
        BaseTier tier = tierAttr == null ? null : tierAttr.tier().getBaseTier();
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
