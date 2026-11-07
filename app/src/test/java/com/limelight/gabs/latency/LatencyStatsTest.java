package com.limelight.gabs.latency;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LatencyStatsTest {
    private static LatencyStats umACem() {
        LatencyStats s = new LatencyStats();
        for (int i = 1; i <= 100; i++) {
            s.add(i);
        }
        return s;
    }

    @Test
    public void estatisticasDe1a100() {
        LatencyStats s = umACem();
        assertEquals(50.5, s.avg(), 1e-9);
        assertEquals(50, s.p50(), 1e-9);
        assertEquals(95, s.p95(), 1e-9);
        assertEquals(100, s.max(), 1e-9);
    }

    @Test
    public void contaPerdidos() {
        LatencyStats s = umACem();
        s.addLost();
        s.addLost();
        assertEquals(2, s.lost());
    }

    @Test
    public void csvComCabecalho() {
        LatencyStats s = umACem();
        String csv = s.toCsv("clique");
        assertTrue(csv.startsWith("modo,amostra,ms"));
        assertTrue(csv.contains("clique,1,1.0"));
    }
}
