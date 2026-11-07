package com.limelight.gabs.input;

/**
 * Gabs Remote: modo de toque da sessão.
 * DIRETO = toque nativo do Windows; TRACKPAD = cursor relativo do upstream;
 * SO_CANETA = dedo não mexe no PC (só gestos locais de zoom, rolagem e clique direito).
 */
public enum TouchMode {
    DIRETO, TRACKPAD, SO_CANETA;

    /** Chave da preferência. */
    public static final String PREF = "gr_touch_mode";

    public static TouchMode fromPreference(String value) {
        if (value != null) {
            for (TouchMode m : values()) {
                if (m.name().equals(value)) {
                    return m;
                }
            }
        }
        return DIRETO;
    }
}
