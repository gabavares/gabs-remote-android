package com.limelight.gabs.input;

/**
 * Gabs Remote: zoom só no tablet (a imagem da sessão é ampliada na tela; o PC não muda).
 *
 * A view é escalada pelo centro (pivô padrão). A translação é limitada para nunca mostrar
 * fora da imagem: com escala s numa view de largura w, |tx| <= (s - 1) * w / 2.
 */
public class LocalZoom {
    public static final float MIN_SCALE = 1.0f, MAX_SCALE = 4.0f;

    private float scale = 1.0f;
    private float tx, ty;
    private int lastWidth, lastHeight;

    /** Pinça: multiplica a escala mantendo o ponto de foco (em coordenadas da view) no lugar. */
    public void onScale(float factor, float focusX, float focusY) {
        float newScale = clamp(scale * factor, MIN_SCALE, MAX_SCALE);
        if (lastWidth > 0 && lastHeight > 0) {
            float cx = lastWidth / 2f, cy = lastHeight / 2f;
            tx = focusX - cx - (focusX - cx - tx) * newScale / scale;
            ty = focusY - cy - (focusY - cy - ty) * newScale / scale;
        } else {
            pendingFocusX = focusX;
            pendingFocusY = focusY;
            pendingFrom = scale;
            hasPending = true;
        }
        scale = newScale;
    }

    // Foco recebido antes de conhecer o tamanho da view (aplicado no primeiro translation()).
    private boolean hasPending;
    private float pendingFocusX, pendingFocusY, pendingFrom = 1f;

    public void onPan(float dx, float dy) {
        tx += dx;
        ty += dy;
    }

    public void reset() {
        scale = 1.0f;
        tx = ty = 0;
        hasPending = false;
    }

    public float scale() {
        return scale;
    }

    /** Translação a aplicar na view (setTranslationX/Y), já limitada ao tamanho da view. */
    public float[] translation(int viewWidth, int viewHeight) {
        lastWidth = viewWidth;
        lastHeight = viewHeight;
        if (hasPending) {
            float cx = viewWidth / 2f, cy = viewHeight / 2f;
            tx = pendingFocusX - cx - (pendingFocusX - cx - tx) * scale / pendingFrom;
            ty = pendingFocusY - cy - (pendingFocusY - cy - ty) * scale / pendingFrom;
            hasPending = false;
        }
        float maxX = (scale - 1f) * viewWidth / 2f;
        float maxY = (scale - 1f) * viewHeight / 2f;
        tx = clamp(tx, -maxX, maxX);
        ty = clamp(ty, -maxY, maxY);
        return new float[]{tx + 0f, ty + 0f};
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
