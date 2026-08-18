package wtf.vd.mekfactoryinfo.neoforge.compat.ae2;

/**
 * Purely client-side, in-memory preference for the AE2 terminal Lines-grouping sort toggle.
 * <p>
 * Unlike AE2's own {@code Settings} (e.g. {@code SORT_BY}), this is never synced to or persisted by
 * the server: it only changes how items are ordered within the client's own terminal view, so a
 * simple local flag is sufficient and avoids needing any networking or config-sync plumbing.
 */
public final class LinesGroupingClientState {

    private static boolean enabled = false;

    private LinesGroupingClientState() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static void toggle() {
        enabled = !enabled;
    }
}

