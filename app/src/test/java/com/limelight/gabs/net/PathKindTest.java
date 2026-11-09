package com.limelight.gabs.net;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.net.InetAddress;

public class PathKindTest {
    private static PathKind of(String ip) throws Exception {
        return PathKind.classify(InetAddress.getByName(ip));
    }

    @Test
    public void tailscaleAddresses() throws Exception {
        assertEquals(PathKind.TAILSCALE, of("100.99.120.91"));
        assertEquals(PathKind.TAILSCALE, of("fd7a:115c:a1e0::5"));
    }

    @Test
    public void homeAddresses() throws Exception {
        assertEquals(PathKind.LAN, of("192.168.15.10"));
        assertEquals(PathKind.LAN, of("10.0.0.2"));
        assertEquals(PathKind.LAN, of("fe80::1"));
        assertEquals(PathKind.LAN, of("fd00::1"));
    }

    @Test
    public void publicAddresses() throws Exception {
        assertEquals(PathKind.IPV6, of("2804:14d::1"));
        assertEquals(PathKind.IPV4_PUBLICO, of("177.10.20.30"));
    }

    @Test
    public void labelsInPortuguese() {
        assertEquals("Rede de casa", PathKind.LAN.label());
        assertEquals("Tailscale", PathKind.TAILSCALE.label());
    }

    @Test
    public void classifiesNamesWithoutDns() {
        assertEquals(PathKind.TAILSCALE, PathKind.classifyAddress("desktop-19h9ktg.tail740109.ts.net"));
        assertEquals(PathKind.MANUAL, PathKind.classifyAddress("pc.exemplo"));
        assertEquals(PathKind.LAN, PathKind.classifyAddress("192.168.15.2"));
        assertEquals(PathKind.TAILSCALE, PathKind.classifyAddress("100.99.120.91"));
        assertEquals(PathKind.IPV6, PathKind.classifyAddress("2804:1b3::5"));
    }
}
