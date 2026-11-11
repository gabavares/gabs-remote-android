package com.limelight.gabs.input;

import android.app.Activity;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.PopupMenu;

/**
 * Gabs Remote: menu da sessão, aberto pelo toque de três dedos.
 * Usa só o PopupMenu padrão do Android (o layout próprio é da fase 7).
 */
public class SessionMenu {
    /** Ações que o menu pede à tela da sessão. */
    public interface Listener {
        void onToggleKeyboard();
        void onTouchModeSelected(TouchMode mode);
        void onResetZoom();
        void onMeasureLatencyClick();
        void onMeasureLatencyStamp();
        void onSendAltTab();
        void onSendWin();
        void onDisconnect();
        /** Liga ou silencia o microfone do tablet no PC (fase 5). */
        void onToggleMic();
    }

    /** O que o host suporta e o estado atual (fase 5: itens que só aparecem com o host novo). */
    public static final class Extras {
        public boolean micAvailable;
        public boolean micOn;
    }

    private static final int ID_KEYBOARD = 1;
    private static final int ID_MODE_DIRETO = 10, ID_MODE_TRACKPAD = 11, ID_MODE_SO_CANETA = 12;
    private static final int ID_RESET_ZOOM = 20;
    private static final int ID_LATENCY_CLICK = 30, ID_LATENCY_STAMP = 31;
    private static final int ID_ALT_TAB = 40, ID_WIN = 41;
    private static final int ID_MIC = 50;
    private static final int ID_DISCONNECT = 90;

    private SessionMenu() {
    }

    /**
     * Abre o menu numa âncora de 1 px no ponto (x, y) da tela (veja {@link MenuAnchor}).
     */
    public static void show(Activity activity, float x, float y, TouchMode current, Extras extras, Listener listener) {
        ViewGroup root = activity.findViewById(android.R.id.content);
        int[] origin = new int[2];
        root.getLocationOnScreen(origin);
        float[] p = MenuAnchor.place(new float[]{x - origin[0]}, new float[]{y - origin[1]}, root.getWidth(), root.getHeight());
        View anchor = new View(activity);
        root.addView(anchor, new FrameLayout.LayoutParams(1, 1));
        anchor.setX(p[0]);
        anchor.setY(p[1]);

        PopupMenu popup = new PopupMenu(activity, anchor, Gravity.NO_GRAVITY);
        popup.setOnDismissListener(menu -> root.removeView(anchor));
        Menu menu = popup.getMenu();

        menu.add(Menu.NONE, ID_KEYBOARD, 0, "Teclado");

        SubMenu modes = menu.addSubMenu(Menu.NONE, Menu.NONE, 1, "Modo de toque");
        modes.add(1, ID_MODE_DIRETO, 0, "Direto").setChecked(current == TouchMode.DIRETO);
        modes.add(1, ID_MODE_TRACKPAD, 1, "Trackpad").setChecked(current == TouchMode.TRACKPAD);
        modes.add(1, ID_MODE_SO_CANETA, 2, "Só caneta").setChecked(current == TouchMode.SO_CANETA);
        modes.setGroupCheckable(1, true, true);

        menu.add(Menu.NONE, ID_RESET_ZOOM, 2, "Redefinir zoom");

        SubMenu latency = menu.addSubMenu(Menu.NONE, Menu.NONE, 3, "Medir latência");
        latency.add(Menu.NONE, ID_LATENCY_CLICK, 0, "Clique");
        latency.add(Menu.NONE, ID_LATENCY_STAMP, 1, "Carimbo");

        menu.add(Menu.NONE, ID_ALT_TAB, 4, "Alt+Tab");
        menu.add(Menu.NONE, ID_WIN, 5, "Win");
        if (extras != null && extras.micAvailable) {
            menu.add(Menu.NONE, ID_MIC, 6, extras.micOn ? "Silenciar microfone" : "Ligar microfone");
        }
        menu.add(Menu.NONE, ID_DISCONNECT, 9, "Desconectar");

        popup.setOnMenuItemClickListener((MenuItem item) -> {
            switch (item.getItemId()) {
                case ID_KEYBOARD: listener.onToggleKeyboard(); return true;
                case ID_MODE_DIRETO: listener.onTouchModeSelected(TouchMode.DIRETO); return true;
                case ID_MODE_TRACKPAD: listener.onTouchModeSelected(TouchMode.TRACKPAD); return true;
                case ID_MODE_SO_CANETA: listener.onTouchModeSelected(TouchMode.SO_CANETA); return true;
                case ID_RESET_ZOOM: listener.onResetZoom(); return true;
                case ID_LATENCY_CLICK: listener.onMeasureLatencyClick(); return true;
                case ID_LATENCY_STAMP: listener.onMeasureLatencyStamp(); return true;
                case ID_ALT_TAB: listener.onSendAltTab(); return true;
                case ID_WIN: listener.onSendWin(); return true;
                case ID_MIC: listener.onToggleMic(); return true;
                case ID_DISCONNECT: listener.onDisconnect(); return true;
                default: return false;
            }
        });
        // Mostra depois do layout da âncora.
        anchor.post(popup::show);
    }
}
