package wtf.vd.mekfactoryinfo.forge.tooltip;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import wtf.vd.mekfactoryinfo.MekFactoryInfo;
import wtf.vd.mekfactoryinfo.compat.mekanism.FactoryLinesHelper;

/**
 * Adds a "Lines: N" line to the regular mouse-hover tooltip of Factory and machine BlockItems.
 */
@Mod.EventBusSubscriber(modid = MekFactoryInfo.MOD_ID, value = Dist.CLIENT)
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
