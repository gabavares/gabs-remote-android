package com.limelight.gabs.wake;

/** Gabs Remote: pacote mágico do Wake-on-LAN (6 bytes 0xFF e o MAC 16 vezes). */
public final class MagicPacket {
    private MagicPacket() {
    }

    /** @return o pacote, ou null se o MAC for inválido ou zerado */
    public static byte[] build(String mac) {
        if (mac == null) {
            return null;
        }
        String[] parts = mac.split("[:-]");
        if (parts.length != 6) {
            return null;
        }
        byte[] m = new byte[6];
        boolean allZero = true;
        for (int i = 0; i < 6; i++) {
            if (!parts[i].matches("[0-9A-Fa-f]{2}")) {
                return null;
            }
            m[i] = (byte) Integer.parseInt(parts[i], 16);
            allZero &= m[i] == 0;
        }
        if (allZero) {
            return null;
        }
        byte[] p = new byte[6 + 16 * 6];
        for (int i = 0; i < 6; i++) {
            p[i] = (byte) 0xFF;
        }
        for (int k = 0; k < 16; k++) {
            System.arraycopy(m, 0, p, 6 + k * 6, 6);
        }
        return p;
    }
}
