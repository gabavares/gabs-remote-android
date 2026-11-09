package com.limelight.gabs.net;

import java.util.ArrayList;
import java.util.List;

/** Gabs Remote: escolhe o caminho entre os que responderam. */
public final class PathSelector {
    /** Diferença de RTT abaixo da qual vale a ordem de preferência. */
    public static final long TIE_MS = 10;

    private PathSelector() {
    }

    public static <T> ParallelPathPoller.Result<T> choose(List<ParallelPathPoller.Result<T>> ok) {
        ParallelPathPoller.Result<T> fastest = null;
        ParallelPathPoller.Result<T> lan = null;
        for (ParallelPathPoller.Result<T> r : ok) {
            if (r.candidate.kind == PathKind.LAN && (lan == null || r.rttMs < lan.rttMs)) {
                lan = r;
            }
            if (fastest == null || r.rttMs < fastest.rttMs) {
                fastest = r;
            }
        }
        if (lan != null) {
            return lan;
        }
        if (fastest == null) {
            return null;
        }
        ParallelPathPoller.Result<T> best = fastest;
        for (ParallelPathPoller.Result<T> r : ok) {
            if (r.rttMs - fastest.rttMs <= TIE_MS && r.candidate.kind.ordinal() < best.candidate.kind.ordinal()) {
                best = r;
            }
        }
        return best;
    }

    /** @param forcedOrNull tipo forçado nas configurações, ou null para todos */
    public static List<Candidate> filter(List<Candidate> all, PathKind forcedOrNull) {
        if (forcedOrNull == null) {
            return all;
        }
        List<Candidate> out = new ArrayList<>();
        for (Candidate c : all) {
            if (c.kind == forcedOrNull) {
                out.add(c);
            }
        }
        return out;
    }
}
