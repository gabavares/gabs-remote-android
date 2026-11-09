package com.limelight.gabs.session;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SessionExitTest {
    @Test
    public void closingTheSessionScreenQuitsTheHostApp() {
        // Voltar ou Desconectar no menu: a tela da sessão está terminando
        assertTrue(SessionExit.quitHostApp(true, true, false));
    }

    @Test
    public void goingToTheBackgroundKeepsThePcReady() {
        // Alt+Tab, Home, recentes: a sessão só pausa
        assertFalse(SessionExit.quitHostApp(false, true, false));
    }

    @Test
    public void withoutAConnectionThereIsNothingToQuit() {
        assertFalse(SessionExit.quitHostApp(true, false, false));
    }

    @Test
    public void reconnectDoesNotQuitHostApp() {
        // A tela termina para reabrir por outro caminho: o PC continua pronto (decisão 0004)
        assertFalse(SessionExit.quitHostApp(true, true, true));
    }
}
