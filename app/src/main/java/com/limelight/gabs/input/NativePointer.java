package com.limelight.gabs.input;

import android.view.PointerIcon;

/**
 * Gabs Remote: ponteiro do próprio Android para o mouse e o trackpad do teclado (cursor no tablet).
 *
 * Sem captura, o Android move e desenha o ponteiro numa camada própria, com a velocidade e a
 * aceleração das configurações do tablet: é o mesmo ponteiro de fora do app. O app só troca o ícone
 * conforme o formato do cursor do Windows e manda a posição ao PC.
 */
public final class NativePointer {
    private NativePointer() {
    }

    /** Ícone do sistema (PointerIcon.TYPE_*) para o formato do cursor do Windows. */
    public static int iconType(CursorShape shape) {
        switch (shape) {
            case HIDDEN: return PointerIcon.TYPE_NULL;
            case IBEAM: return PointerIcon.TYPE_TEXT;
            case HAND: return PointerIcon.TYPE_HAND;
            case WAIT:
            case APP_STARTING: return PointerIcon.TYPE_WAIT;
            case CROSS: return PointerIcon.TYPE_CROSSHAIR;
            case SIZE_NS: return PointerIcon.TYPE_VERTICAL_DOUBLE_ARROW;
            case SIZE_WE: return PointerIcon.TYPE_HORIZONTAL_DOUBLE_ARROW;
            case SIZE_NWSE: return PointerIcon.TYPE_TOP_LEFT_DIAGONAL_DOUBLE_ARROW;
            case SIZE_NESW: return PointerIcon.TYPE_TOP_RIGHT_DIAGONAL_DOUBLE_ARROW;
            case SIZE_ALL: return PointerIcon.TYPE_ALL_SCROLL;
            case NO: return PointerIcon.TYPE_NO_DROP;
            case HELP: return PointerIcon.TYPE_HELP;
            case ARROW:
            case OTHER:
            default:
                return PointerIcon.TYPE_ARROW;
        }
    }

    /**
     * Rolagem com dois dedos no trackpad sem captura (Android 14+: o sistema manda um "dedo falso"
     * com CLASSIFICATION_TWO_FINGER_SWIPE). O conteúdo segue os dedos, como no trackpad do Windows.
     * Saída em unidades de alta resolução (120 = um dente da roda); positivo = para cima / direita.
     */
    public static final class SwipeScroll {
        /** Unidades de rolagem por pixel de deslocamento dos dedos (40 px = um dente). */
        static final float GAIN = 3f;

        private float lastX, lastY, restV, restH;

        public void start(float x, float y) {
            lastX = x;
            lastY = y;
            restV = 0;
            restH = 0;
        }

        /** @return {vertical, horizontal} a mandar (zeros se ainda não somou uma unidade) */
        public int[] move(float x, float y) {
            restV += (y - lastY) * GAIN;
            restH += -(x - lastX) * GAIN;
            lastX = x;
            lastY = y;
            int v = (int) restV, h = (int) restH;
            restV -= v;
            restH -= h;
            return new int[]{v, h};
        }
    }
}
