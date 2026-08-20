package wtf.vd.mekfactoryinfo.forge.compat.jade;

import java.util.ArrayList;
import java.util.List;
import mekanism.api.math.FloatingLong;
import mekanism.common.util.text.EnergyDisplay;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
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

    // Mekanism's own MekanismLang translation keys (see mekanism.common.MekanismLang), universal
    // across every mod's transmitter items since addons (MekanismExtras, EvolvedMekanismExtras,
    // etc.) reuse these same MekanismLang entries in their own item tooltips rather than defining
    // their own. Addon mods commonly recompute the Pull/Speed values via their own private, addon-
    // specific static utility classes (e.g. MekanismExtras' TPTier/PTier) with no shared, overridable
    // method Mekanism itself exposes -- so instead of trying to recompute these values ourselves, we
    // read them back out of the block's own default-ItemStack tooltip (see #readTooltipArg), which
    // is guaranteed to already show whatever value that mod considers correct.
    private static final String KEY_TRANSPORTER_SPEED = "transmitter.mekanism.speed";
    private static final String KEY_TRANSPORTER_PUMP_RATE = "transmitter.mekanism.pump_rate";
    private static final String KEY_PIPE_TUBE_PUMP_RATE_MB = "transmitter.mekanism.pump_rate.mb";

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        BlockState state = accessor.getBlockState();
        TransmitterSpec current = CableSpecHelper.getCurrentSpec(state, accessor.getBlockEntity());
        if (current == null) {
            return;
        }

        TransmitterSpec preview = null;
        Player player = accessor.getPlayer();
        if (player != null && player.isShiftKeyDown()) {
            ItemStack heldItem = player.getMainHandItem();
            preview = CableSpecHelper.getPreviewSpec(state, accessor.getBlockEntity(), heldItem);
        }

        boolean isTransporter = isTransporter(state);

        if (isTransporter) {
            Long tooltipPull = readTooltipArg(state, KEY_TRANSPORTER_PUMP_RATE);
            Long tooltipSpeed = readTooltipArg(state, KEY_TRANSPORTER_SPEED);
            // Prefer the value straight out of the block's own item tooltip: addon mods
            // (MekanismExtras, EvolvedMekanismExtras, etc.) commonly recompute Speed/Pull via their
            // own private static utility classes rather than TransporterTier#getSpeed/getPullAmount,
            // so the tier-based fallback below can be wrong for those; the tooltip is always right.
            long pullPerSecond = tooltipPull != null ? tooltipPull : current.capacity() * 2;
            long speedMs = tooltipSpeed != null ? tooltipSpeed : current.rate() / 5; // matches ItemBlockLogisticalTransporter's item tooltip: speed / 5 (5 = 100 / 20)

            if (preview != null) {
                long previewPullPerSecond = preview.capacity() * 2;
                long previewSpeedMs = preview.rate() / 5;
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
            // Same reasoning as the Transporter's Pull/Speed above: addon Pipe/Tube tiers commonly
            // recompute their pull/rate via their own private static utility classes, so prefer the
            // value the block's own item tooltip already shows.
            Long tooltipRate = isCable ? null : readTooltipArg(state, KEY_PIPE_TUBE_PUMP_RATE_MB);
            long rate = tooltipRate != null ? tooltipRate : current.rate();

            if (preview != null) {
                if (isCable) {
                    tooltip.add(Component.translatable("jade.mek_factory_info.transmitter_capacity_preview",
                            formatEnergyCapacity(current.capacity()),
                            formatEnergyCapacity(preview.capacity())));
                    tooltip.add(Component.translatable("jade.mek_factory_info.transmitter_rate_preview",
                            formatEnergyRate(rate),
                            formatEnergyRate(preview.rate())));
                } else {
                    tooltip.add(Component.translatable("jade.mek_factory_info.transmitter_capacity_preview",
                            formatCapacity(current.capacity(), "mB"),
                            formatCapacity(preview.capacity(), "mB")));
                    tooltip.add(Component.translatable("jade.mek_factory_info.transmitter_rate_preview",
                            formatRate(rate, "mB/t"),
                            formatRate(preview.rate(), "mB/t")));
                }
            } else {
                if (isCable) {
                    tooltip.add(Component.translatable("jade.mek_factory_info.transmitter_capacity",
                            formatEnergyCapacity(current.capacity())));
                    tooltip.add(Component.translatable("jade.mek_factory_info.transmitter_rate",
                            formatEnergyRate(rate)));
                } else {
                    tooltip.add(Component.translatable("jade.mek_factory_info.transmitter_capacity",
                            formatCapacity(current.capacity(), "mB")));
                    tooltip.add(Component.translatable("jade.mek_factory_info.transmitter_rate",
                            formatRate(rate, "mB/t")));
                }
            }
        }
    }

    /**
     * Reads a numeric argument back out of the given block's own default-{@link ItemStack} tooltip
     * (via its {@code Item#appendHoverText}), looking for a line whose translation key matches
     * {@code translationKey}. Returns {@code null} if the block has no item, its tooltip doesn't
     * contain a matching line, or the argument isn't parseable as a number.
     */
    @Nullable
    private static Long readTooltipArg(BlockState state, String translationKey) {
        Item item = state.getBlock().asItem();
        if (item == Items.AIR) {
            return null;
        }
        ItemStack stack = new ItemStack(item);
        List<Component> lines = new ArrayList<>();
        try {
            item.appendHoverText(stack, null, lines, TooltipFlag.Default.NORMAL);
        } catch (RuntimeException e) {
            // Some addon tooltip implementations may depend on client-only state we can't safely
            // fake here (e.g. a null Level); treat any failure as "no value available".
            return null;
        }
        for (Component line : lines) {
            Long value = extractIfMatches(line, translationKey);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    @Nullable
    private static Long extractIfMatches(Component component, String translationKey) {
        if (component.getContents() instanceof TranslatableContents translatable && translatable.getKey().equals(translationKey)) {
            Object[] args = translatable.getArgs();
            if (args.length > 0) {
                Object last = args[args.length - 1];
                String raw = last instanceof Component argComponent ? argComponent.getString() : String.valueOf(last);
                String digits = raw.replaceAll("[^0-9]", "");
                if (!digits.isEmpty()) {
                    try {
                        return Long.parseLong(digits);
                    } catch (NumberFormatException ignored) {
                        // Not actually numeric; keep searching other lines.
                    }
                }
            }
        }
        for (Component sibling : component.getSiblings()) {
            Long value = extractIfMatches(sibling, translationKey);
            if (value != null) {
                return value;
            }
        }
        return null;
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
