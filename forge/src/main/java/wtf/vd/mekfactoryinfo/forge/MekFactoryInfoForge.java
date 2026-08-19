package wtf.vd.mekfactoryinfo.forge;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import wtf.vd.mekfactoryinfo.MekFactoryInfo;
import wtf.vd.mekfactoryinfo.forge.compat.ae2.LinesGroupingConfig;

@Mod(MekFactoryInfo.MOD_ID)
public class MekFactoryInfoForge {

    public MekFactoryInfoForge(IEventBus ignoredEventBus) {
        MekFactoryInfo.init();
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, LinesGroupingConfig.SPEC);
    }
}
