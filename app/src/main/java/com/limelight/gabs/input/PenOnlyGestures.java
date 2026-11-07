package com.limelight.gabs.input;

import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;

/**
 * Gabs Remote: gestos de dois dedos do modo "Só caneta".
 *
 * - pinça: zoom só no tablet ({@link LocalZoom});
 * - arrastar com dois dedos: move a imagem ampliada, ou rola no PC quando não há zoom;
 * - toque rápido de dois dedos: clique direito no PC;
 * - toque duplo de dois dedos: volta o zoom a 1x.
 *
 * Usa coordenadas brutas da tela (getRawX/Y), porque a view da sessão é escalada e movida pelo
 * próprio zoom e as coordenadas locais mudariam durante o gesto.
 */
public class PenOnlyGestures {
    /** O que os gestos pedem à tela da sessão. */
    public interface Host {
        /** Aplica escala e translação do zoom na view da sessão. */
        void applyZoom();
        /** Rola no PC (dy em pixels da tela, positivo = dedos descendo). */
        void scrollPc(float dy);
        /** Clique direito no PC no ponto da tela (coordenadas brutas). */
        void rightClickAt(float rawX, float rawY);
        /** Origem da view da sessão na tela, sem o zoom aplicado: {x, y}. */
        float[] viewOrigin();
    }

    static final long TAP_MAX_MS = 250;
    static final long DOUBLE_TAP_MS = 300;
    static final float MOVE_SLOP_PX = 24;
    static final float PINCH_SLOP = 0.08f;

    private final LocalZoom zoom;
    private final Host host;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private boolean active;
    private long downTime;
    private float startMidX, startMidY, lastMidX, lastMidY;
    private float startSpan, lastSpan;
    private boolean moved, pinching;
    private long lastTapUpTime;
    private Runnable pendingRightClick;

    public PenOnlyGestures(LocalZoom zoom, Host host) {
        this.zoom = zoom;
        this.host = host;
    }

    /** Recebe os eventos de dedo do modo "Só caneta". Sempre consome o evento. */
    public void onTouchEvent(MotionEvent e) {
        int action = e.getActionMasked();
        int count = e.getPointerCount();

        if (action == MotionEvent.ACTION_POINTER_DOWN && count == 2) {
            active = true;
            downTime = e.getEventTime();
            startMidX = lastMidX = midX(e);
            startMidY = lastMidY = midY(e);
            startSpan = lastSpan = span(e);
            moved = pinching = false;
            return;
        }
        if (!active) {
            return;
        }

        if (action == MotionEvent.ACTION_MOVE && count >= 2) {
            float mx = midX(e), my = midY(e), sp = span(e);
            float dx = mx - lastMidX, dy = my - lastMidY;

            if (!pinching && startSpan > 0 && Math.abs(sp / startSpan - 1f) > PINCH_SLOP) {
                pinching = true;
            }
            if (Math.hypot(mx - startMidX, my - startMidY) > MOVE_SLOP_PX) {
                moved = true;
            }

            if (pinching && lastSpan > 0) {
                float[] origin = host.viewOrigin();
                zoom.onScale(sp / lastSpan, mx - origin[0], my - origin[1]);
                zoom.onPan(dx, dy);
                host.applyZoom();
            } else if (moved) {
                if (zoom.scale() > 1f) {
                    zoom.onPan(dx, dy);
                    host.applyZoom();
                } else {
                    host.scrollPc(dy);
                }
            }
            lastMidX = mx;
            lastMidY = my;
            lastSpan = sp;
        } else if (action == MotionEvent.ACTION_POINTER_UP && count == 2) {
            boolean tap = !moved && !pinching && e.getEventTime() - downTime < TAP_MAX_MS;
            if (tap) {
                if (pendingRightClick != null && e.getEventTime() - lastTapUpTime < DOUBLE_TAP_MS) {
                    handler.removeCallbacks(pendingRightClick);
                    pendingRightClick = null;
                    zoom.reset();
                    host.applyZoom();
                } else {
                    final float x = startMidX, y = startMidY;
                    pendingRightClick = () -> {
                        pendingRightClick = null;
                        host.rightClickAt(x, y);
                    };
                    handler.postDelayed(pendingRightClick, DOUBLE_TAP_MS);
                }
                lastTapUpTime = e.getEventTime();
            }
            active = false;
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            active = false;
        }
    }

    private static float midX(MotionEvent e) {
        return (rawX(e, 0) + rawX(e, 1)) / 2f;
    }

    private static float midY(MotionEvent e) {
        return (rawY(e, 0) + rawY(e, 1)) / 2f;
    }

    private static float span(MotionEvent e) {
        return (float) Math.hypot(rawX(e, 0) - rawX(e, 1), rawY(e, 0) - rawY(e, 1));
    }

    private static float rawX(MotionEvent e, int i) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            return e.getRawX(i);
        }
        return e.getX(i) + (e.getRawX() - e.getX());
    }

    private static float rawY(MotionEvent e, int i) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            return e.getRawY(i);
        }
        return e.getY(i) + (e.getRawY() - e.getY());
    }
}
