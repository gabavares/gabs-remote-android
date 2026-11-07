package com.limelight.gabs.input;

/**
 * Gabs Remote: corrige a rotação do trackpad do teclado físico do tablet (Book Cover).
 *
 * No Tab S10+ em paisagem, o trackpad capturado chega girado 90° (arrastar para a esquerda
 * move o cursor para cima). A rotação é aplicada no sentido horário: 90° leva (x, y) a (y, -x).
 */
public final class TrackpadRotation {
    public static final String PREF = "gr_trackpad_rotation";
    public static final int DEFAULT = 90;

    private TrackpadRotation() {
    }

    public static float[] rotate(int degrees, float dx, float dy) {
        switch (degrees) {
            case 90:
                return new float[]{dy, -dx};
            case 180:
                return new float[]{-dx, -dy};
            case 270:
                return new float[]{-dy, dx};
            default:
                return new float[]{dx, dy};
        }
    }

    public static int fromPreference(String value) {
        if (value != null) {
            switch (value) {
                case "0": return 0;
                case "90": return 90;
                case "180": return 180;
                case "270": return 270;
                default: break;
            }
        }
        return DEFAULT;
    }
}
