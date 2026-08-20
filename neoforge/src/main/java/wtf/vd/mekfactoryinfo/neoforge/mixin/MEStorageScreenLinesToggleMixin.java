package wtf.vd.mekfactoryinfo.neoforge.mixin;

import appeng.client.gui.Icon;
import appeng.client.gui.me.common.Repo;
import appeng.client.gui.style.Blitter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.vd.mekfactoryinfo.MekFactoryInfo;
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
    private static final ResourceLocation SORTED_ICON =
            ResourceLocation.fromNamespaceAndPath(MekFactoryInfo.MOD_ID, "textures/gui/ae2-terminal-button-sorted.png");

    @Unique
    private static final ResourceLocation UNSORTED_ICON =
            ResourceLocation.fromNamespaceAndPath(MekFactoryInfo.MOD_ID, "textures/gui/ae2-terminal-button-unsorted.png");

    @Unique
    private LinesGroupingButton mekfactoryinfo$linesGroupingButton;

    @Inject(method = "<init>", at = @At("TAIL"), require = 0)
    private void mekfactoryinfo$addLinesGroupingToggle(CallbackInfo ci) {
        var self = (AEBaseScreenInvoker) (Object) this;
        this.mekfactoryinfo$linesGroupingButton = self.mekfactoryinfo$addToLeftToolbar(
                new LinesGroupingButton(btn -> mekfactoryinfo$onPress()));
    }

    @Unique
    private void mekfactoryinfo$onPress() {
        LinesGroupingClientState.toggle();
        this.mekfactoryinfo$linesGroupingButton.setTooltip(Tooltip.create(mekfactoryinfo$tooltipMessage()));
        this.repo.updateView();
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

    /**
     * A 16x16 icon-only toolbar button that reproduces AE2's own {@code IconButton} rendering convention
     * (background sprite from AE2's toolbar-button atlas, with a custom 16x16 icon layered on top),
     * without depending on AE2's {@code IconButton} class itself since our icon isn't part of AE2's
     * fixed {@code Icon} enum.
     */
    @Unique
    private static final class LinesGroupingButton extends Button {

        private LinesGroupingButton(OnPress onPress) {
            super(0, 0, 16, 16, Component.empty(), onPress, DEFAULT_NARRATION);
            this.setTooltip(Tooltip.create(mekfactoryinfo$tooltipMessage()));
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int yOffset = this.isHovered() ? 1 : 0;
            Icon backgroundIcon = this.isHovered()
                    ? Icon.TOOLBAR_BUTTON_BACKGROUND_HOVER
                    : this.isFocused() ? Icon.TOOLBAR_BUTTON_BACKGROUND_FOCUS : Icon.TOOLBAR_BUTTON_BACKGROUND;
            backgroundIcon.getBlitter()
                    .dest(this.getX() - 1, this.getY() + yOffset, 18, 20)
                    .zOffset(2)
                    .blit(guiGraphics);

            ResourceLocation icon = LinesGroupingClientState.isEnabled() ? SORTED_ICON : UNSORTED_ICON;
            Blitter.texture(icon, 16, 16)
                    .dest(this.getX(), this.getY() + 1 + yOffset, 16, 16)
                    .zOffset(3)
                    .blit(guiGraphics);
        }
    }
}


