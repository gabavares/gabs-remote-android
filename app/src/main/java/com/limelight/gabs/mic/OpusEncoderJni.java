package com.limelight.gabs.mic;

/**
 * Gabs Remote: codificador Opus do microfone (libopus do moonlight-core; jni/moonlight-core/gabsmic.c).
 * 48 kHz, mono, VOIP, FEC dentro da banda com perda esperada de 10%.
 */
public final class OpusEncoderJni {
    static {
        System.loadLibrary("moonlight-core");
    }

    private OpusEncoderJni() {
    }

    /** @return handle, ou 0 se falhar. */
    public static native long create(int bitrate);

    /** @return bytes escritos em {@code out}, ou negativo em erro. */
    public static native int encode(long handle, short[] pcm960, byte[] out);

    public static native void destroy(long handle);
}
