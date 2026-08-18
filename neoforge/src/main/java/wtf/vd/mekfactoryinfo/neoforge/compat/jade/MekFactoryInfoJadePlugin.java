package wtf.vd.mekfactoryinfo.neoforge.compat.jade;

import mekanism.common.block.prefab.BlockFactoryMachine;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Entry point discovered by Jade's plugin scanner. Registers the Factory Lines tooltip provider for
 * every Mekanism-family Factory block (see {@link BlockFactoryMachine.BlockFactory}).
 */
@WailaPlugin
public final class MekFactoryInfoJadePlugin implements IWailaPlugin {

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(FactoryLinesProvider.INSTANCE, BlockFactoryMachine.BlockFactory.class);
    }
}
