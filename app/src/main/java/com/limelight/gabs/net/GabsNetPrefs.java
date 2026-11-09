package com.limelight.gabs.net;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/** Gabs Remote: preferências da categoria "Acesso de fora". */
public final class GabsNetPrefs {
    public static final String PRESENCE_URL = "gr_presence_url";
    public static final String PRESENCE_TOKEN = "gr_presence_token";
    public static final String FORCE_PATH = "gr_force_path";
    public static final String DEFAULT_PRESENCE_URL = "https://remote.gabtavares.com/presenca.php";

    /** Quanto o poller espera pelos outros caminhos depois da primeira resposta. */
    public static final long GRACE_MS = 800;

    private GabsNetPrefs() {
    }

    private static SharedPreferences prefs(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c);
    }

    /** @return o caminho forçado, ou null para escolher sozinho */
    public static PathKind forcedPath(Context c) {
        String v = prefs(c).getString(FORCE_PATH, "AUTO");
        try {
            return "AUTO".equals(v) ? null : PathKind.valueOf(v);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static String presenceUrl(Context c) {
        String v = prefs(c).getString(PRESENCE_URL, DEFAULT_PRESENCE_URL);
        return v == null || v.trim().isEmpty() ? DEFAULT_PRESENCE_URL : v.trim();
    }

    public static String presenceToken(Context c) {
        String v = prefs(c).getString(PRESENCE_TOKEN, "");
        return v == null ? "" : v.trim();
    }
}
