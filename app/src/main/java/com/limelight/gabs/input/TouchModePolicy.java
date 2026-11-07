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

    /**
     * Ação de caneta contida num evento que pode misturar dedos e caneta (ex.: palma apoiada antes).
     *
     * @return a ação a passar para onPenEvent, ou -1 se o evento não tem caneta
     */
    public static int penAction(int actionMasked, int actionIndex, int[] toolTypes) {
        boolean hasPen = false;
        for (int t : toolTypes) {
            if (isPen(t)) {
                hasPen = true;
                break;
            }
        }
        if (!hasPen) {
            return -1;
        }
        switch (actionMasked) {
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_POINTER_UP:
                if (actionIndex >= 0 && actionIndex < toolTypes.length && isPen(toolTypes[actionIndex])) {
                    return actionMasked == MotionEvent.ACTION_POINTER_DOWN ? MotionEvent.ACTION_DOWN : MotionEvent.ACTION_UP;
                }
                // Um dedo entrou ou saiu; a caneta continua na tela.
                return MotionEvent.ACTION_MOVE;
            default:
                return actionMasked;
        }
    }

    private static boolean isPen(int toolType) {
        return toolType == MotionEvent.TOOL_TYPE_STYLUS || toolType == MotionEvent.TOOL_TYPE_ERASER;
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
