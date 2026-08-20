package wtf.vd.mekfactoryinfo.compat.mekanism;

import mekanism.api.chemical.IChemicalTank;
import mekanism.api.chemical.gas.IGasTank;
import mekanism.api.chemical.infuse.IInfusionTank;
import mekanism.api.chemical.pigment.IPigmentTank;
import mekanism.api.chemical.slurry.ISlurryTank;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.fluid.IExtendedFluidTank;
import mekanism.api.tier.BaseTier;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.attribute.AttributeUpgradeable;
import mekanism.common.block.interfaces.ITypeBlock;
import mekanism.common.item.ItemTierInstaller;
import mekanism.common.tier.ChemicalTankTier;
import mekanism.common.tier.EnergyCubeTier;
import mekanism.common.tier.FluidTankTier;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Forge 1.20.1 version of TankSpecHelper. Uses {@code FloatingLong.longValue()} for energy
 * values (Mekanism 1.20.1 returns {@code FloatingLong} for {@code EnergyCubeTier} methods) and
 * {@code Attribute.getBaseTier(Block)} (the 1.21.1 {@code Holder<Block>} overload doesn't exist
 * in 1.20.1 Mekanism).
 */
public final class TankSpecHelper {

    private TankSpecHelper() {
    }

    /**
     * A storage device's capacity and per-tick transfer rate in the device's base unit (mB for
     * fluid/chemical tanks, Joules for energy cubes), normalised to {@code long}.
     */
    public record TankSpec(long storage, long output) {
    }

    @Nullable
    public static TankSpec getCurrentSpec(BlockState state) {
        return getCurrentSpec(state, null);
    }

    /**
     * Block-entity-aware variant that reads the live storage capacity off the block entity's Fluid
     * Tank / Chemical Tank (Gas/Infusion/Pigment/Slurry) / Energy Container via
     * {@link TileEntityMekanism}'s generic accessors (declared on the common Mekanism tile entity
     * base class that every Fluid Tank, Chemical Tank, and Energy Cube -- including addon
     * equivalents -- extends), rather than the tier enum directly, so addon mods that report a
     * different capacity are reflected correctly without any mod-specific handling. The output rate
     * has no equivalent generic accessor (it's read from the tier inline at use), so it always comes
     * from the tier enum (best effort).
     */
    @Nullable
    public static TankSpec getCurrentSpec(BlockState state, @Nullable BlockEntity blockEntity) {
        FluidTankTier fluidTier = TierAttributeHelper.getTierSafely(state.getBlockHolder(), FluidTankTier.class);
        if (fluidTier != null) {
            Long storage = readLiveStorage(blockEntity);
            return new TankSpec(storage != null ? storage : fluidTier.getStorage(), fluidTier.getOutput());
        }
        ChemicalTankTier chemicalTier = TierAttributeHelper.getTierSafely(state.getBlockHolder(), ChemicalTankTier.class);
        if (chemicalTier != null) {
            Long storage = readLiveStorage(blockEntity);
            return new TankSpec(storage != null ? storage : chemicalTier.getStorage(), chemicalTier.getOutput());
        }
        EnergyCubeTier energyTier = TierAttributeHelper.getTierSafely(state.getBlockHolder(), EnergyCubeTier.class);
        if (energyTier != null) {
            Long storage = readLiveStorage(blockEntity);
            return new TankSpec(storage != null ? storage : energyTier.getMaxEnergy().longValue(), energyTier.getOutput().longValue());
        }
        // Addon mods (MekanismExtras, etc.) use their own custom tier type (e.g. FTTier, ECTier) via a
        // custom Attribute record (not AttributeTier) with a tier() accessor, rather than the base
        // FluidTankTier/ChemicalTankTier/EnergyCubeTier enums. Fall back to reading that tier's own
        // getStorage/getMaxEnergy + getOutput methods (same naming convention as the base tiers) via
        // reflection.
        AddonTankTier addonTier = getAddonTankTier(state.getBlock());
        if (addonTier != null) {
            Long storage = readLiveStorage(blockEntity);
            return new TankSpec(storage != null ? storage : addonTier.storage(), addonTier.output());
        }
        return null;
    }

    /**
     * An addon's tank/energy cube tier spec, read via reflection off its custom tier object (see
     * {@link #getAddonTankTier}).
     */
    private record AddonTankTier(long storage, long output, boolean isEnergy) {
    }

    /**
     * Attempts to read a storage/output spec from an addon Tank/Energy Cube block's custom tier
     * attribute via reflection. Addon mods implement {@code Attribute} as a Java record or class with
     * a {@code tier()} accessor; the returned tier object is expected to expose either
     * {@code getMaxEnergy()} (Energy Cube) or {@code getStorage()} (Fluid/Chemical Tank), plus
     * {@code getOutput()} in both cases -- the same method names Mekanism's own tier enums use.
     * Returns {@code null} on any mismatch (e.g. transmitter tiers, non-tank attributes).
     */
    @Nullable
    private static AddonTankTier getAddonTankTier(Block block) {
        if (!(block instanceof ITypeBlock typeBlock)) {
            return null;
        }
        for (Attribute attr : typeBlock.getType().getAll()) {
            Object tierObj = getTierAccessor(attr);
            if (tierObj == null) {
                continue;
            }
            Long maxEnergy = invokeAsLong(tierObj, "getMaxEnergy");
            Long output = invokeAsLong(tierObj, "getOutput");
            if (maxEnergy != null && output != null) {
                return new AddonTankTier(maxEnergy, output, true);
            }
            Long storage = invokeAsLong(tierObj, "getStorage");
            if (storage != null && output != null) {
                return new AddonTankTier(storage, output, false);
            }
        }
        return null;
    }

    /**
     * Reads the object returned by an {@link Attribute}'s {@code tier()} accessor, if it has one.
     * Returns {@code null} on any mismatch (e.g. non-tier attributes such as facing/state ones).
     */
    @Nullable
    private static Object getTierAccessor(Attribute attr) {
        try {
            Method tierMethod = attr.getClass().getMethod("tier");
            return tierMethod.invoke(attr);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    /**
     * Invokes the given no-arg method by name on {@code target} and coerces its return value to
     * {@code long}. Supports {@code long}/{@code int} returns directly, and Mekanism 1.20.1's
     * {@code FloatingLong} (via its {@code longValue()} method) since that's what {@code EnergyCubeTier}
     * and addon energy tiers commonly return for Joule amounts. Returns {@code null} if the method
     * doesn't exist or its return type isn't one of these.
     */
    @Nullable
    private static Long invokeAsLong(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            Object result = method.invoke(target);
            if (result instanceof Long value) {
                return value;
            }
            if (result instanceof Integer value) {
                return value.longValue();
            }
            if (result != null) {
                // Mekanism 1.20.1's FloatingLong (used by EnergyCubeTier et al.) exposes longValue().
                try {
                    Method longValue = result.getClass().getMethod("longValue");
                    return (Long) longValue.invoke(result);
                } catch (ReflectiveOperationException ignored) {
                    // Not a FloatingLong-like type; fall through to null.
                }
            }
        } catch (ReflectiveOperationException e) {
            // Method doesn't exist on this tier type; not applicable.
        }
        return null;
    }

    /**
     * Reads the live storage capacity off the given block entity's Fluid Tank / Chemical Tank
     * (Gas/Infusion/Pigment/Slurry) / Energy Container, whichever applies. Returns {@code null} if
     * not applicable (no block entity, or none of these tank types apply).
     */
    @Nullable
    private static Long readLiveStorage(@Nullable BlockEntity blockEntity) {
        if (!(blockEntity instanceof TileEntityMekanism tile)) {
            return null;
        }
        List<IExtendedFluidTank> fluidTanks = tile.getFluidTanks(null);
        if (!fluidTanks.isEmpty()) {
            return (long) fluidTanks.get(0).getCapacity();
        }
        // Mekanism 1.20.1's Chemical Tank block can hold any one of four chemical types, each with
        // its own accessor on TileEntityMekanism (no unified getChemicalTanks in this version).
        List<IGasTank> gasTanks = tile.getGasTanks(null);
        if (!gasTanks.isEmpty()) {
            return getFirstCapacity(gasTanks);
        }
        List<IInfusionTank> infusionTanks = tile.getInfusionTanks(null);
        if (!infusionTanks.isEmpty()) {
            return getFirstCapacity(infusionTanks);
        }
        List<IPigmentTank> pigmentTanks = tile.getPigmentTanks(null);
        if (!pigmentTanks.isEmpty()) {
            return getFirstCapacity(pigmentTanks);
        }
        List<ISlurryTank> slurryTanks = tile.getSlurryTanks(null);
        if (!slurryTanks.isEmpty()) {
            return getFirstCapacity(slurryTanks);
        }
        List<IEnergyContainer> energyContainers = tile.getEnergyContainers(null);
        if (!energyContainers.isEmpty()) {
            return energyContainers.get(0).getMaxEnergy().longValue();
        }
        return null;
    }

    private static long getFirstCapacity(List<? extends IChemicalTank<?, ?>> tanks) {
        return tanks.get(0).getCapacity();
    }

    public static boolean isEnergyCube(BlockState state) {
        if (TierAttributeHelper.getTierSafely(state.getBlockHolder(), EnergyCubeTier.class) != null) {
            return true;
        }
        AddonTankTier addonTier = getAddonTankTier(state.getBlock());
        return addonTier != null && addonTier.isEnergy();
    }

    @Nullable
    public static TankSpec getPreviewSpec(BlockState state, ItemStack heldItem) {
        if (!(heldItem.getItem() instanceof ItemTierInstaller installer)) {
            return null;
        }
        AttributeUpgradeable upgradeable = Attribute.get(state, AttributeUpgradeable.class);
        if (upgradeable == null) {
            return null;
        }
        // In Mekanism 1.20.1, getBaseTier takes Block (not Holder<Block>).
        BaseTier currentBaseTier = Attribute.getBaseTier(state.getBlock());
        if (currentBaseTier != installer.getFromTier() || currentBaseTier == installer.getToTier()) {
            return null;
        }
        BlockState upgraded = upgradeable.upgradeResult(state, installer.getToTier());
        if (upgraded == state) {
            return null;
        }
        return getCurrentSpec(upgraded);
    }
}
