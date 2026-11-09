package com.limelight.gabs.session;

import com.limelight.LimeLog;
import com.limelight.gabs.net.Candidate;
import com.limelight.gabs.net.ParallelPathPoller;

import java.util.List;

/**
 * Gabs Remote: tenta achar o PC de novo por qualquer caminho conhecido, com as esperas da
 * {@link ReconnectPolicy}. Roda numa thread própria; o resultado chega pelo {@link Listener}.
 */
public final class Reconnector {
    public interface Listener {
        void onAttempt(int attempt);
        void onFound(ParallelPathPoller.Result<Boolean> result);
        void onGaveUp();
    }

    private final List<Candidate> candidates;
    private final ParallelPathPoller.Prober<Boolean> prober;
    private final Listener listener;
    private final long graceMs;
    private Thread thread;

    public Reconnector(List<Candidate> candidates, ParallelPathPoller.Prober<Boolean> prober, long graceMs, Listener listener) {
        this.candidates = candidates;
        this.prober = prober;
        this.graceMs = graceMs;
        this.listener = listener;
    }

    public synchronized void start() {
        if (thread != null) {
            return;
        }
        thread = new Thread(this::run, "Gabs reconnect");
        thread.start();
    }

    public synchronized void cancel() {
        if (thread != null) {
            thread.interrupt();
        }
    }

    private void run() {
        long start = System.currentTimeMillis();
        try {
            for (int attempt = 0; ; attempt++) {
                long delay = ReconnectPolicy.nextDelayMs(attempt, System.currentTimeMillis() - start);
                if (delay < 0) {
                    listener.onGaveUp();
                    return;
                }
                Thread.sleep(delay);
                listener.onAttempt(attempt + 1);
                ParallelPathPoller.Result<Boolean> r = ParallelPathPoller.poll(candidates, prober, graceMs);
                if (r != null) {
                    LimeLog.info("Gabs Remote: PC encontrado de novo por " + r.candidate);
                    listener.onFound(r);
                    return;
                }
            }
        } catch (InterruptedException e) {
            LimeLog.info("Gabs Remote: reconexão cancelada");
        }
    }
}
