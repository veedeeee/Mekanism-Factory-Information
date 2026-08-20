package wtf.vd.mekfactoryinfo.neoforge.tooltip;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import wtf.vd.mekfactoryinfo.MekFactoryInfo;
import wtf.vd.mekfactoryinfo.compat.mekanism.FactoryLinesHelper;

/**
 * Adds a "Lines: N" line to the regular mouse-hover tooltip (inventory, JEI, etc.) of Factory and
 * regular (upgradeable-to-Factory) Mekanism machine {@code BlockItem}s, using the same tier-agnostic
 * {@link FactoryLinesHelper#getLinesForBlock} lookup already used by the AE2 Lines-grouping sort
 * feature. Unlike {@link wtf.vd.mekfactoryinfo.neoforge.compat.jade.FactoryLinesProvider}, this
 * applies to the item itself (no placed block/BlockEntity needed) and has no Installer preview,
 * since there is no "target" block to preview an upgrade onto.
 */
@EventBusSubscriber(modid = MekFactoryInfo.MOD_ID, value = Dist.CLIENT)
public final class FactoryLinesTooltipHandler {

    private FactoryLinesTooltipHandler() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!(event.getItemStack().getItem() instanceof BlockItem blockItem)) {
            return;
        }
        Integer lines = FactoryLinesHelper.getLinesForBlock(blockItem.getBlock());
        if (lines != null) {
            event.getToolTip().add(Component.translatable("tooltip.mek_factory_info.lines", lines));
        }
    }
}
