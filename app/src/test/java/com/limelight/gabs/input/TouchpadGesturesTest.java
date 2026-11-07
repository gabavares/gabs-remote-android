package com.limelight.gabs.input;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class TouchpadGesturesTest {
    // Valores de MotionEvent, repetidos aqui para o teste rodar sem o Android.
    private static final int DOWN = 0, UP = 1, MOVE = 2, CANCEL = 3, POINTER_DOWN = 5, POINTER_UP = 6;
    private static final int PRIMARY = 1;

    private final List<String> out = new ArrayList<>();
    private TouchpadGestures pad;

    @Before
    public void setUp() {
        pad = new TouchpadGestures(0, new TouchpadGestures.Output() {
            @Override public void move(int dx, int dy) { out.add("move " + dx + " " + dy); }
            @Override public void scroll(int vertical, int horizontal) { out.add("scroll " + vertical + " " + horizontal); }
            @Override public void button(int which, boolean down) { out.add((which == TouchpadGestures.LEFT ? "left " : "right ") + (down ? "down" : "up")); }
        });
    }

    /** Evento com os dedos em pares (id, x, y). */
    private void ev(int action, int index, long time, int buttons, float... p) {
        int n = p.length / 3;
        int[] ids = new int[n];
        float[] xs = new float[n], ys = new float[n];
        for (int i = 0; i < n; i++) {
            ids[i] = (int) p[3 * i];
            xs[i] = p[3 * i + 1];
            ys[i] = p[3 * i + 2];
        }
        pad.onTouch(action, index, time, buttons, ids, xs, ys);
    }

    private int sumScrollVertical() {
        int sum = 0;
        for (String s : out) {
            if (s.startsWith("scroll ")) sum += Integer.parseInt(s.split(" ")[1]);
        }
        return sum;
    }

    @Test
    public void oneFingerMovesTheCursor() {
        ev(DOWN, 0, 0, 0, 0, 100, 100);
        ev(MOVE, 0, 50, 0, 0, 110, 95);
        ev(MOVE, 0, 300, 0, 0, 130, 95);
        ev(UP, 0, 400, 0, 0, 130, 95);
        assertEquals(List.of("move 10 -5", "move 20 0"), out);
    }

    @Test
    public void movementFollowsTheTrackpadRotation() {
        pad = new TouchpadGestures(90, new TouchpadGestures.Output() {
            @Override public void move(int dx, int dy) { out.add("move " + dx + " " + dy); }
            @Override public void scroll(int vertical, int horizontal) { }
            @Override public void button(int which, boolean down) { }
        });
        ev(DOWN, 0, 0, 0, 0, 100, 100);
        ev(MOVE, 0, 300, 0, 0, 110, 100);
        assertEquals(List.of("move 0 -10"), out);
    }

    @Test
    public void twoFingersScrollWithoutMovingTheCursor() {
        ev(DOWN, 0, 0, 0, 0, 100, 100);
        ev(POINTER_DOWN, 1, 10, 0, 0, 100, 100, 1, 200, 100);
        ev(MOVE, 0, 100, 0, 0, 100, 140, 1, 200, 140);
        ev(MOVE, 0, 200, 0, 0, 100, 180, 1, 200, 180);
        ev(POINTER_UP, 1, 400, 0, 0, 100, 180, 1, 200, 180);
        ev(UP, 0, 410, 0, 0, 100, 180);
        for (String s : out) {
            assertTrue(s, s.startsWith("scroll "));
        }
        // Dedos descendo puxam o conteúdo para baixo: roda para cima (positivo).
        assertTrue(sumScrollVertical() > 0);
    }

    @Test
    public void quickTapIsALeftClick() {
        ev(DOWN, 0, 0, 0, 0, 100, 100);
        ev(MOVE, 0, 40, 0, 0, 102, 101);
        ev(UP, 0, 90, 0, 0, 102, 101);
        assertEquals(List.of("move 2 1", "left down", "left up"), out);
    }

    @Test
    public void twoFingerTapIsARightClick() {
        ev(DOWN, 0, 0, 0, 0, 100, 100);
        ev(POINTER_DOWN, 1, 20, 0, 0, 100, 100, 1, 200, 100);
        ev(POINTER_UP, 1, 100, 0, 0, 100, 100, 1, 200, 100);
        ev(UP, 0, 110, 0, 0, 100, 100);
        assertEquals(List.of("right down", "right up"), out);
    }

    @Test
    public void slowOrLongTouchIsNotATap() {
        ev(DOWN, 0, 0, 0, 0, 100, 100);
        ev(UP, 0, 600, 0, 0, 100, 100);
        ev(DOWN, 0, 1000, 0, 0, 100, 100);
        ev(MOVE, 0, 1050, 0, 0, 300, 100);
        ev(UP, 0, 1100, 0, 0, 300, 100);
        assertEquals(List.of("move 200 0"), out);
    }

    @Test
    public void physicalClickAndDragFollowsTheMovingFinger() {
        ev(DOWN, 0, 0, 0, 0, 400, 1400);                                   // polegar no botão
        ev(MOVE, 0, 100, PRIMARY, 0, 400, 1400);                          // aperta
        ev(POINTER_DOWN, 1, 200, PRIMARY, 0, 400, 1400, 1, 400, 500);     // indicador
        ev(MOVE, 0, 300, PRIMARY, 0, 400, 1400, 1, 450, 520);             // arrasta
        ev(POINTER_UP, 1, 400, PRIMARY, 0, 400, 1400, 1, 450, 520);
        ev(MOVE, 0, 500, 0, 0, 400, 1400);                                // solta o botão
        ev(UP, 0, 600, 0, 0, 400, 1400);
        assertEquals(List.of("left down", "move 50 20", "left up"), out);
    }

    @Test
    public void physicalClickWithTwoFingersIsARightClick() {
        ev(DOWN, 0, 0, 0, 0, 100, 100);
        ev(POINTER_DOWN, 1, 10, 0, 0, 100, 100, 1, 200, 100);
        ev(MOVE, 0, 100, PRIMARY, 0, 100, 100, 1, 200, 100);
        ev(MOVE, 0, 200, 0, 0, 100, 100, 1, 200, 100);
        ev(POINTER_UP, 1, 300, 0, 0, 100, 100, 1, 200, 100);
        ev(UP, 0, 310, 0, 0, 100, 100);
        assertEquals(List.of("right down", "right up"), out);
    }

    @Test
    public void canceledTouchReleasesTheButton() {
        ev(DOWN, 0, 0, 0, 0, 100, 100);
        ev(MOVE, 0, 100, PRIMARY, 0, 100, 100);
        ev(CANCEL, 0, 150, PRIMARY, 0, 100, 100);
        assertEquals(List.of("left down", "left up"), out);
    }
}
