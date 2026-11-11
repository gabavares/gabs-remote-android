package com.limelight.gabs.mic;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.NoiseSuppressor;

import com.limelight.LimeLog;

/**
 * Gabs Remote: grava o microfone do tablet, codifica em Opus e entrega ao {@link MicSender}.
 * Fonte VOICE_COMMUNICATION: o Android aplica o cancelamento de eco do aparelho (o som do PC
 * sai no alto-falante do tablet e não pode voltar pelo microfone).
 */
public final class MicCapture implements MicController.Capture {
    /** Configuração da gravação (testável sem Android). */
    public static final class Config {
        public final int source;
        public final int sampleRate;
        public final int channelMask;
        public final int encoding;
        public final int frameSamples;
        public final int bitrate;

        Config(int source, int sampleRate, int channelMask, int encoding, int frameSamples, int bitrate) {
            this.source = source;
            this.sampleRate = sampleRate;
            this.channelMask = channelMask;
            this.encoding = encoding;
            this.frameSamples = frameSamples;
            this.bitrate = bitrate;
        }
    }

    public interface ErrorListener {
        void onMicError(String message);
    }

    public static Config config() {
        return new Config(MediaRecorder.AudioSource.VOICE_COMMUNICATION, 48000,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, 960, 32000);
    }

    private final MicSender sender;
    private final ErrorListener errors;
    private Thread thread;
    private volatile boolean running;

    public MicCapture(MicSender sender, ErrorListener errors) {
        this.sender = sender;
        this.errors = errors;
    }

    @Override
    public synchronized void start() {
        if (thread != null) {
            return;
        }
        running = true;
        thread = new Thread(this::loop, "Gabs mic");
        thread.start();
    }

    @Override
    public synchronized void stop() {
        running = false;
        Thread t = thread;
        thread = null;
        if (t != null) {
            t.interrupt();
            try {
                t.join(500);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @SuppressLint("MissingPermission") // a tela da sessão pede RECORD_AUDIO antes de ligar
    private void loop() {
        Config c = config();
        int min = AudioRecord.getMinBufferSize(c.sampleRate, c.channelMask, c.encoding);
        int bufferBytes = Math.max(min, c.frameSamples * 2 * 4);
        AudioRecord rec = null;
        AcousticEchoCanceler aec = null;
        NoiseSuppressor ns = null;
        long enc = 0;
        try {
            rec = new AudioRecord(c.source, c.sampleRate, c.channelMask, c.encoding, bufferBytes);
            if (rec.getState() != AudioRecord.STATE_INITIALIZED) {
                errors.onMicError("Não foi possível abrir o microfone do tablet");
                return;
            }
            if (AcousticEchoCanceler.isAvailable()) {
                aec = AcousticEchoCanceler.create(rec.getAudioSessionId());
                if (aec != null) aec.setEnabled(true);
            }
            if (NoiseSuppressor.isAvailable()) {
                ns = NoiseSuppressor.create(rec.getAudioSessionId());
                if (ns != null) ns.setEnabled(true);
            }
            enc = OpusEncoderJni.create(c.bitrate);
            if (enc == 0) {
                errors.onMicError("Não foi possível iniciar o codificador do microfone");
                return;
            }
            rec.startRecording();
            LimeLog.info("Gabs Remote: microfone gravando (eco cancelado: " + (aec != null) + ")");
            short[] pcm = new short[c.frameSamples];
            byte[] out = new byte[1000];
            boolean netWarned = false;
            while (running) {
                int got = 0;
                while (running && got < pcm.length) {
                    int n = rec.read(pcm, got, pcm.length - got);
                    if (n < 0) {
                        errors.onMicError("O microfone do tablet parou (" + n + ")");
                        return;
                    }
                    got += n;
                }
                if (!running) break;
                int len = OpusEncoderJni.encode(enc, pcm, out);
                if (len <= 0) continue;
                try {
                    sender.send(out, len);
                } catch (Exception e) {
                    if (!netWarned) {
                        netWarned = true;
                        LimeLog.warning("Gabs Remote: microfone: falha ao mandar: " + e);
                    }
                }
            }
        } catch (RuntimeException e) {
            errors.onMicError("Microfone do tablet: " + e.getMessage());
        } finally {
            if (rec != null) {
                try {
                    rec.stop();
                } catch (IllegalStateException ignored) {
                }
                rec.release();
            }
            if (aec != null) aec.release();
            if (ns != null) ns.release();
            if (enc != 0) OpusEncoderJni.destroy(enc);
            LimeLog.info("Gabs Remote: microfone parado");
        }
    }
}
