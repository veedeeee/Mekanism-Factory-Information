package wtf.vd.mekfactoryinfo.forge.compat.jade;

import mekanism.common.block.prefab.BlockFactoryMachine;
import mekanism.common.block.prefab.BlockTile.BlockTileModel;
import mekanism.common.block.transmitter.BlockTransmitter;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Entry point discovered by Jade's plugin scanner.
 */
@WailaPlugin
public final class MekFactoryInfoJadePlugin implements IWailaPlugin {

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(FactoryLinesProvider.INSTANCE, BlockFactoryMachine.class);
        registration.registerBlockComponent(TankSpecProvider.INSTANCE, BlockTileModel.class);
        registration.registerBlockComponent(CableSpecProvider.INSTANCE, BlockTransmitter.class);
    }
}
