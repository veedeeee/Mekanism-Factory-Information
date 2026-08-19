package wtf.vd.mekfactoryinfo.forge.compat.ae2;

/**
 * Purely client-side preference for the AE2 terminal Lines-grouping sort toggle, persisted via
 * {@link LinesGroupingConfig} so it survives game restarts (not just world/session changes).
 */
public final class LinesGroupingClientState {

    private LinesGroupingClientState() {
    }

    public static boolean isEnabled() {
        return LinesGroupingConfig.LINES_GROUPING_ENABLED.get();
    }

    public static void setEnabled(boolean value) {
        LinesGroupingConfig.LINES_GROUPING_ENABLED.set(value);
        LinesGroupingConfig.LINES_GROUPING_ENABLED.save();
    }

    public static void toggle() {
        setEnabled(!isEnabled());
    }
}
