package com.limelight.gabs.net;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;

/**
 * Gabs Remote: por onde o app chega ao PC, na ordem de preferência da spec (§5.2).
 * Fora de casa o caminho é o Tailscale (decisão 0005); IPv6 e IPv4 público ficam para o dia em que
 * houver porta aberta.
 */
public enum PathKind {
    LAN("Rede de casa"),
    IPV6("IPv6 direto"),
    IPV4_PUBLICO("IPv4 (porta aberta)"),
    TAILSCALE("Tailscale"),
    MANUAL("Endereço manual");

    private final String label;

    PathKind(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /**
     * Classifica sem consultar o DNS: endereço literal pelo {@link #classify}; nome do MagicDNS do
     * Tailscale ({@code *.ts.net}) é Tailscale; outros nomes são endereço manual.
     */
    public static PathKind classifyAddress(String address) {
        if (address == null || address.isEmpty()) {
            return MANUAL;
        }
        String a = address.toLowerCase(java.util.Locale.ROOT);
        boolean literal = a.indexOf(':') >= 0 || a.matches("[0-9.]+");
        if (!literal) {
            return a.endsWith(".ts.net") || a.endsWith(".ts.net.") ? TAILSCALE : MANUAL;
        }
        try {
            return classify(InetAddress.getByName(a)); // literal: sem DNS
        } catch (java.net.UnknownHostException e) {
            return MANUAL;
        }
    }

    public static PathKind classify(InetAddress a) {
        byte[] b = a.getAddress();
        if (a instanceof Inet4Address) {
            int b0 = b[0] & 0xFF, b1 = b[1] & 0xFF;
            if (b0 == 100 && (b1 & 0xC0) == 64) {
                return TAILSCALE; // 100.64.0.0/10
            }
            if (a.isSiteLocalAddress() || a.isLinkLocalAddress() || a.isLoopbackAddress()) {
                return LAN;
            }
            return IPV4_PUBLICO;
        }
        if (a instanceof Inet6Address) {
            int b0 = b[0] & 0xFF;
            if (b0 == 0xfd && (b[1] & 0xFF) == 0x7a && (b[2] & 0xFF) == 0x11 && (b[3] & 0xFF) == 0x5c
                    && (b[4] & 0xFF) == 0xa1 && (b[5] & 0xFF) == 0xe0) {
                return TAILSCALE; // fd7a:115c:a1e0::/48
            }
            if ((b0 & 0xFE) == 0xFC || a.isLinkLocalAddress() || a.isLoopbackAddress()) {
                return LAN; // fc00::/7, fe80::/10
            }
            return IPV6;
        }
        return MANUAL;
    }
}
