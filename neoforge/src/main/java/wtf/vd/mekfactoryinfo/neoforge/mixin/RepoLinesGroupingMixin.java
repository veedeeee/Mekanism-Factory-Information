package wtf.vd.mekfactoryinfo.neoforge.mixin;

import appeng.api.config.SortDir;
import appeng.api.config.SortOrder;
import appeng.api.stacks.AEKey;
import java.util.Comparator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wtf.vd.mekfactoryinfo.neoforge.compat.ae2.FactoryLinesGroupingComparators;
import wtf.vd.mekfactoryinfo.neoforge.compat.ae2.LinesGroupingClientState;

/**
 * Applies the Lines-grouping sort toggle on top of AE2's own NAME/MOD key comparator.
 * <p>
 * AE2 is an optional dependency for this mod (see {@code neoforge.mods.toml}), so this targets AE2's
 * class by name ({@code @Pseudo}) rather than a compiled class reference, so Mixin silently skips
 * applying it when AE2 is not installed instead of erroring.
 */
@Pseudo
@Mixin(targets = "appeng.client.gui.me.common.Repo", remap = false)
public abstract class RepoLinesGroupingMixin {

    @Inject(method = "getKeyComparator", at = @At("RETURN"), cancellable = true, require = 0)
    private void mekfactoryinfo$applyLinesGrouping(SortOrder sortBy, SortDir sortDir,
            CallbackInfoReturnable<Comparator<AEKey>> cir) {
        if (!LinesGroupingClientState.isEnabled()) {
            return;
        }
        if (sortBy != SortOrder.NAME && sortBy != SortOrder.MOD) {
            return;
        }
        cir.setReturnValue(FactoryLinesGroupingComparators.wrap(cir.getReturnValue(), sortBy, sortDir));
    }
}

