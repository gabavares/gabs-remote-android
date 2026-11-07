package com.limelight.gabs.input;

import java.util.HashMap;
import java.util.Map;

/**
 * Gabs Remote: gestos do trackpad do teclado do tablet (Book Cover) com o ponteiro capturado.
 *
 * Capturado, o Android entrega os dedos crus do trackpad, sem os gestos que ele mesmo faria;
 * o upstream só usava o deslocamento do primeiro dedo. Aqui, como num trackpad do Windows:
 * um dedo move o cursor; dois dedos rolam; toque rápido = clique, toque de dois dedos = clique
 * direito; clique físico com dois dedos apoiados = clique direito; com o botão apertado, o cursor
 * segue o dedo que mais se move (arrastar com o polegar no botão e o indicador movendo).
 *
 * Coordenadas em unidades do trackpad (15 por mm no Tab S10+); a rotação vem de {@link TrackpadRotation}.
 */
public final class TouchpadGestures {
    public static final int LEFT = 1, RIGHT = 3;

    /** O que mandar ao PC. */
    public interface Output {
        void move(int dx, int dy);

        /** Rolagem em unidades de alta resolução (120 = um dente da roda); positivo = para cima / direita. */
        void scroll(int vertical, int horizontal);

        void button(int which, boolean down);
    }

    // Valores de MotionEvent (sem depender do Android, para testar na JVM).
    private static final int ACTION_DOWN = 0, ACTION_UP = 1, ACTION_MOVE = 2, ACTION_CANCEL = 3,
            ACTION_POINTER_DOWN = 5, ACTION_POINTER_UP = 6;
    private static final int BUTTON_PRIMARY = 1, BUTTON_SECONDARY = 2;

    static final long TAP_MAX_MS = 250;
    static final float TAP_SLOP = 45;       // 3 mm
    static final float SCROLL_GAIN = 1.2f;  // 10 mm de dedo ≈ 1,5 dente da roda

    private final int rotation;
    private final Output output;
    private final Map<Integer, float[]> last = new HashMap<>();
    private long gestureStart;
    private int maxFingers;
    private float travel;
    private boolean clicked;
    private int pressedButton;  // botão que mandamos apertado (0 = nenhum)
    private int lastButtons;
    private float scrollRestV, scrollRestH;

    public TouchpadGestures(int rotation, Output output) {
        this.rotation = rotation;
        this.output = output;
    }

    /**
     * Um evento do trackpad capturado. Em POINTER_UP e UP o dedo que sai ainda está nas listas.
     */
    public void onTouch(int action, int actionIndex, long time, int buttonState, int[] ids, float[] xs, float[] ys) {
        int n = ids.length;
        if (action == ACTION_DOWN) {
            last.clear();
            gestureStart = time;
            maxFingers = 0;
            travel = 0;
            clicked = false;
            scrollRestV = scrollRestH = 0;
        }

        // Movimento antes dos botões, para o clique cair onde o dedo está.
        if (action == ACTION_MOVE) {
            handleMove(n, buttonState, ids, xs, ys);
        }
        for (int i = 0; i < n; i++) {
            last.putIfAbsent(ids[i], new float[]{xs[i], ys[i]});
        }
        maxFingers = Math.max(maxFingers, last.size());

        if (action == ACTION_CANCEL) {
            releaseButton();
            lastButtons = 0;
            last.clear();
            return;
        }
        handleButtons(buttonState, last.size());

        if (action == ACTION_POINTER_UP && actionIndex < n) {
            last.remove(ids[actionIndex]);
        } else if (action == ACTION_UP) {
            releaseButton();
            lastButtons = 0;
            if (!clicked && time - gestureStart <= TAP_MAX_MS && travel < TAP_SLOP && maxFingers <= 2) {
                int which = maxFingers == 2 ? RIGHT : LEFT;
                output.button(which, true);
                output.button(which, false);
            }
            last.clear();
        }
    }

    private void handleMove(int n, int buttonState, int[] ids, float[] xs, float[] ys) {
        float sumX = 0, sumY = 0, bestX = 0, bestY = 0, best = -1;
        int counted = 0;
        for (int i = 0; i < n; i++) {
            float[] prev = last.get(ids[i]);
            if (prev == null) {
                continue;
            }
            float dx = xs[i] - prev[0], dy = ys[i] - prev[1];
            prev[0] = xs[i];
            prev[1] = ys[i];
            sumX += dx;
            sumY += dy;
            counted++;
            float size = Math.abs(dx) + Math.abs(dy);
            if (size > best) {
                best = size;
                bestX = dx;
                bestY = dy;
            }
        }
        if (counted == 0) {
            return;
        }

        if (counted >= 2 && buttonState == 0 && pressedButton == 0) {
            float[] d = TrackpadRotation.rotate(rotation, sumX / counted, sumY / counted);
            travel += Math.abs(d[0]) + Math.abs(d[1]);
            scrollRestV += d[1] * SCROLL_GAIN;
            scrollRestH += -d[0] * SCROLL_GAIN;
            int v = (int) scrollRestV, h = (int) scrollRestH;
            scrollRestV -= v;
            scrollRestH -= h;
            if (v != 0 || h != 0) {
                output.scroll(v, h);
            }
            return;
        }

        float[] d = TrackpadRotation.rotate(rotation, bestX, bestY);
        travel += Math.abs(d[0]) + Math.abs(d[1]);
        int dx = Math.round(d[0]), dy = Math.round(d[1]);
        if (dx != 0 || dy != 0) {
            output.move(dx, dy);
        }
    }

    private void handleButtons(int buttonState, int fingers) {
        boolean wasDown = (lastButtons & (BUTTON_PRIMARY | BUTTON_SECONDARY)) != 0;
        boolean isDown = (buttonState & (BUTTON_PRIMARY | BUTTON_SECONDARY)) != 0;
        if (isDown && !wasDown && pressedButton == 0) {
            pressedButton = ((buttonState & BUTTON_SECONDARY) != 0 || fingers >= 2) ? RIGHT : LEFT;
            clicked = true;
            output.button(pressedButton, true);
        } else if (!isDown && wasDown) {
            releaseButton();
        }
        lastButtons = buttonState;
    }

    private void releaseButton() {
        if (pressedButton != 0) {
            output.button(pressedButton, false);
            pressedButton = 0;
        }
    }
}
