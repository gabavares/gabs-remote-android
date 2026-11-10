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

    /** Preferência "Velocidade do cursor", em %: 100 = 1 para 1 (o cursor anda o mesmo que o dedo na tela). */
    public static final String PREF_SPEED = "gr_cursor_speed";
    public static final int SPEED_DEFAULT_PERCENT = 100;
    static final int SPEED_MIN_PERCENT = 25;
    static final int SPEED_MAX_PERCENT = 300;

    public static float speedFromPercent(int percent) {
        int p = Math.max(SPEED_MIN_PERCENT, Math.min(SPEED_MAX_PERCENT, percent));
        return p / 100f;
    }

    private float speed = 1f;

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    /**
     * Deslocamento de mouse em pixels do PC (mouse ou trackpad do teclado), vezes a velocidade.
     * Sem aceleração: o movimento é linear, sem depender do tempo entre os eventos.
     */
    public void moveBy(float dx, float dy, long nowMs) {
        if (dx == 0 && dy == 0) {
            return;
        }
        if (hostShape == CursorShape.HIDDEN && wakeAtMs < hiddenAtMs) {
            wakeAtMs = nowMs;
        }
        mouseActive = true;
        x = clamp(x + dx * speed, width - 1);
        y = clamp(y + dy * speed, height - 1);
        output.sendPosition(Math.round(x), Math.round(y), width, height);
    }

    /**
     * Deslocamento do dedo no trackpad da tela, em pixels da tela. A imagem do PC aparece com
     * shownWidth x shownHeight pixels de tela (zoom incluído): com velocidade 100%, o cursor anda na
     * tela o mesmo que o dedo.
     */
    public void moveByScreen(float dxPx, float dyPx, float shownWidth, float shownHeight, long nowMs) {
        if (shownWidth <= 0 || shownHeight <= 0) {
            return;
        }
        moveBy(dxPx * width / shownWidth, dyPx * height / shownHeight, nowMs);
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

    /**
     * Mouse ou trackpad do teclado sem captura: o Android move e desenha o ponteiro dele
     * ({@link NativePointer}); aqui só acompanha a posição (o trackpad da tela continua dali) e
     * acorda o formato se o host estava escondendo o cursor.
     *
     * @param nx posição normalizada (0 a 1) na imagem
     * @param ny posição normalizada (0 a 1) na imagem
     */
    public void onNativePointer(float nx, float ny, long nowMs) {
        onDirectInput(nx, ny);
        if (hostShape == CursorShape.HIDDEN && wakeAtMs < hiddenAtMs) {
            wakeAtMs = nowMs;
        }
    }

    /** Formato do ponteiro nativo: o do host, ou o último visível durante a carência depois de mexer. */
    public CursorShape nativeShape(long nowMs) {
        if (hostShape == CursorShape.HIDDEN && nowMs - wakeAtMs < WAKE_GRACE_MS) {
            return lastVisibleShape;
        }
        return hostShape;
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
