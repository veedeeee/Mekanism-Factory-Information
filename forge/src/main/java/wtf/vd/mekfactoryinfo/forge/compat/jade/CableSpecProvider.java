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
import wtf.vd.mekfactoryinfo.compat.mekanism.CableSpecHelper;
import wtf.vd.mekfactoryinfo.compat.mekanism.CableSpecHelper.TransmitterSpec;

/**
 * Shows the current capacity and transfer rate for Mekanism transmitters.
 * Cable uses EnergyDisplay; Pipe/Tube use mB; Transporter uses Pull/Speed with Mekanism's conversion.
 */
public enum CableSpecProvider implements IBlockComponentProvider {

    INSTANCE;

    private static final ResourceLocation UID = new ResourceLocation(MekFactoryInfo.MOD_ID, "cable_spec");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        BlockState state = accessor.getBlockState();
        TransmitterSpec current = CableSpecHelper.getCurrentSpec(state);
        if (current == null) {
            return;
        }

        TransmitterSpec preview = null;
        Player player = accessor.getPlayer();
        if (player != null && player.isShiftKeyDown()) {
            ItemStack heldItem = player.getMainHandItem();
            preview = CableSpecHelper.getPreviewSpec(state, heldItem);
        }

        boolean isTransporter = isTransporter(state);

        if (isTransporter) {
            long pullPerSecond = current.capacity() * 2;
            long speedMs = current.rate() / 100;

            if (preview != null) {
                long previewPullPerSecond = preview.capacity() * 2;
                long previewSpeedMs = preview.rate() / 100;
                tooltip.add(Component.translatable("jade.mek_factory_info.transmitter_pull_preview",
                        formatRate(pullPerSecond, "/s"),
                        formatRate(previewPullPerSecond, "/s")));
                tooltip.add(Component.translatable("jade.mek_factory_info.transmitter_speed_preview",
                        formatRate(speedMs, "m/s"),
                        formatRate(previewSpeedMs, "m/s")));
            } else {
                tooltip.add(Component.translatable("jade.mek_factory_info.transmitter_pull",
                        formatRate(pullPerSecond, "/s")));
                tooltip.add(Component.translatable("jade.mek_factory_info.transmitter_speed",
                        formatRate(speedMs, "m/s")));
            }
        } else {
            boolean isCable = isCable(state);

            if (preview != null) {
                if (isCable) {
                    tooltip.add(Component.translatable("jade.mek_factory_info.transmitter_capacity_preview",
                            formatEnergyCapacity(current.capacity()),
                            formatEnergyCapacity(preview.capacity())));
                    tooltip.add(Component.translatable("jade.mek_factory_info.transmitter_rate_preview",
                            formatEnergyRate(current.rate()),
                            formatEnergyRate(preview.rate())));
                } else {
                    tooltip.add(Component.translatable("jade.mek_factory_info.transmitter_capacity_preview",
                            formatCapacity(current.capacity(), "mB"),
                            formatCapacity(preview.capacity(), "mB")));
                    tooltip.add(Component.translatable("jade.mek_factory_info.transmitter_rate_preview",
                            formatRate(current.rate(), "mB/t"),
                            formatRate(preview.rate(), "mB/t")));
                }
            } else {
                if (isCable) {
                    tooltip.add(Component.translatable("jade.mek_factory_info.transmitter_capacity",
                            formatEnergyCapacity(current.capacity())));
                    tooltip.add(Component.translatable("jade.mek_factory_info.transmitter_rate",
                            formatEnergyRate(current.rate())));
                } else {
                    tooltip.add(Component.translatable("jade.mek_factory_info.transmitter_capacity",
                            formatCapacity(current.capacity(), "mB")));
                    tooltip.add(Component.translatable("jade.mek_factory_info.transmitter_rate",
                            formatRate(current.rate(), "mB/t")));
                }
            }
        }
    }

    private static boolean isCable(BlockState state) {
        mekanism.common.tier.CableTier cableTier = wtf.vd.mekfactoryinfo.compat.mekanism.TierAttributeHelper
                .getTierSafely(state.getBlockHolder(), mekanism.common.tier.CableTier.class);
        return cableTier != null;
    }

    private static boolean isTransporter(BlockState state) {
        mekanism.common.tier.TransporterTier transporterTier = wtf.vd.mekfactoryinfo.compat.mekanism.TierAttributeHelper
                .getTierSafely(state.getBlockHolder(), mekanism.common.tier.TransporterTier.class);
        return transporterTier != null;
    }

    private static String formatEnergyCapacity(long joules) {
        return EnergyDisplay.of(FloatingLong.createConst(joules)).getTextComponent().getString();
    }

    private static String formatEnergyRate(long joulesPerTick) {
        return formatRate(joulesPerTick, "FE/t");
    }

    private static String formatCapacity(long value, String unit) {
        return String.format("%,d %s", value, unit);
    }

    private static String formatRate(long value, String unit) {
        return String.format("%,d %s", value, unit);
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
