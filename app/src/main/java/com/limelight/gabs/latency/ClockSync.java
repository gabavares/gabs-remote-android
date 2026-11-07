package com.limelight.gabs.latency;

/** Gabs Remote: sincroniza o relógio do tablet com o do marcador no PC (ping UDP). */
public final class ClockSync {
    private ClockSync() {
    }

    /**
     * Diferença entre o relógio do marcador e o do tablet, usando a amostra de menor ida e volta.
     *
     * @param t0     hora do tablet ao mandar cada ping
     * @param hostMs hora do marcador na resposta
     * @param t1     hora do tablet ao receber a resposta
     * @return offset tal que horaDoMarcador = horaDoTablet + offset
     */
    public static long offsetMs(long[] t0, long[] hostMs, long[] t1) {
        int best = -1;
        long bestRtt = Long.MAX_VALUE;
        for (int i = 0; i < t0.length; i++) {
            long rtt = t1[i] - t0[i];
            if (rtt >= 0 && rtt < bestRtt) {
                bestRtt = rtt;
                best = i;
            }
        }
        if (best < 0) {
            throw new IllegalArgumentException("nenhuma amostra válida");
        }
        return hostMs[best] - (t0[best] + t1[best]) / 2;
    }
}
