package com.limelight.gabs.input;

/**
 * Gabs Remote: onde o menu da sessão aparece.
 * O menu desce de uma âncora; ancorado na tela da sessão (que ocupa a tela toda) ele ia parar fora da tela.
 * A âncora fica no centro dos dedos do toque, sem sair da tela.
 */
public final class MenuAnchor {
    private MenuAnchor() {
    }

    /**
     * @return {x, y} da âncora, em pixels da tela.
     */
    public static float[] place(float[] xs, float[] ys, int width, int height) {
        int n = Math.min(xs.length, ys.length);
        if (n == 0) {
            return new float[]{width / 2f, height / 2f};
        }
        float x = 0, y = 0;
        for (int i = 0; i < n; i++) {
            x += xs[i];
            y += ys[i];
        }
        x /= n;
        y /= n;
        return new float[]{clamp(x, width), clamp(y, height)};
    }

    private static float clamp(float v, int size) {
        return Math.max(0, Math.min(size - 1, v));
    }
}
