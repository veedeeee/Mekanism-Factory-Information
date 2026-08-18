package wtf.vd.mekfactoryinfo.neoforge.compat.jade;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import wtf.vd.mekfactoryinfo.MekFactoryInfo;
import wtf.vd.mekfactoryinfo.compat.mekanism.FactoryLinesHelper;

/**
 * Shows the current processing Lines count when looking at a Factory block, or the implicit "1
 * Line" for a regular Mekanism machine that could be upgraded into one. While the viewing player is
 * sneaking and holding a compatible Tier Installer, shows a preview of the Lines count the installer
 * would upgrade the block to, e.g. {@code Lines: 3 -> 5} or {@code Lines: 1 -> 3}.
 */
public enum FactoryLinesProvider implements IBlockComponentProvider {

    INSTANCE;

    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(MekFactoryInfo.MOD_ID, "factory_lines");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        BlockState state = accessor.getBlockState();
        BlockEntity blockEntity = accessor.getBlockEntity();
        Integer currentLines = FactoryLinesHelper.getCurrentLines(state, blockEntity);
        if (currentLines == null) {
            return;
        }

        Integer previewLines = null;
        Player player = accessor.getPlayer();
        if (player != null && player.isShiftKeyDown()) {
            ItemStack heldItem = player.getMainHandItem();
            previewLines = FactoryLinesHelper.getPreviewLines(state, heldItem);
        }

        if (previewLines != null) {
            tooltip.add(Component.translatable("jade.mek_factory_info.lines_preview", currentLines, previewLines));
        } else {
            tooltip.add(Component.translatable("jade.mek_factory_info.lines", currentLines));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
