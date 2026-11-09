package com.limelight.gabs.net;

import java.util.ArrayList;
import java.util.List;

/**
 * Gabs Remote: testa todos os caminhos do PC ao mesmo tempo (estilo ICE, spec §5.2).
 * Volta na hora se a rede de casa responder; senão, depois da primeira resposta espera no máximo
 * {@code graceMs} pelas outras e escolhe com {@link PathSelector}.
 */
public final class ParallelPathPoller {
    public interface Prober<T> {
        /** @return o resultado, ou null se o caminho falhou */
        T probe(Candidate c) throws InterruptedException;
    }

    public static final class Result<T> {
        public final Candidate candidate;
        public final long rttMs;
        public final T value;

        public Result(Candidate candidate, long rttMs, T value) {
            this.candidate = candidate;
            this.rttMs = rttMs;
            this.value = value;
        }
    }

    private ParallelPathPoller() {
    }

    public static <T> Result<T> poll(List<Candidate> cs, final Prober<T> prober, long graceMs) throws InterruptedException {
        final Object lock = new Object();
        final List<Result<T>> ok = new ArrayList<>();
        final int[] done = {0};
        List<Thread> threads = new ArrayList<>();

        for (final Candidate c : cs) {
            Thread t = new Thread(() -> {
                long start = System.nanoTime();
                T value = null;
                try {
                    value = prober.probe(c);
                } catch (InterruptedException ignored) {
                } catch (RuntimeException ignored) {
                }
                long rtt = (System.nanoTime() - start) / 1_000_000;
                synchronized (lock) {
                    if (value != null) {
                        ok.add(new Result<>(c, rtt, value));
                    }
                    done[0]++;
                    lock.notifyAll();
                }
            }, "Gabs path poll - " + c);
            threads.add(t);
            t.start();
        }

        try {
            synchronized (lock) {
                long deadline = 0;
                while (done[0] < cs.size()) {
                    if (hasLan(ok)) {
                        break;
                    }
                    if (!ok.isEmpty()) {
                        long now = System.nanoTime() / 1_000_000;
                        if (deadline == 0) {
                            deadline = now + graceMs;
                        }
                        if (now >= deadline) {
                            break;
                        }
                        lock.wait(deadline - now);
                    } else {
                        lock.wait();
                    }
                }
                return PathSelector.choose(new ArrayList<>(ok));
            }
        } finally {
            for (Thread t : threads) {
                t.interrupt();
            }
        }
    }

    private static <T> boolean hasLan(List<Result<T>> ok) {
        for (Result<T> r : ok) {
            if (r.candidate.kind == PathKind.LAN) {
                return true;
            }
        }
        return false;
    }
}
