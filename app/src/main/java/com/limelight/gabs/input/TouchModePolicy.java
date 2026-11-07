package com.limelight.gabs.input;

import android.view.MotionEvent;

/**
 * Gabs Remote: decide o que fazer com cada toque de dedo, conforme o modo e a caneta.
 *
 * Rejeição de palma: enquanto a caneta está perto (hover) ou encostada, e por PALM_GRACE_MS
 * depois que ela sai, toques de dedo são descartados. Três dedos são sempre o gesto do menu.
 */
public class TouchModePolicy {
    public enum Action { DROP, NATIVE_TOUCH, UPSTREAM_CONTEXT, GESTURE }

    public static final long PALM_GRACE_MS = 300;

    private boolean penPresent;
    private long penLeftAtMs = Long.MIN_VALUE / 2;

    /** Recebe todo evento de caneta (inclusive hover), antes de ele ir ao host. */
    public void onPenEvent(int actionMasked, long nowMs) {
        switch (actionMasked) {
            case MotionEvent.ACTION_HOVER_ENTER:
            case MotionEvent.ACTION_HOVER_MOVE:
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_MOVE:
                penPresent = true;
                break;
            case MotionEvent.ACTION_HOVER_EXIT:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
                if (penPresent) {
                    penLeftAtMs = nowMs;
                }
                penPresent = false;
                break;
            default:
                break;
        }
    }

    public boolean penNearby(long nowMs) {
        return penPresent || nowMs - penLeftAtMs < PALM_GRACE_MS;
    }

    public Action fingerAction(TouchMode mode, int pointerCount, long nowMs) {
        if (pointerCount >= 3) {
            return Action.GESTURE;
        }
        if (penNearby(nowMs)) {
            return Action.DROP;
        }
        switch (mode) {
            case TRACKPAD:
                return Action.UPSTREAM_CONTEXT;
            case SO_CANETA:
                return pointerCount >= 2 ? Action.GESTURE : Action.DROP;
            case DIRETO:
            default:
                return Action.NATIVE_TOUCH;
        }
    }
}
