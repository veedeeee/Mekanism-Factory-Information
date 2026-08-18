package wtf.vd.mekfactoryinfo.neoforge.compat.ae2;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.attribute.AttributeFactoryType;
import mekanism.common.content.blocktype.FactoryType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves the Mekanism machine family (if any) that an AE2 key belongs to, keyed off
 * {@link AttributeFactoryType}. Both a regular machine and every Factory tier of the same recipe
 * type share the same {@link FactoryType}, and {@link FactoryType#getBaseBlock()} always points back
 * to Mekanism's own base (non-Factory) machine block for that type - so this works without hardcoding
 * any specific machine and without a registry scan, and remains valid even for a future third-party
 * extension mod's Factory blocks as long as they reuse Mekanism's own {@code FactoryType} values (see
 * {@code AttributeFactoryType}/{@code Machine.FactoryMachine}).
 */
public final class FactoryFamilyResolver {

    private FactoryFamilyResolver() {
    }

    /**
     * Returns the family identity for {@code key}, or {@code null} if it is not backed by a
     * Mekanism block that carries {@link AttributeFactoryType} (i.e. not part of any Factory family).
     */
    @Nullable
    public static FactoryFamily resolve(AEKey key) {
        Block block = blockOf(key);
        if (block == null) {
            return null;
        }
        AttributeFactoryType attribute = Attribute.get(block, AttributeFactoryType.class);
        if (attribute == null) {
            return null;
        }
        FactoryType type = attribute.getFactoryType();
        Block representative = type.getBaseBlock().get();
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(representative);
        String name = representative.asItem().getDescription().getString();
        return new FactoryFamily(name, id.getNamespace());
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

