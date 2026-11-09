package com.limelight.gabs.wake;

/** Gabs Remote: espera o PC ligar depois do pacote mágico, com desistência em 3 minutos. */
public final class WakeFlow {
    public static final long TIMEOUT_MS = 180_000;

    public enum Status { WAITING, ONLINE, TIMEOUT }

    private long startMs;
    private Status status = Status.WAITING;

    public void start(long nowMs) {
        startMs = nowMs;
        status = Status.WAITING;
    }

    public Status onPoll(boolean pcOnline, long nowMs) {
        if (status != Status.WAITING) {
            return status;
        }
        if (pcOnline) {
            status = Status.ONLINE;
        } else if (elapsedMs(nowMs) >= TIMEOUT_MS) {
            status = Status.TIMEOUT;
        }
        return status;
    }

    public long elapsedMs(long nowMs) {
        return nowMs - startMs;
    }
}
