package wtf.vd.mekfactoryinfo.forge.compat.jade;

import mekanism.api.math.FloatingLong;
import mekanism.common.util.text.EnergyDisplay;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import wtf.vd.mekfactoryinfo.MekFactoryInfo;
import wtf.vd.mekfactoryinfo.compat.mekanism.TankSpecHelper;
import wtf.vd.mekfactoryinfo.compat.mekanism.TankSpecHelper.TankSpec;

/**
 * Shows the current storage capacity and per-tick transfer rate when looking at a Mekanism Fluid or
 * Chemical Tank, or Energy Cube. Energy Cube uses EnergyDisplay format; Tanks use mB.
 */
public enum TankSpecProvider implements IBlockComponentProvider {

    INSTANCE;

    private static final ResourceLocation UID = new ResourceLocation(MekFactoryInfo.MOD_ID, "tank_spec");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        BlockState state = accessor.getBlockState();
        TankSpec current = TankSpecHelper.getCurrentSpec(state);
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

    private static String formatStorage(long value, boolean isEnergyCube) {
        if (isEnergyCube) {
            return EnergyDisplay.of(FloatingLong.createConst(value)).getTextComponent().getString();
        }
        return String.format("%,d mB", value);
    }

    private static String formatOutput(long value, boolean isEnergyCube) {
        if (isEnergyCube) {
            return EnergyDisplay.of(FloatingLong.createConst(value)).getTextComponent().getString() + "/t";
        }
        return String.format("%,d mB", value);
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
