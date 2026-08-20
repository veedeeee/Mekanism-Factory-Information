package wtf.vd.mekfactoryinfo.forge.mixin;

import appeng.client.gui.AEBaseScreen;
import net.minecraft.client.gui.components.Button;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes AE2's protected {@code AEBaseScreen#addToLeftToolbar}.
 */
@Pseudo
@Mixin(targets = "appeng.client.gui.AEBaseScreen", remap = false)
public interface AEBaseScreenInvoker {

    @Invoker("addToLeftToolbar")
    <B extends Button> B mekfactoryinfo$addToLeftToolbar(B button);
}
