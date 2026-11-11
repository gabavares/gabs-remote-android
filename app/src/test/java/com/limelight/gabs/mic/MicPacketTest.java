package com.limelight.gabs.mic;

import static org.junit.Assert.assertEquals;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.Test;

public class MicPacketTest {
    /** host/tests/unit/gabs/testdata/mic-packet.hex (gerado pelo OpenSSL na Task 3). */
    static final String HOST_VECTOR = "47524d31070000008fd1b91a7a06b5d2043a567e738e16a283c7c00b";

    static SecretKey testKey() {
        byte[] k = new byte[16];
        for (int i = 0; i < 16; i++) {
            k[i] = (byte) i;
        }
        return new SecretKeySpec(k, "AES");
    }

    static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte x : b) {
            sb.append(String.format("%02x", x & 0xff));
        }
        return sb.toString();
    }

    @Test
    public void matchesHostVector() {
        byte[] opus = "gabs".getBytes(StandardCharsets.US_ASCII);
        assertEquals(HOST_VECTOR, hex(MicPacket.seal(7, opus, opus.length, testKey())));
    }

    @Test
    public void usesOnlyTheGivenLength() {
        byte[] opus = "gabsXYZ".getBytes(StandardCharsets.US_ASCII);
        assertEquals(HOST_VECTOR, hex(MicPacket.seal(7, opus, 4, testKey())));
    }
}
