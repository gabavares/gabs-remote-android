package com.limelight.gabs.input;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.limelight.R;

/**
 * Gabs Remote: desenha o {@link LocalCursor} por cima da imagem da sessão (um ImageView com o
 * desenho do formato, movido por translação; aparece no quadro seguinte da tela).
 */
public final class CursorOverlay {
    /** Lado do desenho (os vetores têm 32 x 32 unidades). */
    private static final float SIZE_DP = 24;
    private static final float VIEWPORT = 32;

    private final ImageView view;
    private final float sizePx;
    private CursorShape current;

    public CursorOverlay(Context context, ViewGroup parent) {
        sizePx = SIZE_DP * context.getResources().getDisplayMetrics().density;
        view = new ImageView(context);
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        view.setClickable(false);
        view.setFocusable(false);
        view.setVisibility(View.GONE);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams((int) sizePx, (int) sizePx);
        lp.gravity = Gravity.TOP | Gravity.LEFT;
        parent.addView(view, lp);
    }

    /** Redesenha conforme o cursor e a posição atual da view da sessão (zoom local incluído). */
    public void render(LocalCursor cursor, View streamView, long nowMs) {
        CursorShape shape = cursor.drawnShape(nowMs);
        int w = streamView.getWidth(), h = streamView.getHeight();
        if (shape == null || w == 0 || h == 0) {
            view.setVisibility(View.GONE);
            return;
        }
        if (shape != current) {
            view.setImageResource(drawable(shape));
            current = shape;
        }
        float[] p = cursor.viewPoint(w, h);
        // A view pode estar ampliada pelo centro (zoom local do modo Só caneta).
        float x = streamView.getLeft() + w / 2f + (p[0] - w / 2f) * streamView.getScaleX() + streamView.getTranslationX();
        float y = streamView.getTop() + h / 2f + (p[1] - h / 2f) * streamView.getScaleY() + streamView.getTranslationY();
        float[] hot = hotspot(shape);
        view.setTranslationX(x - hot[0] / VIEWPORT * sizePx);
        view.setTranslationY(y - hot[1] / VIEWPORT * sizePx);
        view.setVisibility(View.VISIBLE);
        view.bringToFront();
    }

    public void hide() {
        view.setVisibility(View.GONE);
    }

    public void remove() {
        ViewGroup parent = (ViewGroup) view.getParent();
        if (parent != null) {
            parent.removeView(view);
        }
    }

    private static int drawable(CursorShape shape) {
        switch (shape) {
            case IBEAM: return R.drawable.gr_cursor_ibeam;
            case HAND: return R.drawable.gr_cursor_hand;
            case WAIT: return R.drawable.gr_cursor_wait;
            case APP_STARTING: return R.drawable.gr_cursor_app_starting;
            case CROSS: return R.drawable.gr_cursor_cross;
            case SIZE_NS: return R.drawable.gr_cursor_size_ns;
            case SIZE_WE: return R.drawable.gr_cursor_size_we;
            case SIZE_NWSE: return R.drawable.gr_cursor_size_nwse;
            case SIZE_NESW: return R.drawable.gr_cursor_size_nesw;
            case SIZE_ALL: return R.drawable.gr_cursor_size_all;
            case NO: return R.drawable.gr_cursor_no;
            case ARROW:
            default:
                return R.drawable.gr_cursor_arrow;
        }
    }

    /** Ponto ativo do desenho, em unidades do vetor (0 a 32). */
    private static float[] hotspot(CursorShape shape) {
        switch (shape) {
            case ARROW:
            case APP_STARTING:
                return new float[]{2, 2};
            case HAND:
                return new float[]{11, 2.5f};
            default:
                return new float[]{16, 16};
        }
    }
}
