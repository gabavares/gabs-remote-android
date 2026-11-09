package com.limelight.gabs.wake;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class WakeFlowTest {
    @Test
    public void becomesOnlineWhenThePcAnswers() {
        WakeFlow f = new WakeFlow();
        f.start(0);
        assertEquals(WakeFlow.Status.WAITING, f.onPoll(false, 1000));
        assertEquals(WakeFlow.Status.ONLINE, f.onPoll(true, 45000));
        assertEquals(45000, f.elapsedMs(45000));
    }

    @Test
    public void timesOutAfterThreeMinutes() {
        WakeFlow f = new WakeFlow();
        f.start(0);
        assertEquals(WakeFlow.Status.WAITING, f.onPoll(false, 179999));
        assertEquals(WakeFlow.Status.TIMEOUT, f.onPoll(false, 180000));
        assertEquals(WakeFlow.Status.TIMEOUT, f.onPoll(true, 181000));
    }
}
