package wtf.vd.mekfactoryinfo.neoforge.mixin;

import appeng.client.gui.me.common.Repo;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.vd.mekfactoryinfo.neoforge.compat.ae2.LinesGroupingClientState;

/**
 * Adds the client-side-only Lines-grouping sort toggle button to AE2's terminal toolbar.
 * <p>
 * AE2 is an optional dependency for this mod (see {@code neoforge.mods.toml}), so this targets AE2's
 * class by name ({@code @Pseudo}) rather than a compiled class reference, so Mixin silently skips
 * applying it when AE2 is not installed instead of erroring.
 */
@Pseudo
@Mixin(targets = "appeng.client.gui.me.common.MEStorageScreen", remap = false)
public abstract class MEStorageScreenLinesToggleMixin {

    @Shadow
    @Final
    protected Repo repo;

    @Unique
    private Button mekfactoryinfo$linesGroupingButton;

    @Inject(method = "<init>", at = @At("TAIL"), require = 0)
    private void mekfactoryinfo$addLinesGroupingToggle(CallbackInfo ci) {
        var self = (AEBaseScreenInvoker) (Object) this;
        this.mekfactoryinfo$linesGroupingButton = self.mekfactoryinfo$addToLeftToolbar(Button.builder(
                        mekfactoryinfo$buttonLabel(), btn -> mekfactoryinfo$onPress())
                .tooltip(Tooltip.create(mekfactoryinfo$tooltipMessage()))
                .bounds(0, 0, 16, 16)
                .build());
    }

    @Unique
    private void mekfactoryinfo$onPress() {
        LinesGroupingClientState.toggle();
        this.mekfactoryinfo$linesGroupingButton.setMessage(mekfactoryinfo$buttonLabel());
        this.mekfactoryinfo$linesGroupingButton.setTooltip(Tooltip.create(mekfactoryinfo$tooltipMessage()));
        this.repo.updateView();
    }

    @Unique
    private static Component mekfactoryinfo$buttonLabel() {
        String key = LinesGroupingClientState.isEnabled()
                ? "gui.mek_factory_info.lines_grouping_toggle.on"
                : "gui.mek_factory_info.lines_grouping_toggle.off";
        return Component.translatable(key);
    }

    /**
     * Builds the two-line tooltip Component, mirroring AE2's own toggle-button tooltip convention -
     * a white title followed by a gray description line (see {@code appeng.client.gui.Tooltip}).
     */
    @Unique
    private static Component mekfactoryinfo$tooltipMessage() {
        String descriptionKey = LinesGroupingClientState.isEnabled()
                ? "gui.mek_factory_info.lines_grouping_toggle.enabled"
                : "gui.mek_factory_info.lines_grouping_toggle.disabled";
        return Component.translatable("gui.mek_factory_info.lines_grouping_toggle.title")
                .withStyle(ChatFormatting.WHITE)
                .append("\n")
                .append(Component.translatable(descriptionKey).withStyle(ChatFormatting.GRAY));
    }
}


