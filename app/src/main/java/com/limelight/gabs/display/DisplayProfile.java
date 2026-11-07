package com.limelight.gabs.display;

/**
 * Gabs Remote: perfil de tela pedido ao host com o parâmetro grDisplay do /launch.
 *
 * NATIVO cria no PC uma tela virtual na resolução do tablet (com escala 100%, 2/3 ou 1/2);
 * FIXO_1080 e FIXO_720 criam a tela virtual nessas resoluções; ESPELHAR transmite o monitor
 * físico do PC (2560x1440) sem mexer nele.
 */
public enum DisplayProfile {
    NATIVO, ESPELHAR, FIXO_1080, FIXO_720;

    /** Chave da preferência do perfil. */
    public static final String PREF_PROFILE = "gr_display_profile";
    /** Chave da preferência da escala do perfil nativo (100, 67 ou 50). */
    public static final String PREF_NATIVE_SCALE = "gr_native_scale";

    /** Resolução do monitor físico do PC, usada no Espelhar. */
    static final int MIRROR_WIDTH = 2560, MIRROR_HEIGHT = 1440;

    /** Chave da preferência da escala do Windows na tela virtual ("0" = não mudar). */
    public static final String PREF_WINDOWS_SCALE = "gr_windows_scale";

    /** Valor do parâmetro grScale, ou null para não mandar (Espelhar ou "não mudar"). */
    public String windowsScaleParam(String prefValue) {
        if (this == ESPELHAR || prefValue == null) {
            return null;
        }
        switch (prefValue) {
            case "100": case "125": case "150": case "175": case "200":
                return prefValue;
            default:
                return null;
        }
    }

    /** Valor do parâmetro grDisplay do /launch e do /resume. */
    public String launchParam() {
        return this == ESPELHAR ? "mirror" : "virtual";
    }

    /** Lê o valor gravado na preferência; qualquer coisa desconhecida vira NATIVO. */
    public static DisplayProfile fromPreference(String value) {
        if (value != null) {
            for (DisplayProfile p : values()) {
                if (p.name().equals(value)) {
                    return p;
                }
            }
        }
        return NATIVO;
    }

    /**
     * Resolução da sessão para o perfil, sempre em paisagem.
     *
     * @param scalePercent escala do nativo: 100, 67 (2/3 exatos) ou 50
     * @param nativeWidth  largura física da tela do tablet (em qualquer orientação)
     * @param nativeHeight altura física da tela do tablet
     * @return {largura, altura}, ambas pares
     */
    public static int[] resolve(DisplayProfile p, int scalePercent, int nativeWidth, int nativeHeight) {
        switch (p) {
            case ESPELHAR:
                return new int[]{MIRROR_WIDTH, MIRROR_HEIGHT};
            case FIXO_1080:
                return new int[]{1920, 1080};
            case FIXO_720:
                return new int[]{1280, 720};
            case NATIVO:
            default:
                int longSide = Math.max(nativeWidth, nativeHeight);
                int shortSide = Math.min(nativeWidth, nativeHeight);
                return new int[]{scaleDownToEven(longSide, scalePercent), scaleDownToEven(shortSide, scalePercent)};
        }
    }

    private static int scaleDownToEven(int size, int scalePercent) {
        int scaled;
        if (scalePercent == 67) {
            scaled = size * 2 / 3;
        } else if (scalePercent == 50) {
            scaled = size / 2;
        } else {
            scaled = size;
        }
        return scaled & ~1;
    }
}
