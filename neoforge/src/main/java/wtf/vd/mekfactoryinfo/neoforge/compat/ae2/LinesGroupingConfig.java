package wtf.vd.mekfactoryinfo.neoforge.compat.ae2;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client-side config spec persisting the AE2 terminal Lines-grouping sort toggle across game restarts.
 * <p>
 * Registered as a {@code CLIENT} config in {@code MekFactoryInfoNeoForge}, saved by NeoForge to
 * {@code config/mek_factory_info-client.toml}.
 */
public final class LinesGroupingConfig {

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue LINES_GROUPING_ENABLED;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        LINES_GROUPING_ENABLED = builder
                .comment("Whether the AE2 terminal Lines-grouping sort toggle is enabled.")
                .define("linesGroupingEnabled", false);
        SPEC = builder.build();
    }

    private LinesGroupingConfig() {
    }
}
