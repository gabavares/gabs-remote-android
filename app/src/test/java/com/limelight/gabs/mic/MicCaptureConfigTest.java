package com.limelight.gabs.mic;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MicCaptureConfigTest {
    @Test
    public void usesVoiceCommunication() {
        MicCapture.Config c = MicCapture.config();
        assertEquals(android.media.MediaRecorder.AudioSource.VOICE_COMMUNICATION, c.source);
        assertEquals(48000, c.sampleRate);
        assertEquals(960, c.frameSamples);
        assertEquals(android.media.AudioFormat.CHANNEL_IN_MONO, c.channelMask);
        assertEquals(android.media.AudioFormat.ENCODING_PCM_16BIT, c.encoding);
        assertEquals(32000, c.bitrate);
    }
}
