package wtf.vd.mekfactoryinfo.neoforge.compat.jade;

import mekanism.common.block.prefab.BlockFactoryMachine;
import mekanism.common.block.prefab.BlockTile.BlockTileModel;
import mekanism.common.block.transmitter.BlockTransmitter;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Entry point discovered by Jade's plugin scanner. Registers:
 * <ul>
 *   <li>{@link FactoryLinesProvider} for every Mekanism-family Factory block and regular
 *       (upgradeable-to-Factory) machine block (see {@link BlockFactoryMachine}).</li>
 *   <li>{@link TankSpecProvider} for {@link BlockTileModel}, Mekanism's generic tiered-machine block
 *       prefab (covers Fluid/Chemical Tanks among many other machine types); the provider itself
 *       filters internally for tanks and is a no-op for anything else.</li>
 *   <li>{@link CableSpecProvider} for {@link BlockTransmitter}, covering all transmitter types
 *       (Universal Cables, Mechanical Pipes, Pressurized Tubes, etc.); likewise filters internally
 *       for cables only.</li>
 * </ul>
 */
@WailaPlugin
public final class MekFactoryInfoJadePlugin implements IWailaPlugin {

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        // BlockFactoryMachine covers both actual multi-Line Factory blocks and the regular
        // (implicitly single-Line) Mekanism machines that a Tier Installer can upgrade into one.
        registration.registerBlockComponent(FactoryLinesProvider.INSTANCE, BlockFactoryMachine.class);
        registration.registerBlockComponent(TankSpecProvider.INSTANCE, BlockTileModel.class);
        registration.registerBlockComponent(CableSpecProvider.INSTANCE, BlockTransmitter.class);
    }
}
