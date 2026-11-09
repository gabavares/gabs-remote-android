package com.limelight.gabs.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class PresenceClientTest {
    private static final String CONTRATO = "{\"uuid\":\"0F3A6C2B-1D4E-4F5A-8B9C-0D1E2F3A4B5C\",\"porta\":47989,"
            + "\"ipv6\":[\"2804:1b3::5\"],\"tailscale\":[\"100.99.120.91\"],\"versao\":\"x\","
            + "\"ipv4\":\"177.10.20.30\",\"atualizado\":1700000000}";

    @Test
    public void parsesTheContract() {
        PresenceClient.Info info = PresenceClient.parse(CONTRATO);
        assertEquals("177.10.20.30", info.ipv4);
        assertEquals(1, info.ipv6.size());
        assertEquals("100.99.120.91", info.tailscale.get(0));
        assertEquals(47989, info.porta);
        assertEquals(1700000000L, info.atualizado);
    }

    @Test
    public void missingIpv4IsNull() {
        PresenceClient.Info info = PresenceClient.parse("{\"porta\":47989,\"ipv6\":[],\"tailscale\":[],\"atualizado\":1}");
        assertNull(info.ipv4);
    }

    @Test
    public void parseRejectsMalformed() {
        assertNull(PresenceClient.parse("{"));
        assertNull(PresenceClient.parse("{\"porta\":47989,\"ipv6\":\"2804::1\",\"tailscale\":[],\"atualizado\":1}"));
        assertNull(PresenceClient.parse("{\"porta\":47989,\"ipv6\":[],\"tailscale\":[],\"ipv4\":\"nao-e-ip\",\"atualizado\":1}"));
        assertNull(PresenceClient.parse(null));
    }
}
