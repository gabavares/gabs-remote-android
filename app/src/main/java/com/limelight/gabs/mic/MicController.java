package com.limelight.gabs.mic;

import javax.crypto.SecretKey;

/**
 * Gabs Remote: estado do microfone na sessão (decisão 0007).
 * Começa mudo (a não ser com "Microfone ligado ao conectar"); em segundo plano a captura para
 * sem esquecer se estava ligado; na reconexão troca a chave e mantém o estado.
 */
public final class MicController {
    public interface Capture {
        void start();
        void stop();
    }

    public interface Sender {
        void resetKey(SecretKey key);
    }

    private final Capture capture;
    private final Sender sender;
    private boolean on;
    private boolean foreground = true;
    private boolean closed;
    private boolean running;

    public MicController(Capture capture, Sender sender, boolean onAtConnect) {
        this.capture = capture;
        this.sender = sender;
        this.on = onAtConnect;
        apply();
    }

    public synchronized boolean isOn() {
        return on;
    }

    public synchronized void toggle() {
        on = !on;
        apply();
    }

    public synchronized void onForeground() {
        foreground = true;
        apply();
    }

    public synchronized void onBackground() {
        foreground = false;
        apply();
    }

    public synchronized void onReconnect(SecretKey newKey) {
        sender.resetKey(newKey);
        apply();
    }

    public synchronized void close() {
        closed = true;
        apply();
    }

    private void apply() {
        boolean want = on && foreground && !closed;
        if (want && !running) {
            capture.start();
            running = true;
        } else if (!want && running) {
            capture.stop();
            running = false;
        }
    }
}
