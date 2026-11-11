package com.limelight.gabs.mic;

import java.security.GeneralSecurityException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Gabs Remote: pacote UDP do microfone (decisão 0007; formato no plano da fase 5).
 * "GRM1" | seq (uint32 LE) | tag GCM (16) | quadro Opus cifrado com AES-128-GCM e a chave da sessão.
 * IV: seq LE nos bytes 0–3, zeros nos 4–9, 'C' e 'M' nos 10–11. O host (gabs/mic.cpp) confere igual.
 */
public final class MicPacket {
    public static final int HEADER = 24;
    public static final int TAG = 16;

    private MicPacket() {
    }

    public static byte[] seal(int seq, byte[] opus, int opusLen, SecretKey rikey) {
        byte[] iv = new byte[12];
        iv[0] = (byte) seq;
        iv[1] = (byte) (seq >>> 8);
        iv[2] = (byte) (seq >>> 16);
        iv[3] = (byte) (seq >>> 24);
        iv[10] = 'C';
        iv[11] = 'M';
        byte[] sealed;
        try {
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, rikey, new GCMParameterSpec(TAG * 8, iv));
            sealed = c.doFinal(opus, 0, opusLen); // cifra + tag
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
        byte[] out = new byte[HEADER + opusLen];
        out[0] = 'G';
        out[1] = 'R';
        out[2] = 'M';
        out[3] = '1';
        System.arraycopy(iv, 0, out, 4, 4);
        System.arraycopy(sealed, opusLen, out, 8, TAG);
        System.arraycopy(sealed, 0, out, HEADER, opusLen);
        return out;
    }
}
