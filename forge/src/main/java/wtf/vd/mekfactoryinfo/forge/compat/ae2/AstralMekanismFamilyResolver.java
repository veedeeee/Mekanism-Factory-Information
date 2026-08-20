package wtf.vd.mekfactoryinfo.forge.compat.ae2;

import java.lang.reflect.Method;
import mekanism.common.block.interfaces.IHasTileEntity;
import mekanism.common.content.blocktype.FactoryType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import wtf.vd.mekfactoryinfo.compat.mekanism.FactoryLinesHelper;

/**
 * Explicit, addon-specific carve-out for <b>Astral Mekanism</b> (registry namespace
 * {@code astral_mekanism}).
 *
 * <p>Unlike every other addon this mod otherwise deals with, none of Astral Mekanism's machines --
 * not just its Energized/Astral Energized Smelting lines, but its Metallurgic Infuser, Precision
 * Sawmill, etc. as well -- reuse any of Mekanism's Factory-family attribute machinery at all (see
 * {@link FactoryFamilyResolver}). Some of them (the Smelting lines) are built on Astral Mekanism's
 * own, entirely separate {@code GeneralRecipeType} system and process plain vanilla
 * {@link net.minecraft.world.item.crafting.SmeltingRecipe}s; others (Metallurgic Infuser,
 * Precision Sawmill) directly reuse Mekanism's real {@code MekanismRecipeType} recipe types but
 * still carry none of the Factory attribute machinery. Either way there is no shared, reflectable
 * signal available via {@link FactoryFamilyResolver} to group them generically. Rather than leave
 * Astral Mekanism's machines permanently ungrouped, this resolver reads the (also
 * Astral-Mekanism-specific) {@code BlockEntity#getRecipeType()} convention via reflection -- using
 * {@link IHasTileEntity#createDummyBlockEntity()}, Mekanism's own supported mechanism for building
 * a data-only block entity instance with no world attached, exactly as JEI/tooltip code does --
 * and maps the resulting recipe type's registry path (e.g. {@code "smelting"}, or
 * {@code "metallurgic_infusing"} for the Metallurgic Infuser) onto Mekanism's base
 * {@link FactoryType} enum by name, so the block still lands in the same shared sort family as
 * every other mod's equivalent line, for every machine category this addon has -- not just
 * Smelting.
 *
 * <p>This is a deliberate, addon-specific exception to this mod's normal "no addon-specific
 * hardcoding" policy; see the linked tracking issue for the rationale.
 */
public final class AstralMekanismFamilyResolver {

    private static final String ASTRAL_MEKANISM_NAMESPACE = "astral_mekanism";

    private AstralMekanismFamilyResolver() {
    }

    @Nullable
    public static SortFamily resolve(Block block, ResourceLocation blockId) {
        if (!blockId.getNamespace().equals(ASTRAL_MEKANISM_NAMESPACE)) {
            return null;
        }
        if (!(block instanceof IHasTileEntity<?> hasTileEntity)) {
            return null;
        }
        BlockEntity dummy = createDummyBlockEntitySafely(hasTileEntity);
        if (dummy == null) {
            return null;
        }
        String recipeTypePath = readRecipeTypePath(dummy);
        if (recipeTypePath == null) {
            return null;
        }
        FactoryType matched = null;
        for (FactoryType candidate : FactoryType.values()) {
            String component = candidate.getRegistryNameComponent();
            // Exact match covers most categories (e.g. "sawing"); a "_"-prefixed suffix match
            // additionally covers compound recipe-type names like Metallurgic Infusing's
            // "metallurgic_infusing", which still functionally corresponds to FactoryType.INFUSING.
            if (component.equals(recipeTypePath) || recipeTypePath.endsWith("_" + component)) {
                matched = candidate;
                break;
            }
        }
        if (matched == null) {
            return null;
        }
        // Prefer the block-level Lines lookup (handles Astral Mekanism's own multi-line Factory
        // tiers), falling back to reading the dummy block entity directly for its single-tier,
        // non-Factory machines (e.g. Essential/Astral Metallurgic Infuser), which structurally have
        // no Factory-tier attribute at all to read Lines off of.
        Integer lines = FactoryLinesHelper.getLinesForBlock(block);
        if (lines == null) {
            lines = FactoryLinesHelper.getLinesForBlockEntity(dummy);
        }
        if (lines == null) {
            return null;
        }
        String name = Component.translatable(matched.getTranslationKey()).getString();
        return new SortFamily(name, ASTRAL_MEKANISM_NAMESPACE, lines);
    }

    /**
     * Wraps {@link IHasTileEntity#createDummyBlockEntity()} to tolerate any addon block entity whose
     * constructor throws when built outside a real world; returns {@code null} on any such failure
     * instead of propagating it.
     */
    @Nullable
    private static BlockEntity createDummyBlockEntitySafely(IHasTileEntity<?> hasTileEntity) {
        try {
            return hasTileEntity.createDummyBlockEntity();
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * Reads {@code BlockEntity#getRecipeType()} (Astral Mekanism's convention, returning an
     * {@code IUnifiedRecipeTypeProvider}) and then that result's {@code getRegistryName()}, both via
     * reflection, since neither type is part of a shared, importable API. Returns the resulting
     * {@link ResourceLocation}'s path (e.g. {@code "smelting"} for vanilla furnace recipes), or
     * {@code null} on any shape mismatch.
     */
    @Nullable
    private static String readRecipeTypePath(BlockEntity dummy) {
        Object recipeType;
        Object registryName;
        try {
            Method getRecipeType = dummy.getClass().getMethod("getRecipeType");
            recipeType = getRecipeType.invoke(dummy);
            if (recipeType == null) {
                return null;
            }
            Method getRegistryName = recipeType.getClass().getMethod("getRegistryName");
            registryName = getRegistryName.invoke(recipeType);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
        return registryName instanceof ResourceLocation resourceLocation ? resourceLocation.getPath() : null;
    }
}
