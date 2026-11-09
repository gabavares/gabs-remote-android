package com.limelight.gabs.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.limelight.gabs.net.PathKind;
import com.limelight.nvstream.jni.MoonBridge;

import org.junit.Test;

public class ReconnectPolicyTest {
    @Test
    public void reconnectsOnNetworkErrors() {
        assertTrue(ReconnectPolicy.shouldReconnect(MoonBridge.ML_ERROR_NO_VIDEO_TRAFFIC, false));
        assertTrue(ReconnectPolicy.shouldReconnect(-1, false));
    }

    @Test
    public void doesNotReconnectWhenItEndedOnPurpose() {
        assertFalse(ReconnectPolicy.shouldReconnect(MoonBridge.ML_ERROR_GRACEFUL_TERMINATION, false));
        assertFalse(ReconnectPolicy.shouldReconnect(MoonBridge.ML_ERROR_PROTECTED_CONTENT, false));
        assertFalse(ReconnectPolicy.shouldReconnect(MoonBridge.ML_ERROR_FRAME_CONVERSION, false));
        assertFalse(ReconnectPolicy.shouldReconnect(-1, true));
    }

    @Test
    public void backoffAndGiveUp() {
        assertEquals(0, ReconnectPolicy.nextDelayMs(0, 0));
        assertEquals(1000, ReconnectPolicy.nextDelayMs(1, 0));
        assertEquals(4000, ReconnectPolicy.nextDelayMs(3, 5000));
        assertEquals(15000, ReconnectPolicy.nextDelayMs(7, 60000));
        assertEquals(-1, ReconnectPolicy.nextDelayMs(7, 110000));
    }

    @Test
    public void tailscaleSurvivesNetworkChanges() {
        assertFalse(ReconnectPolicy.restartOnNetworkChange(PathKind.TAILSCALE));
        assertTrue(ReconnectPolicy.restartOnNetworkChange(PathKind.IPV6));
        assertTrue(ReconnectPolicy.restartOnNetworkChange(PathKind.LAN));
        assertTrue(ReconnectPolicy.restartOnNetworkChange(null));
    }
}
