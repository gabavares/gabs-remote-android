package com.limelight.gabs.input;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Gabs Remote: acompanha o formato do cursor do PC pelo /gabs/cursor do host (long poll).
 *
 * Cada pedido leva a versão que o app já tem; o host responde na hora se ela for velha, ou quando o
 * formato mudar, ou depois de 10 s. Host sem a rota (anterior à 0.4.1) = 404 = para; o app fica na seta.
 */
public final class CursorShapePoller implements Runnable {
    public static final long RETRY_MS = 1000;

    public interface Fetcher {
        /** @return o corpo da resposta ("versão formato"); FileNotFoundException = host sem a rota */
        String fetch(long knownVersion) throws IOException;
    }

    public interface Listener {
        void onShape(CursorShape shape);
    }

    public interface Sleeper {
        void sleep(long ms) throws InterruptedException;
    }

    private final Fetcher fetcher;
    private final Listener listener;
    private final Sleeper sleeper;
    private volatile boolean running = true;
    private long known;

    public CursorShapePoller(Fetcher fetcher, Listener listener, Sleeper sleeper) {
        this.fetcher = fetcher;
        this.listener = listener;
        this.sleeper = sleeper;
    }

    /** Um pedido. @return false para parar. */
    boolean step() {
        if (!running) {
            return false;
        }
        try {
            CursorShape.Reply reply = CursorShape.parseReply(fetcher.fetch(known));
            if (reply == null) {
                sleeper.sleep(RETRY_MS);
            } else if (reply.version != known) {
                known = reply.version;
                if (running) {
                    listener.onShape(reply.shape);
                }
            }
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            try {
                sleeper.sleep(RETRY_MS);
            } catch (InterruptedException ie) {
                return false;
            }
        } catch (InterruptedException e) {
            return false;
        }
        return running;
    }

    @Override
    public void run() {
        while (step()) {
            // segue
        }
    }

    public void stop() {
        running = false;
    }
}
