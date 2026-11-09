package com.limelight.gabs.wake;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class RemoteWakeTest {
    @Test
    public void parsesTheMethod() {
        assertEquals(RemoteWake.Method.ROTEADOR, RemoteWake.parse("ROTEADOR"));
        assertEquals(RemoteWake.Method.NENHUM, RemoteWake.parse("NENHUM"));
        assertEquals(RemoteWake.Method.NENHUM, RemoteWake.parse("xyz"));
        assertEquals(RemoteWake.Method.NENHUM, RemoteWake.parse(null));
    }

    @Test
    public void portFallsBackToTheRouterRule() {
        assertEquals(8080, RemoteWake.parsePort("8080"));
        assertEquals(9, RemoteWake.parsePort("9"));
        assertEquals(8080, RemoteWake.parsePort("0"));
        assertEquals(8080, RemoteWake.parsePort("70000"));
        assertEquals(8080, RemoteWake.parsePort("abc"));
        assertEquals(8080, RemoteWake.parsePort(null));
    }

    @Test
    public void homeIpPrefersThePresencePage() {
        assertEquals("191.32.66.196", RemoteWake.homeIpv4("191.32.66.196", "177.1.2.3"));
        assertEquals("177.1.2.3", RemoteWake.homeIpv4(null, "177.1.2.3"));
        assertNull(RemoteWake.homeIpv4(null, null));
        assertNull(RemoteWake.homeIpv4(null, "192.168.15.2")); // endereço de casa não serve de fora
    }

    @Test
    public void magicPacketHas16CopiesOfTheMac() {
        byte[] p = MagicPacket.build("18:c0:4d:6a:9b:2d");
        assertEquals(102, p.length);
        for (int i = 0; i < 6; i++) {
            assertEquals((byte) 0xFF, p[i]);
        }
        byte[] mac = {(byte) 0x18, (byte) 0xC0, (byte) 0x4D, (byte) 0x6A, (byte) 0x9B, (byte) 0x2D};
        for (int k = 0; k < 16; k++) {
            byte[] copy = new byte[6];
            System.arraycopy(p, 6 + k * 6, copy, 0, 6);
            assertArrayEquals(mac, copy);
        }
    }

    @Test
    public void invalidMacHasNoPacket() {
        assertNull(MagicPacket.build("00:00:00:00:00:00"));
        assertNull(MagicPacket.build("18:c0:4d"));
        assertNull(MagicPacket.build("zz:c0:4d:6a:9b:2d"));
        assertNull(MagicPacket.build(null));
    }

    @Test
    public void planExplainsWhyItCannotWake() {
        assertEquals(RemoteWake.Result.OFF, RemoteWake.plan(RemoteWake.Method.NENHUM, "18:c0:4d:6a:9b:2d", "191.32.66.196"));
        assertEquals(RemoteWake.Result.NO_MAC, RemoteWake.plan(RemoteWake.Method.ROTEADOR, null, "191.32.66.196"));
        assertEquals(RemoteWake.Result.NO_HOME_IP, RemoteWake.plan(RemoteWake.Method.ROTEADOR, "18:c0:4d:6a:9b:2d", null));
        assertEquals(RemoteWake.Result.SENT, RemoteWake.plan(RemoteWake.Method.ROTEADOR, "18:c0:4d:6a:9b:2d", "191.32.66.196"));
    }
}
