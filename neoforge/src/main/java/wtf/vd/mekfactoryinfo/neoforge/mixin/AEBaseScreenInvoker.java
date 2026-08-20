package wtf.vd.mekfactoryinfo.neoforge.mixin;

import appeng.client.gui.AEBaseScreen;
import net.minecraft.client.gui.components.Button;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes AE2's protected {@code AEBaseScreen#addToLeftToolbar} so the Lines-grouping toggle button
 * mixin can add its button without needing to literally extend {@code AEBaseScreen} itself.
 * <p>
 * AE2 is an optional dependency for this mod (see {@code neoforge.mods.toml}), so this targets AE2's
 * class by name ({@code @Pseudo}) rather than a compiled class reference, so Mixin silently skips
 * applying it when AE2 is not installed instead of erroring.
 */
@Pseudo
@Mixin(targets = "appeng.client.gui.AEBaseScreen", remap = false)
public interface AEBaseScreenInvoker {

    @Invoker("addToLeftToolbar")
    <B extends Button> B mekfactoryinfo$addToLeftToolbar(B button);
}
