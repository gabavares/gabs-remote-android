package com.limelight.gabs.input;

/** Layout de teclado do Windows durante a sessão (parâmetro grKeyboard do /launch). */
public final class KeyboardLayout {
    /** Chave da preferência ("0" = não mudar). */
    public static final String PREF_KEY = "gr_keyboard_layout";
    /** Português (Brasil) ABNT2, o teclado do tablet. */
    public static final String DEFAULT = "00010416";

    private KeyboardLayout() {
    }

    /**
     * Valor da preferência a usar: "0" (não mudar) ou um KLID; sem valor, ou com um valor que não é
     * nenhum dos dois, o padrão. A tela de configurações já gravou "10416" (o XML compilava o padrão
     * como número), e com isso o app deixava de pedir o português.
     */
    public static String fromPreference(String stored) {
        if (stored != null && (stored.equals("0") || launchParam(stored) != null)) {
            return stored;
        }
        return DEFAULT;
    }

    /** Valor do parâmetro grKeyboard, ou null para não mandar. */
    public static String launchParam(String prefValue) {
        if (prefValue == null || !prefValue.matches("[0-9A-Fa-f]{8}")) {
            return null;
        }
        return prefValue.toUpperCase(java.util.Locale.ROOT);
    }
}
