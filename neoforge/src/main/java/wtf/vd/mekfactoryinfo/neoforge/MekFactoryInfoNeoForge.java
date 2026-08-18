package wtf.vd.mekfactoryinfo.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import wtf.vd.mekfactoryinfo.MekFactoryInfo;

@Mod(MekFactoryInfo.MOD_ID)
public class MekFactoryInfoNeoForge {

    public MekFactoryInfoNeoForge(IEventBus ignoredEventBus) {
        MekFactoryInfo.init();
    }
}
