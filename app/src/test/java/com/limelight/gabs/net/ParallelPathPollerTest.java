package com.limelight.gabs.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParallelPathPollerTest {
    /** Prober falso: dorme o tempo pedido para cada endereço; negativo = falha depois de |t| ms. */
    private static ParallelPathPoller.Prober<String> fake(final Map<String, Long> delays) {
        return c -> {
            long t = delays.get(c.address.address);
            Thread.sleep(Math.abs(t));
            return t < 0 ? null : c.address.address;
        };
    }

    private static Map<String, Long> delays(Object... kv) {
        Map<String, Long> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], ((Number) kv[i + 1]).longValue());
        }
        return m;
    }

    @Test
    public void lanFailureFallsBackToRemote() throws Exception {
        List<Candidate> cs = Arrays.asList(PathSelectorTest.c(PathKind.LAN, "lan"), PathSelectorTest.c(PathKind.TAILSCALE, "ts"));
        ParallelPathPoller.Result<String> r = ParallelPathPoller.poll(cs, fake(delays("lan", -5, "ts", 30)), 800);
        assertEquals(PathKind.TAILSCALE, r.candidate.kind);
        assertEquals("ts", r.value);
    }

    @Test
    public void lanReturnsImmediately() throws Exception {
        List<Candidate> cs = Arrays.asList(PathSelectorTest.c(PathKind.LAN, "lan"), PathSelectorTest.c(PathKind.TAILSCALE, "ts"));
        long start = System.nanoTime();
        ParallelPathPoller.Result<String> r = ParallelPathPoller.poll(cs, fake(delays("lan", 10, "ts", 2000)), 800);
        long ms = (System.nanoTime() - start) / 1_000_000;
        assertEquals(PathKind.LAN, r.candidate.kind);
        assertTrue("levou " + ms + " ms", ms < 500);
    }

    @Test
    public void slowCandidateDoesNotBlockChoice() throws Exception {
        List<Candidate> cs = Arrays.asList(PathSelectorTest.c(PathKind.IPV6, "v6"), PathSelectorTest.c(PathKind.IPV4_PUBLICO, "v4"));
        long start = System.nanoTime();
        ParallelPathPoller.Result<String> r = ParallelPathPoller.poll(cs, fake(delays("v6", 30, "v4", 60_000)), 800);
        long ms = (System.nanoTime() - start) / 1_000_000;
        assertEquals(PathKind.IPV6, r.candidate.kind);
        assertTrue("levou " + ms + " ms", ms < 30 + 800 + 200);
    }

    @Test
    public void allFailReturnsNull() throws Exception {
        List<Candidate> cs = Arrays.asList(PathSelectorTest.c(PathKind.LAN, "lan"), PathSelectorTest.c(PathKind.TAILSCALE, "ts"));
        assertNull(ParallelPathPoller.poll(cs, fake(delays("lan", -5, "ts", -10)), 800));
    }

    @Test
    public void emptyListReturnsNull() throws Exception {
        assertNull(ParallelPathPoller.poll(Collections.<Candidate>emptyList(), fake(delays()), 800));
    }
}
