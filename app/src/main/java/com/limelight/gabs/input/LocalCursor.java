package com.limelight.gabs.input;

/**
 * Gabs Remote: cursor desenhado no próprio tablet (preferência "Cursor no tablet", grCursor=local).
 *
 * O cursor que vem dentro do vídeo chega com toda a latência da imagem. Aqui o app guarda a posição,
 * desenha o cursor na hora e manda ao PC a posição absoluta, para os dois lados concordarem.
 * A posição fica em pixels da imagem do PC (largura x altura do stream): 1 de deslocamento = 1 pixel,
 * como o mouse relativo mandava antes.
 *
 * Visibilidade, como no Windows: o cursor aparece quando a última entrada foi de mouse (trackpad na
 * tela, trackpad do teclado ou mouse) e some depois de um toque direto ou da caneta. O formato vem do
 * host; quando o host esconde o cursor (digitando, jogo), mover mostra o último formato na hora e,
 * se o host continuar escondendo por {@link #WAKE_GRACE_MS}, vale o host.
 */
public final class LocalCursor {
    /** Chave da preferência. */
    public static final String PREF = "gr_local_cursor";
    public static final boolean PREF_DEFAULT = true;

    /** Tempo em que mover mostra o cursor mesmo com o host dizendo "escondido". */
    public static final long WAKE_GRACE_MS = 500;

    /** Para onde vai a posição absoluta (NvConnection.sendMousePosition). */
    public interface Output {
        void sendPosition(int x, int y, int referenceWidth, int referenceHeight);
    }

    private final int width, height;
    private final Output output;
    private float x, y;
    private boolean mouseActive;
    private CursorShape hostShape = CursorShape.ARROW;
    private CursorShape lastVisibleShape = CursorShape.ARROW;
    private long hiddenAtMs = Long.MIN_VALUE / 2;
    private long wakeAtMs = Long.MIN_VALUE / 2;

    /**
     * @param width largura da imagem do PC (pixels do stream)
     * @param height altura da imagem do PC
     */
    public LocalCursor(int width, int height, Output output) {
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        this.output = output;
        this.x = this.width / 2f;
        this.y = this.height / 2f;
    }

    /** Valor do parâmetro grCursor do /launch e do /resume, ou null para não mandar. */
    public static String launchParam(boolean enabled) {
        return enabled ? "local" : null;
    }

    /**
     * Aceleração no lugar da do Windows ("Aumentar precisão do ponteiro"), que não age em posição
     * absoluta: devagar = 1 para 1 (precisão); rápido = até {@link #ACCEL_MAX} vezes.
     * Ganho = 0,8 + 1,2 × velocidade (pixels por ms), entre 1 e ACCEL_MAX.
     */
    static final float ACCEL_MAX = 3.5f;

    static float gain(float speedPxPerMs) {
        float g = 0.8f + 1.2f * speedPxPerMs;
        return g < 1f ? 1f : (g > ACCEL_MAX ? ACCEL_MAX : g);
    }

    private long lastMoveMs = Long.MIN_VALUE / 2;

    /** Deslocamento de mouse (pixels do PC, antes da aceleração). */
    public void moveBy(float dx, float dy, long nowMs) {
        if (dx == 0 && dy == 0) {
            return;
        }
        if (hostShape == CursorShape.HIDDEN && wakeAtMs < hiddenAtMs) {
            wakeAtMs = nowMs;
        }
        mouseActive = true;
        // Intervalo entre 4 e 100 ms: eventos no mesmo ms não viram velocidade infinita; depois de parar, começa devagar.
        long dt = Math.max(4, Math.min(100, nowMs - lastMoveMs));
        lastMoveMs = nowMs;
        float g = gain((float) Math.hypot(dx, dy) / dt);
        x = clamp(x + dx * g, width - 1);
        y = clamp(y + dy * g, height - 1);
        output.sendPosition(Math.round(x), Math.round(y), width, height);
    }

    /**
     * Toque direto ou caneta: o Windows leva o cursor ao ponto e o esconde.
     *
     * @param nx posição normalizada (0 a 1) na imagem
     * @param ny posição normalizada (0 a 1) na imagem
     */
    public void onDirectInput(float nx, float ny) {
        x = clamp(nx * (width - 1), width - 1);
        y = clamp(ny * (height - 1), height - 1);
        mouseActive = false;
    }

    /** Formato novo contado pelo host. */
    public void onHostShape(CursorShape shape, long nowMs) {
        if (shape == CursorShape.HIDDEN) {
            if (hostShape != CursorShape.HIDDEN) {
                hiddenAtMs = nowMs;
            }
        } else {
            lastVisibleShape = shape;
        }
        hostShape = shape;
    }

    public boolean visible(long nowMs) {
        if (!mouseActive) {
            return false;
        }
        return hostShape != CursorShape.HIDDEN || nowMs - wakeAtMs < WAKE_GRACE_MS;
    }

    /** Formato atual (o último visível enquanto o host esconde). */
    public CursorShape shape() {
        return hostShape == CursorShape.HIDDEN ? lastVisibleShape : hostShape;
    }

    /** Formato a desenhar, ou null para não desenhar nada. */
    public CursorShape drawnShape(long nowMs) {
        if (!visible(nowMs)) {
            return null;
        }
        CursorShape s = shape();
        switch (s) {
            case OTHER:
            case HELP:
            case HIDDEN:
                return CursorShape.ARROW;
            default:
                return s;
        }
    }

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    /** Ponta do cursor numa view de vw x vh que mostra a imagem inteira (vale depois de girar a tela). */
    public float[] viewPoint(int vw, int vh) {
        return new float[]{x * vw / width, y * vh / height};
    }

    private static float clamp(float v, float max) {
        return v < 0 ? 0 : (v > max ? max : v);
    }
}
