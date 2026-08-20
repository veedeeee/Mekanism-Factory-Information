package wtf.vd.mekfactoryinfo.forge.compat.jade;

import mekanism.common.block.prefab.BlockTile;
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
        // BlockTile is the common ancestor for Machine/Factory blocks (both Mekanism's own and addon
        // equivalents like MekanismExtras' BlockExtraFactoryMachine, a BlockFactoryMachine sibling)
        // as well as Tanks/Energy Cubes. FactoryLinesHelper guards against showing a bogus "Lines: 1"
        // on the latter (see FactoryLinesHelper#hasAnyTierAttribute).
        registration.registerBlockComponent(FactoryLinesProvider.INSTANCE, BlockTile.class);
        registration.registerBlockComponent(TankSpecProvider.INSTANCE, BlockTileModel.class);
        registration.registerBlockComponent(CableSpecProvider.INSTANCE, BlockTransmitter.class);
    }
}
