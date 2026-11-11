package com.limelight.gabs.mic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.Test;

public class MicControllerTest {
    static class FakeCapture implements MicController.Capture {
        boolean running;
        int starts;
        int stops;

        @Override public void start() { running = true; starts++; }
        @Override public void stop() { running = false; stops++; }
    }

    static class FakeSender implements MicController.Sender {
        SecretKey key;

        @Override public void resetKey(SecretKey k) { key = k; }
    }

    @Test
    public void startsMutedByDefault() {
        FakeCapture cap = new FakeCapture();
        MicController c = new MicController(cap, new FakeSender(), false);
        assertFalse(c.isOn());
        assertFalse(cap.running);
        assertEquals(0, cap.starts);
    }

    @Test
    public void startsOnWhenPreferred() {
        FakeCapture cap = new FakeCapture();
        MicController c = new MicController(cap, new FakeSender(), true);
        assertTrue(c.isOn());
        assertTrue(cap.running);
    }

    @Test
    public void toggleStartsAndStops() {
        FakeCapture cap = new FakeCapture();
        MicController c = new MicController(cap, new FakeSender(), false);
        c.toggle();
        assertTrue(c.isOn());
        assertTrue(cap.running);
        c.toggle();
        assertFalse(c.isOn());
        assertFalse(cap.running);
        assertEquals(1, cap.starts);
        assertEquals(1, cap.stops);
    }

    @Test
    public void stopsInBackground() {
        FakeCapture cap = new FakeCapture();
        MicController c = new MicController(cap, new FakeSender(), true);
        c.onBackground();
        assertFalse(cap.running);
        assertTrue(c.isOn());
        // Mexer no mudo em segundo plano não liga a captura.
        c.toggle();
        c.toggle();
        assertFalse(cap.running);
    }

    @Test
    public void resumesInForeground() {
        FakeCapture cap = new FakeCapture();
        MicController c = new MicController(cap, new FakeSender(), true);
        c.onBackground();
        c.onForeground();
        assertTrue(cap.running);
        assertEquals(2, cap.starts);

        FakeCapture muted = new FakeCapture();
        MicController m = new MicController(muted, new FakeSender(), false);
        m.onBackground();
        m.onForeground();
        assertFalse(muted.running);
    }

    @Test
    public void keepsStateAcrossReconnect() {
        FakeCapture cap = new FakeCapture();
        FakeSender sender = new FakeSender();
        MicController c = new MicController(cap, sender, false);
        c.toggle();
        SecretKey k = new SecretKeySpec(new byte[16], "AES");
        c.onReconnect(k);
        assertSame(k, sender.key);
        assertTrue(c.isOn());
        assertTrue(cap.running);

        FakeCapture cap2 = new FakeCapture();
        MicController muted = new MicController(cap2, new FakeSender(), false);
        muted.onReconnect(k);
        assertFalse(muted.isOn());
        assertFalse(cap2.running);
    }

    @Test
    public void closeStopsForGood() {
        FakeCapture cap = new FakeCapture();
        MicController c = new MicController(cap, new FakeSender(), true);
        c.close();
        assertFalse(cap.running);
        c.onForeground();
        c.toggle();
        c.toggle();
        assertFalse(cap.running);
    }
}
