package com.limelight.gabs.input;

/** Layout de teclado do Windows durante a sessão (parâmetro grKeyboard do /launch). */
public final class KeyboardLayout {
    /** Chave da preferência ("0" = não mudar). */
    public static final String PREF_KEY = "gr_keyboard_layout";
    /** Português (Brasil) ABNT2, o teclado do tablet. */
    public static final String DEFAULT = "00010416";

    private KeyboardLayout() {
    }

    /** Valor do parâmetro grKeyboard, ou null para não mandar. */
    public static String launchParam(String prefValue) {
        if (prefValue == null || !prefValue.matches("[0-9A-Fa-f]{8}")) {
            return null;
        }
        return prefValue.toUpperCase(java.util.Locale.ROOT);
    }
}
