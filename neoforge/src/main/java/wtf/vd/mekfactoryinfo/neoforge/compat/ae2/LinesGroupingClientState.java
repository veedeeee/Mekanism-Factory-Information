package wtf.vd.mekfactoryinfo.neoforge.compat.ae2;

/**
 * Purely client-side preference for the AE2 terminal Lines-grouping sort toggle, persisted via
 * {@link LinesGroupingConfig} so it survives game restarts (not just world/session changes).
 * <p>
 * Unlike AE2's own {@code Settings} (e.g. {@code SORT_BY}), this is never synced to or persisted by
 * the server: it only changes how items are ordered within the client's own terminal view, so a local
 * client config value is sufficient and avoids needing any networking or server-side config-sync plumbing.
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

