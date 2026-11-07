package com.limelight.gabs.latency;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ClockSyncTest {
    @Test
    public void usaAAmostraDeMenorIdaEVolta() {
        long[] t0 = {0, 100, 200};
        long[] host = {1000, 1090, 1200};
        long[] t1 = {20, 104, 260};
        assertEquals(988, ClockSync.offsetMs(t0, host, t1));
    }
}
