package com.limelight.gabs.session;

/**
 * Gabs Remote: o que acontece no PC quando a tela da sessão para.
 *
 * Em segundo plano (Alt+Tab, Home, recentes) a sessão só pausa: o host mantém a tela virtual e o
 * teclado, e voltar é rápido. Fechar a tela da sessão (Voltar, Desconectar no menu) encerra o app
 * no host, que libera o PC na hora.
 */
public final class SessionExit {
    private SessionExit() {
    }

    /**
     * @param finishing a tela da sessão está terminando ({@code Activity.isFinishing()})
     * @param connected a sessão chegou a ser aberta
     * @return true para pedir ao host que encerre o app
     */
    public static boolean quitHostApp(boolean finishing, boolean connected) {
        return finishing && connected;
    }
}
