package com.limelight.gabs.mic;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class MicSenderTest {
    static int seqOf(byte[] p) {
        return (p[4] & 0xff) | (p[5] & 0xff) << 8 | (p[6] & 0xff) << 16 | (p[7] & 0xff) << 24;
    }

    @Test
    public void seqNeverRepeatsAcrossMute() throws Exception {
        List<byte[]> sent = new ArrayList<>();
        MicSender sender = new MicSender(sent::add, MicPacketTest.testKey());
        MicController.Capture capture = new MicController.Capture() {
            @Override public void start() { }
            @Override public void stop() { }
        };
        MicController c = new MicController(capture, sender, true);
        byte[] opus = {1, 2, 3};
        for (int i = 0; i < 3; i++) sender.send(opus, opus.length);
        c.toggle(); // mudo
        c.toggle(); // liga de novo
        for (int i = 0; i < 2; i++) sender.send(opus, opus.length);
        assertEquals(5, sent.size());
        for (int i = 0; i < 5; i++) {
            assertEquals(i, seqOf(sent.get(i)));
        }
    }

    @Test
    public void newKeyKeepsCounting() throws Exception {
        List<byte[]> sent = new ArrayList<>();
        MicSender sender = new MicSender(sent::add, MicPacketTest.testKey());
        byte[] opus = {9};
        sender.send(opus, 1);
        sender.resetKey(new javax.crypto.spec.SecretKeySpec(new byte[16], "AES"));
        sender.send(opus, 1);
        assertEquals(1, seqOf(sent.get(1)));
    }
}
