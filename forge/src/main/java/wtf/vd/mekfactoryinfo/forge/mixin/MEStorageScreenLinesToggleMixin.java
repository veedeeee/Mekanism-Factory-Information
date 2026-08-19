package wtf.vd.mekfactoryinfo.forge.mixin;

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
import wtf.vd.mekfactoryinfo.forge.compat.ae2.LinesGroupingClientState;

/**
 * Adds the client-side-only Lines-grouping sort toggle button to AE2's terminal toolbar.
 */
@Pseudo
@Mixin(targets = "appeng.client.gui.me.common.MEStorageScreen", remap = false)
public abstract class MEStorageScreenLinesToggleMixin {

    @Shadow
    @Final
    protected Repo repo;

    @Unique
    private static final ResourceLocation SORTED_ICON =
            new ResourceLocation(MekFactoryInfo.MOD_ID, "textures/gui/ae2-terminal-button-sorted.png");

    @Unique
    private static final ResourceLocation UNSORTED_ICON =
            new ResourceLocation(MekFactoryInfo.MOD_ID, "textures/gui/ae2-terminal-button-unsorted.png");

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

    @Unique
    private static final class LinesGroupingButton extends Button {

        private LinesGroupingButton(OnPress onPress) {
            super(0, 0, 16, 16, Component.empty(), onPress, DEFAULT_NARRATION);
            this.setTooltip(Tooltip.create(mekfactoryinfo$tooltipMessage()));
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int yOffset = this.isHovered() ? 1 : 0;
            // AE2 1.20.1 has no TOOLBAR_BUTTON_BACKGROUND_HOVER; use the base background for all states.
            Icon.TOOLBAR_BUTTON_BACKGROUND.getBlitter()
                    .dest(this.getX() - 1, this.getY() + yOffset, 18, 20)
                    .blit(guiGraphics);

            ResourceLocation icon = LinesGroupingClientState.isEnabled() ? SORTED_ICON : UNSORTED_ICON;
            Blitter.texture(icon, 16, 16)
                    .dest(this.getX(), this.getY() + 1 + yOffset, 16, 16)
                    .blit(guiGraphics);
        }
    }
}
