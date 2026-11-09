package com.limelight.gabs.session;

import com.limelight.gabs.net.PathKind;
import com.limelight.nvstream.jni.MoonBridge;

/**
 * Gabs Remote: quando e com que espera a sessão tenta voltar sozinha depois de cair. O PC continua
 * pronto durante as tentativas (decisão 0004); o app só reabre a sessão.
 */
public final class ReconnectPolicy {
    public static final long GIVE_UP_MS = 120_000;
    private static final long[] DELAYS_MS = {0, 1000, 2000, 4000, 8000};
    private static final long LATER_DELAY_MS = 15000;

    private ReconnectPolicy() {
    }

    public static boolean shouldReconnect(int errorCode, boolean userQuit) {
        if (userQuit) {
            return false;
        }
        switch (errorCode) {
            case MoonBridge.ML_ERROR_GRACEFUL_TERMINATION:
            case MoonBridge.ML_ERROR_PROTECTED_CONTENT:
            case MoonBridge.ML_ERROR_FRAME_CONVERSION:
                return false;
            default:
                return true;
        }
    }

    /** @return a espera antes da tentativa {@code attempt} (0 = primeira), ou -1 para desistir */
    public static long nextDelayMs(int attempt, long elapsedMs) {
        long delay = attempt < DELAYS_MS.length ? DELAYS_MS[attempt] : LATER_DELAY_MS;
        return elapsedMs + delay > GIVE_UP_MS ? -1 : delay;
    }

    /** O túnel do Tailscale sobrevive à troca de rede: espera a queda real em vez de derrubar a sessão. */
    public static boolean restartOnNetworkChange(PathKind active) {
        return active != PathKind.TAILSCALE;
    }
}
