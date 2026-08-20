package wtf.vd.mekfactoryinfo.compat.mekanism;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import mekanism.api.tier.AlloyTier;
import mekanism.api.tier.BaseTier;
import mekanism.api.tier.ITier;
import mekanism.common.content.network.transmitter.BoxedPressurizedTube;
import mekanism.common.content.network.transmitter.BufferedTransmitter;
import mekanism.common.content.network.transmitter.LogisticalTransporter;
import mekanism.common.content.network.transmitter.MechanicalPipe;
import mekanism.common.content.network.transmitter.Transmitter;
import mekanism.common.content.network.transmitter.UniversalCable;
import mekanism.common.item.ItemAlloy;
import mekanism.common.tier.CableTier;
import mekanism.common.tier.PipeTier;
import mekanism.common.tier.TransporterTier;
import mekanism.common.tier.TubeTier;
import mekanism.common.tile.transmitter.TileEntityTransmitter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Forge 1.20.1 version of CableSpecHelper. Uses {@code FloatingLong} for cable capacity
 * (Mekanism 1.20.1 returns {@code FloatingLong} rather than {@code long} for energy values) and
 * {@code AlloyTier.getBaseTier()} (no {@code IAlloyTier.getBaseTierLevel()} in 1.20.1).
 *
 * @see wtf.vd.mekfactoryinfo.compat.mekanism.TierAttributeHelper
 */
public final class CableSpecHelper {

    private CableSpecHelper() {
    }

    // Mekanism's own MekanismLang translation keys (see mekanism.common.MekanismLang), universal
    // across every mod's transmitter items since addons (MekanismExtras, EvolvedMekanismExtras,
    // etc.) reuse these same MekanismLang entries in their own item tooltips rather than defining
    // their own. Used by #refinePreviewFromTooltip to correct an addon-computed preview's
    // capacity/rate against whatever value the mod's own tooltip actually shows (see its javadoc).
    private static final String KEY_CABLE_CAPACITY_PER_TICK = "capacity.mekanism.per_tick";
    private static final String KEY_PIPE_TUBE_CAPACITY_MB_PER_TICK = "capacity.mekanism.mb.per_tick";
    private static final String KEY_PIPE_TUBE_PUMP_RATE_MB = "transmitter.mekanism.pump_rate.mb";
    private static final String KEY_TRANSPORTER_PUMP_RATE = "transmitter.mekanism.pump_rate";
    private static final String KEY_TRANSPORTER_SPEED = "transmitter.mekanism.speed";

    /**
     * A transmitter's specs. Semantics vary by transmitter type:
     * <ul>
     *   <li>Cable: capacity (Joules) + transfer rate (J/t)</li>
     *   <li>Pipe: capacity (mB) + pull amount (mB/t)</li>
     *   <li>Tube: capacity (mB) + pull amount (mB/t)</li>
     *   <li>Transporter: pull amount (items/tick) + speed (per half-second)</li>
     * </ul>
     */
    public record TransmitterSpec(long capacity, long rate) {
    }

    @Nullable
    public static TransmitterSpec getCurrentSpec(BlockState state) {
        return getCurrentSpec(state, null);
    }

    /**
     * Block-entity-aware variant that reads Cable/Pipe/Tube capacity from the live transmitter's
     * overridable {@code BufferedTransmitter#getCapacity()} rather than the tier enum directly, so
     * addon mods that override it (e.g. MekanismExtras reusing a base tier enum but reporting a
     * larger capacity) are reflected correctly without any mod-specific handling. Transporter has no
     * capacity concept, and none of the rate/pull/speed values have an equivalent override point in
     * Mekanism's transmitter base classes, so those always come from the tier enum (best effort).
     */
    @Nullable
    public static TransmitterSpec getCurrentSpec(BlockState state, @Nullable BlockEntity blockEntity) {
        CableTier cableTier = TierAttributeHelper.getTierSafely(state.getBlockHolder(), CableTier.class);
        if (cableTier != null) {
            long capacity = getLiveCapacity(blockEntity, cableTier.getCableCapacity().longValue());
            return new TransmitterSpec(capacity, capacity);
        }
        PipeTier pipeTier = TierAttributeHelper.getTierSafely(state.getBlockHolder(), PipeTier.class);
        if (pipeTier != null) {
            long capacity = getLiveCapacity(blockEntity, (long) pipeTier.getPipeCapacity());
            return new TransmitterSpec(capacity, (long) pipeTier.getPipePullAmount());
        }
        TubeTier tubeTier = TierAttributeHelper.getTierSafely(state.getBlockHolder(), TubeTier.class);
        if (tubeTier != null) {
            long capacity = getLiveCapacity(blockEntity, tubeTier.getTubeCapacity());
            return new TransmitterSpec(capacity, tubeTier.getTubePullAmount());
        }
        TransporterTier transporterTier = TierAttributeHelper.getTierSafely(state.getBlockHolder(), TransporterTier.class);
        if (transporterTier != null) {
            return new TransmitterSpec((long) transporterTier.getPullAmount(), (long) transporterTier.getSpeed());
        }
        return null;
    }

    /**
     * Reads the live capacity off {@code blockEntity}'s {@code Transmitter} via the overridable
     * {@code BufferedTransmitter#getCapacity()} virtual method, falling back to {@code tierCapacity}
     * (the stock tier enum's value) if no block entity is available or it isn't a buffered
     * transmitter (e.g. Transporter, which has no capacity concept).
     */
    private static long getLiveCapacity(@Nullable BlockEntity blockEntity, long tierCapacity) {
        if (blockEntity instanceof TileEntityTransmitter te) {
            Transmitter<?, ?, ?> transmitter = te.getTransmitter();
            if (transmitter instanceof BufferedTransmitter<?, ?, ?, ?> buffered) {
                return buffered.getCapacity();
            }
        }
        return tierCapacity;
    }

    /**
     * @deprecated prefer {@link #getPreviewSpec(BlockState, BlockEntity, ItemStack)}: without a
     * block entity, this cannot distinguish a genuine vanilla transmitter from an addon's Java
     * subclass that merely reuses one of Mekanism's {@code BaseTier} constants for unrelated
     * bookkeeping but is not actually upgradeable by a plain Alloy.
     */
    @Deprecated
    @Nullable
    public static TransmitterSpec getPreviewSpec(BlockState state, ItemStack heldItem) {
        return getPreviewSpec(state, null, heldItem);
    }

    /**
     * Block-entity-aware variant of {@link #getPreviewSpec(BlockState, ItemStack)}. See the common
     * module's {@code CableSpecHelper} for the full rationale: requires that {@code blockEntity}'s
     * live {@code Transmitter} object is a genuine, unmodified instance of one of Mekanism's own
     * transmitter classes (its runtime class' package is Mekanism's own), which filters out
     * MekanismExtras/EvolvedMekanismExtras's transmitter subclasses that nominally reuse a vanilla
     * {@code BaseTier} constant but wire real upgrades through a separate mechanism a vanilla Alloy
     * can never trigger.
     */
    @Nullable
    public static TransmitterSpec getPreviewSpec(BlockState state, @Nullable BlockEntity blockEntity, ItemStack heldItem) {
        if (!(heldItem.getItem() instanceof ItemAlloy alloy)) {
            return previewViaAddonAlloy(state, blockEntity, heldItem.getItem());
        }
        if (!isGenuineVanillaTransmitter(blockEntity)) {
            return null;
        }
        AlloyTier alloyTier = alloy.getTier();
        BaseTier alloyBaseTier = alloyTier.getBaseTier();

        CableTier cableTier = TierAttributeHelper.getTierSafely(state.getBlockHolder(), CableTier.class);
        if (cableTier != null && TierRank.isImmediatelyAbove(alloyBaseTier, cableTier.getBaseTier())) {
            CableTier nextTier = findByBaseTier(CableTier.values(), alloyBaseTier);
            if (nextTier == null) {
                return null;
            }
            long capacity = nextTier.getCableCapacity().longValue();
            return new TransmitterSpec(capacity, capacity);
        }

        PipeTier pipeTier = TierAttributeHelper.getTierSafely(state.getBlockHolder(), PipeTier.class);
        if (pipeTier != null && TierRank.isImmediatelyAbove(alloyBaseTier, pipeTier.getBaseTier())) {
            PipeTier nextTier = findByBaseTier(PipeTier.values(), alloyBaseTier);
            if (nextTier == null) {
                return null;
            }
            return new TransmitterSpec((long) nextTier.getPipeCapacity(), (long) nextTier.getPipePullAmount());
        }

        TubeTier tubeTier = TierAttributeHelper.getTierSafely(state.getBlockHolder(), TubeTier.class);
        if (tubeTier != null && TierRank.isImmediatelyAbove(alloyBaseTier, tubeTier.getBaseTier())) {
            TubeTier nextTier = findByBaseTier(TubeTier.values(), alloyBaseTier);
            if (nextTier == null) {
                return null;
            }
            return new TransmitterSpec(nextTier.getTubeCapacity(), nextTier.getTubePullAmount());
        }

        TransporterTier transporterTier = TierAttributeHelper.getTierSafely(state.getBlockHolder(), TransporterTier.class);
        if (transporterTier != null && TierRank.isImmediatelyAbove(alloyBaseTier, transporterTier.getBaseTier())) {
            TransporterTier nextTier = findByBaseTier(TransporterTier.values(), alloyBaseTier);
            if (nextTier == null) {
                return null;
            }
            return new TransmitterSpec((long) nextTier.getPullAmount(), (long) nextTier.getSpeed());
        }

        return null;
    }

    /**
     * Reflective preview for addon Alloy items that don't extend vanilla {@link ItemAlloy} at all
     * (e.g. MekanismExtras' {@code ExtraItemAlloy}, {@code ItemAlloyRadiance}). See the common
     * module's {@code CableSpecHelper} javadoc for the full rationale: a {@code getTier()} accessor
     * on the item, a one-level {@code get*Tier()} unwrap down to the type the tile entity's own
     * {@code upgradeResult} expects (see {@link TierBridgeHelper#unwrapOneLevel}), and a (possibly
     * non-public) {@code upgradeResult(BlockState, tier)} method declared somewhere in the tile
     * entity's class hierarchy drive the preview purely via reflection.
     */
    @Nullable
    private static TransmitterSpec previewViaAddonAlloy(BlockState state, @Nullable BlockEntity blockEntity, Item item) {
        if (!(blockEntity instanceof TileEntityTransmitter)) {
            return null;
        }
        Object tierObj = tryInvoke(item, "getTier");
        if (tierObj == null) {
            return null;
        }
        Object unwrapped = TierBridgeHelper.unwrapOneLevel(tierObj);
        for (Class<?> cls = blockEntity.getClass(); cls != null && cls != Object.class; cls = cls.getSuperclass()) {
            for (Method method : cls.getDeclaredMethods()) {
                if (!method.getName().equals("upgradeResult") || method.getParameterCount() != 2) {
                    continue;
                }
                Class<?> paramType = method.getParameterTypes()[1];
                Object arg = paramType.isInstance(tierObj) ? tierObj : (paramType.isInstance(unwrapped) ? unwrapped : null);
                if (arg == null) {
                    continue;
                }
                try {
                    method.setAccessible(true);
                    Object result = method.invoke(blockEntity, state, arg);
                    if (!(result instanceof BlockState upgraded) || upgraded == state) {
                        return null;
                    }
                    TransmitterSpec fallback = getCurrentSpec(upgraded);
                    return fallback == null ? null : refinePreviewFromTooltip(upgraded, fallback);
                } catch (ReflectiveOperationException e) {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Corrects a preview spec computed purely from an addon's (possibly reused) tier enum -- which,
     * for mods like MekanismExtras, can be wildly smaller than the value the mod actually uses at
     * runtime (it overrides capacity/rate via its own config-driven static lookup rather than the
     * tier enum's stock fields; see the class doc) -- by reading the *actual* value back out of the
     * upgraded block's own default-{@link ItemStack} tooltip, the same technique
     * {@code CableSpecProvider} already relies on for reading addon-recomputed *current* pull rates.
     * That tooltip is guaranteed to already show whatever value the mod considers correct, regardless
     * of how it's computed internally. Falls back to {@code fallback} for any value whose tooltip
     * line can't be found or parsed (e.g. a genuinely vanilla/Evolved-Mixin transmitter, whose tier
     * enum value was already correct).
     */
    private static TransmitterSpec refinePreviewFromTooltip(BlockState upgraded, TransmitterSpec fallback) {
        Item item = upgraded.getBlock().asItem();
        if (item == Items.AIR) {
            return fallback;
        }
        if (TierAttributeHelper.getTierSafely(upgraded.getBlockHolder(), CableTier.class) != null) {
            Long capacity = readTooltipArg(item, KEY_CABLE_CAPACITY_PER_TICK);
            return capacity == null ? fallback : new TransmitterSpec(capacity, capacity);
        }
        boolean isPipeOrTube = TierAttributeHelper.getTierSafely(upgraded.getBlockHolder(), PipeTier.class) != null
                || TierAttributeHelper.getTierSafely(upgraded.getBlockHolder(), TubeTier.class) != null;
        if (isPipeOrTube) {
            Long capacity = readTooltipArg(item, KEY_PIPE_TUBE_CAPACITY_MB_PER_TICK);
            Long rate = readTooltipArg(item, KEY_PIPE_TUBE_PUMP_RATE_MB);
            return new TransmitterSpec(capacity != null ? capacity : fallback.capacity(), rate != null ? rate : fallback.rate());
        }
        if (TierAttributeHelper.getTierSafely(upgraded.getBlockHolder(), TransporterTier.class) != null) {
            // The tooltip shows Pull/Speed already converted for display (pull * 2, speed / 5, see
            // ItemBlockLogisticalTransporter); undo that conversion to keep TransmitterSpec's raw units.
            Long tooltipPull = readTooltipArg(item, KEY_TRANSPORTER_PUMP_RATE);
            Long tooltipSpeed = readTooltipArg(item, KEY_TRANSPORTER_SPEED);
            long pull = tooltipPull != null ? tooltipPull / 2 : fallback.capacity();
            long speed = tooltipSpeed != null ? tooltipSpeed * 5 : fallback.rate();
            return new TransmitterSpec(pull, speed);
        }
        return fallback;
    }

    /**
     * Reads a numeric argument back out of {@code item}'s own default-{@link ItemStack} tooltip (via
     * {@code Item#appendHoverText}), looking for a line whose translation key matches
     * {@code translationKey}. Returns {@code null} if the tooltip has no matching line, or its
     * argument isn't parseable as a number.
     */
    @Nullable
    private static Long readTooltipArg(Item item, String translationKey) {
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

    @Nullable
    private static Object tryInvoke(Item item, String methodName) {
        try {
            Method method = item.getClass().getMethod(methodName);
            return method.invoke(item);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    /**
     * Returns {@code true} if {@code blockEntity} is a {@link TileEntityTransmitter} whose live
     * {@code Transmitter} object is a genuine, unmodified instance of one of Mekanism's own
     * transmitter classes ({@link UniversalCable}, {@link MechanicalPipe},
     * {@link BoxedPressurizedTube}, {@link LogisticalTransporter}). See the common module's
     * {@code CableSpecHelper} javadoc for the full rationale.
     */
    private static boolean isGenuineVanillaTransmitter(@Nullable BlockEntity blockEntity) {
        if (!(blockEntity instanceof TileEntityTransmitter te)) {
            return false;
        }
        Transmitter<?, ?, ?> transmitter = te.getTransmitter();
        if (transmitter == null) {
            return false;
        }
        return transmitter.getClass().getPackageName().equals(UniversalCable.class.getPackageName());
    }

    /**
     * Finds the tier in {@code tiers} whose {@link ITier#getBaseTier()} equals {@code target}, or
     * {@code null} if none matches. Deliberately scans a freshly-obtained {@code values()} array
     * (always reflecting the enum's current, possibly Mixin-extended {@code $VALUES}) rather than
     * calling the tier class's own static {@code get(BaseTier)} helper: that helper loops over a
     * {@code EnumUtils.XXX_TIERS} array that is cached once, the first time it's referenced, as a
     * {@code static final} field -- if that happens to run before an addon's Mixin (e.g. Evolved
     * Mekanism's {@code CableTierMixin}) has appended its own tiers to {@code $VALUES}, the cached
     * array is permanently stale and {@code get(BaseTier)} can never find those addon tiers,
     * silently falling back to the lowest tier's stats instead. Calling {@code values()} directly
     * avoids that caching race entirely.
     */
    @Nullable
    private static <T extends ITier> T findByBaseTier(T[] tiers, BaseTier target) {
        for (T tier : tiers) {
            if (tier.getBaseTier() == target) {
                return tier;
            }
        }
        return null;
    }
}
