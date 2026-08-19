package wtf.vd.mekfactoryinfo.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import wtf.vd.mekfactoryinfo.MekFactoryInfo;
import wtf.vd.mekfactoryinfo.neoforge.compat.ae2.LinesGroupingConfig;

@Mod(MekFactoryInfo.MOD_ID)
public class MekFactoryInfoNeoForge {

    public MekFactoryInfoNeoForge(IEventBus ignoredEventBus, ModContainer container) {
        MekFactoryInfo.init();
        container.registerConfig(ModConfig.Type.CLIENT, LinesGroupingConfig.SPEC);
    }
}
