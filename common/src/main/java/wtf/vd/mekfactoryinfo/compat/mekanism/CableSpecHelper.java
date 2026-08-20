package wtf.vd.mekfactoryinfo.compat.mekanism;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import mekanism.api.tier.BaseTier;
import mekanism.api.tier.ITier;
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
 * Shared, loader-agnostic logic for reading Mekanism transmitter specs and for previewing what a
 * held Alloy would change them to. Supports:
 * <ul>
 *   <li>Universal Cable (CableTier): capacity + transfer rate</li>
 *   <li>Mechanical Pipe (PipeTier): capacity + pull amount</li>
 *   <li>Pressurized Tube (TubeTier): capacity + pull amount</li>
 *   <li>Logistical/Restrictive/Diversion Transporter (TransporterTier): pull amount + speed</li>
 * </ul>
 * <p>
 * Note: Thermodynamic Conductor is intentionally excluded; it uses ConductorTier which is not a
 * "capacity" tier (uses conduction and insulation stats instead), so no meaningful Jade tooltip
 * would apply.
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
     * A transmitter's specs. Meaning varies by transmitter type:
     * <ul>
     *   <li>Cable: capacity (Joules) + transfer rate (J/t)</li>
     *   <li>Pipe: capacity (mB) + pull amount (mB/t)</li>
     *   <li>Tube: capacity (mB) + pull amount (mB/t)</li>
     *   <li>Transporter: pull amount (items/tick) + speed (per half-second)</li>
     * </ul>
     */
    public record TransmitterSpec(long capacity, long rate) {
    }

    /**
     * Returns the current specs for the given block state, or {@code null} if it isn't a Mekanism
     * tiered transmitter (Cable, Pipe, Tube, or Transporter). Reads the tier enum's stock capacity;
     * prefer {@link #getCurrentSpec(BlockState, BlockEntity)} when a block entity is available, since
     * addon mods (e.g. MekanismExtras) can override a transmitter's live capacity to a different value
     * than its (reused) tier enum reports.
     */
    @Nullable
    public static TransmitterSpec getCurrentSpec(BlockState state) {
        return getCurrentSpec(state, null);
    }

    /**
     * Block-entity-aware variant of {@link #getCurrentSpec(BlockState)}. For Cable/Pipe/Tube, the
     * capacity is read from the live transmitter object's overridable {@code getCapacity()} (declared
     * on {@code BufferedTransmitter}) rather than the tier enum directly, so addon mods that override
     * it (e.g. MekanismExtras reusing a base tier enum but reporting a larger capacity) are reflected
     * correctly without any mod-specific handling. The transfer rate (Cable) / pull amount (Pipe,
     * Tube, Transporter) / speed (Transporter) have no such override point in Mekanism's transmitter
     * base classes, so those remain read from the tier enum (best effort; addons that swap the tier's
     * values via a private static lookup, rather than overriding a virtual method, cannot be reflected
     * generically).
     */
    @Nullable
    public static TransmitterSpec getCurrentSpec(BlockState state, @Nullable BlockEntity blockEntity) {
        CableTier cableTier = TierAttributeHelper.getTierSafely(state.getBlockHolder(), CableTier.class);
        if (cableTier != null) {
            long capacity = getLiveCapacity(blockEntity, cableTier.getCableCapacity());
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
     * If {@code heldItem} is an Alloy that could legally upgrade the transmitter currently in
     * {@code state} (i.e. its tier is exactly one level above the transmitter's current tier,
     * mirroring {@code IUpgradeableTransmitter#canUpgrade}), returns the resulting specs after that
     * upgrade. Returns {@code null} otherwise.
     *
     * @deprecated prefer {@link #getPreviewSpec(BlockState, BlockEntity, ItemStack)}: without a
     * block entity, this cannot distinguish a genuine vanilla transmitter from an addon's Java
     * subclass that merely reuses one of Mekanism's {@code BaseTier} constants for unrelated
     * bookkeeping (see MekanismExtras' {@code CTier}/{@code PTier}/etc, which remap
     * {@code BaseTier.BASIC/ADVANCED/ELITE/ULTIMATE} to Absolute/Supreme/Cosmic/Infinite) but is not
     * actually upgradeable by a plain Alloy.
     */
    @Deprecated
    @Nullable
    public static TransmitterSpec getPreviewSpec(BlockState state, ItemStack heldItem) {
        return getPreviewSpec(state, null, heldItem);
    }

    /**
     * Block-entity-aware variant of {@link #getPreviewSpec(BlockState, ItemStack)}. Requires that
     * {@code blockEntity}'s live {@code Transmitter} object is a genuine instance of one of
     * Mekanism's own transmitter classes ({@link UniversalCable}, {@link MechanicalPipe},
     * PressurizedTube/BoxedPressurizedTube, {@link LogisticalTransporter}) -- i.e. its own package is
     * Mekanism's, not an addon's -- before returning a preview.
     * <p>
     * This is what filters out MekanismExtras/EvolvedMekanismExtras's transmitters (e.g.
     * {@code ExtraUniversalCable extends UniversalCable}): they nominally reuse one of Mekanism's own
     * {@code BaseTier} constants (see the class doc above), which would otherwise make {@link
     * TierRank#isImmediatelyAbove} think a plain vanilla Alloy could upgrade them one tier -- but
     * those addons wire actual upgrades through a completely separate, addon-specific
     * upgradeable-transmitter interface and tier system that a vanilla Alloy can never trigger
     * in-game, so showing a preview here would be a false positive. Evolved Mekanism, by contrast,
     * Mixins its extra tiers directly onto Mekanism's own transmitter classes (no Java subclass), so
     * its transmitters still pass this check and correctly continue to show previews.
     */
    @Nullable
    public static TransmitterSpec getPreviewSpec(BlockState state, @Nullable BlockEntity blockEntity, ItemStack heldItem) {
        if (!(heldItem.getItem() instanceof ItemAlloy alloy)) {
            return previewViaAddonAlloy(state, blockEntity, heldItem.getItem());
        }
        if (!isGenuineVanillaTransmitter(blockEntity)) {
            return null;
        }
        // NeoForge's ItemAlloy#getTier() returns IAlloyTier (no getBaseTier()), only
        // getBaseTierLevel(); round-trip through BaseTier.getTier(int) to recover the enum constant.
        BaseTier alloyBaseTier = BaseTier.getTier(alloy.getTier().getBaseTierLevel());

        CableTier cableTier = TierAttributeHelper.getTierSafely(state.getBlockHolder(), CableTier.class);
        if (cableTier != null && TierRank.isImmediatelyAbove(alloyBaseTier, cableTier.getBaseTier())) {
            CableTier nextTier = findByBaseTier(CableTier.values(), alloyBaseTier);
            if (nextTier == null) {
                return null;
            }
            return new TransmitterSpec(nextTier.getCableCapacity(), nextTier.getCableCapacity());
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
     * (e.g. MekanismExtras' {@code ExtraItemAlloy}, {@code ItemAlloyRadiance}). Every one of these,
     * and the transmitter tile entity classes they can upgrade (MekanismExtras'
     * {@code ExtraTileEntityXxx}, EvolvedMekanismExtras' equivalents), follow the same structural
     * convention as the Tier Installer preview in {@link FactoryLinesHelper}: a {@code getTier()}
     * accessor on the item, a one-level {@code get*Tier()} unwrap down to the type the tile entity's
     * own {@code upgradeResult} expects (see {@link TierBridgeHelper#unwrapOneLevel}), and a
     * (possibly non-public) {@code upgradeResult(BlockState, tier)} method declared somewhere in the
     * tile entity's class hierarchy -- so this drives the preview purely via reflection instead of an
     * addon-specific mapping.
     * <p>
     * Note this cannot discover cross-addon combinations that translate between two unrelated tier
     * enums outside of that one-level-unwrap convention (e.g. EvolvedMekanismExtras' own glue code
     * converting a MekanismExtras Alloy's tier into its own distinct tier enum for its own
     * transmitters) -- those fall outside what's reachable this way and simply show no preview,
     * rather than a wrong one.
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
     * PressurizedTube/BoxedPressurizedTube, {@link LogisticalTransporter}) -- i.e. its runtime class' package
     * is Mekanism's own {@code mekanism.common.content.network.transmitter} package, not some addon's
     * package.
     * <p>
     * Evolved Mekanism adds its extra tiers via Mixin directly onto these same vanilla classes (no
     * Java subclass), so its transmitters still pass. MekanismExtras/EvolvedMekanismExtras instead
     * define actual Java subclasses (e.g. {@code ExtraUniversalCable extends UniversalCable}) that
     * wire real upgrades through a completely separate, addon-specific mechanism, so those
     * transmitters correctly fail this check even though their block still nominally reuses one of
     * Mekanism's {@code BaseTier} constants (see the class doc above).
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

