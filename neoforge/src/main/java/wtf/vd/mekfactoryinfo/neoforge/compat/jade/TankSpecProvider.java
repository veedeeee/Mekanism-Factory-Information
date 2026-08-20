package wtf.vd.mekfactoryinfo.neoforge.compat.jade;

import java.util.List;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.fluid.IExtendedFluidTank;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.util.text.EnergyDisplay;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import wtf.vd.mekfactoryinfo.MekFactoryInfo;
import wtf.vd.mekfactoryinfo.compat.mekanism.TankSpecHelper;
import wtf.vd.mekfactoryinfo.compat.mekanism.TankSpecHelper.TankSpec;

/**
 * Shows the current storage capacity and per-tick transfer rate when looking at a Mekanism Fluid or
 * Chemical Tank, or Energy Cube. While the viewing player is sneaking and holding a compatible Tier
 * Installer, shows a preview of the spec the installer would upgrade the tank to, mirroring
 * {@link FactoryLinesProvider}'s Shift+Installer preview convention.
 * <p>
 * Energy Cube storage and output are displayed using Mekanism's {@link EnergyDisplay} format
 * (e.g., "1.6 MFE", "800 kFE/t") to match the item tooltip. Fluid/Chemical Tanks use mB.
 */
public enum TankSpecProvider implements IBlockComponentProvider {

    INSTANCE;

    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(MekFactoryInfo.MOD_ID, "tank_spec");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        BlockState state = accessor.getBlockState();
        Long liveStorage = readLiveStorage(accessor.getBlockEntity());
        TankSpec current = TankSpecHelper.getCurrentSpec(state, liveStorage);
        if (current == null) {
            return;
        }

        TankSpec preview = null;
        Player player = accessor.getPlayer();
        if (player != null && player.isShiftKeyDown()) {
            ItemStack heldItem = player.getMainHandItem();
            preview = TankSpecHelper.getPreviewSpec(state, heldItem);
        }

        boolean isEnergyCube = TankSpecHelper.isEnergyCube(state);

        if (preview != null) {
            tooltip.add(Component.translatable("jade.mek_factory_info.tank_storage_preview",
                    formatStorage(current.storage(), isEnergyCube),
                    formatStorage(preview.storage(), isEnergyCube)));
            tooltip.add(Component.translatable("jade.mek_factory_info.tank_output_preview",
                    formatOutput(current.output(), isEnergyCube),
                    formatOutput(preview.output(), isEnergyCube)));
        } else {
            tooltip.add(Component.translatable("jade.mek_factory_info.tank_storage",
                    formatStorage(current.storage(), isEnergyCube)));
            tooltip.add(Component.translatable("jade.mek_factory_info.tank_output",
                    formatOutput(current.output(), isEnergyCube)));
        }
    }

    /**
     * Reads the live storage capacity off the given block entity's Fluid Tank / Chemical Tank /
     * Energy Container, whichever applies, via {@link TileEntityMekanism}'s generic accessors
     * (declared on the common Mekanism tile entity base class that every Fluid Tank, Chemical Tank,
     * and Energy Cube -- including addon equivalents -- extends), rather than the tier enum
     * directly, so addon mods that report a different capacity are reflected correctly without any
     * mod-specific handling. Returns {@code null} if not applicable (no block entity, or none of
     * these tank types apply); {@link TankSpecHelper#getCurrentSpec} falls back to the tier enum's
     * stock value in that case.
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
        List<IChemicalTank> chemicalTanks = tile.getChemicalTanks(null);
        if (!chemicalTanks.isEmpty()) {
            return chemicalTanks.get(0).getCapacity();
        }
        List<IEnergyContainer> energyContainers = tile.getEnergyContainers(null);
        if (!energyContainers.isEmpty()) {
            return energyContainers.get(0).getMaxEnergy();
        }
        return null;
    }

    private static String formatStorage(long value, boolean isEnergyCube) {
        if (isEnergyCube) {
            return EnergyDisplay.of(value).getTextComponent().getString();
        }
        return String.format("%,d mB", value);
    }

    private static String formatOutput(long value, boolean isEnergyCube) {
        if (isEnergyCube) {
            return EnergyDisplay.of(value).getTextComponent().getString() + "/t";
        }
        return String.format("%,d mB", value);
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
