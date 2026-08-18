package wtf.vd.mekfactoryinfo.neoforge.compat.jade;

import mekanism.common.block.prefab.BlockFactoryMachine;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Entry point discovered by Jade's plugin scanner. Registers the Factory Lines tooltip provider for
 * every Mekanism-family Factory block and regular (upgradeable-to-Factory) machine block
 * (see {@link BlockFactoryMachine}).
 */
@WailaPlugin
public final class MekFactoryInfoJadePlugin implements IWailaPlugin {

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        // BlockFactoryMachine covers both actual multi-Line Factory blocks and the regular
        // (implicitly single-Line) Mekanism machines that a Tier Installer can upgrade into one.
        registration.registerBlockComponent(FactoryLinesProvider.INSTANCE, BlockFactoryMachine.class);
    }
}
