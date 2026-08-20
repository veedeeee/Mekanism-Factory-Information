package wtf.vd.mekfactoryinfo.forge.compat.ae2;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Client-side config spec persisting the AE2 terminal Lines-grouping sort toggle across game restarts.
 * <p>
 * Registered as a {@code CLIENT} config in {@code MekFactoryInfoForge}, saved by Forge to
 * {@code config/mek_factory_info-client.toml}.
 */
public final class LinesGroupingConfig {

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue LINES_GROUPING_ENABLED;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        LINES_GROUPING_ENABLED = builder
                .comment("Whether the AE2 terminal Lines-grouping sort toggle is enabled.")
                .define("linesGroupingEnabled", false);
        SPEC = builder.build();
    }

    private LinesGroupingConfig() {
    }
}
