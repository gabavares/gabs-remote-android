package com.limelight.gabs.latency;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Choreographer;
import android.view.PixelCopy;
import android.view.SurfaceView;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Gabs Remote: mede a latência lendo o quadro decodificado da sessão (docs/latencia.md).
 *
 * O marcador no PC desenha, no canto superior esquerdo do monitor principal, um carimbo de
 * tempo (128x80 px) e um quadrado (x 128-191, y 0-63) que alterna preto/branco a cada F24.
 *
 * - Clique: manda F24 e mede até o quadrado mudar de cor na imagem (40 amostras).
 * - Carimbo: sincroniza o relógio com o marcador (UDP 48100) e compara o carimbo com a hora
 *   do tablet, a cada quadro, por 10 s. Inclui o tempo do PixelCopy.
 */
@TargetApi(Build.VERSION_CODES.N)
public class LatencyProbe {
    /** O que a medição precisa da sessão. */
    public interface Session {
        /** Manda F24 (descer e subir) pelo caminho de teclado do upstream. */
        void sendF24();
        /** Chamado na thread principal ao terminar. */
        void onFinished(String mode, LatencyStats stats, String error);
    }

    public static final int CLOCK_PORT = 48100;
    static final int CLICK_SAMPLES = 40;
    static final long CLICK_INTERVAL_MS = 400, CLICK_TIMEOUT_MS = 1000;
    static final long STAMP_DURATION_MS = 10_000;
    static final int CLOCK_PINGS = 20;

    private static final int STAMP_W = 128, STAMP_H = 80, CELL = 16, CLICK_X0 = 128, CLICK_X1 = 192, CLICK_H = 64;

    private final SurfaceView surface;
    private final int streamWidth, streamHeight;
    private final String hostAddress;
    private final Session session;
    private final Handler main = new Handler(Looper.getMainLooper());

    private HandlerThread copyThread;
    private Handler copyHandler;
    private boolean running;

    public LatencyProbe(SurfaceView surface, int streamWidth, int streamHeight, String hostAddress, Session session) {
        this.surface = surface;
        this.streamWidth = streamWidth;
        this.streamHeight = streamHeight;
        this.hostAddress = hostAddress;
        this.session = session;
    }

    public boolean isRunning() {
        return running;
    }

    // ---------- leitura da região do carimbo ----------

    /** Região do carimbo + quadrado (192x80 px do PC) no buffer da superfície. */
    private Rect sourceRect() {
        float fx = (float) surface.getWidth() / streamWidth;
        float fy = (float) surface.getHeight() / streamHeight;
        return new Rect(0, 0, Math.max(1, Math.round(CLICK_X1 * fx)), Math.max(1, Math.round(STAMP_H * fy)));
    }

    private interface FrameReader {
        /** Recebe a região copiada e a hora (elapsedRealtime) em que a cópia foi pedida. */
        void onFrame(Bitmap region, long requestedAtMs);
    }

    private void startCopyThread() {
        copyThread = new HandlerThread("GabsLatency");
        copyThread.start();
        copyHandler = new Handler(copyThread.getLooper());
    }

    private void stopCopyThread() {
        if (copyThread != null) {
            copyThread.quitSafely();
            copyThread = null;
        }
    }

    /** Pede uma cópia por quadro (Choreographer) enquanto running for true. */
    private void readEveryFrame(FrameReader reader) {
        final Rect src = sourceRect();
        Choreographer.getInstance().postFrameCallback(new Choreographer.FrameCallback() {
            private boolean busy;

            @Override
            public void doFrame(long frameTimeNanos) {
                if (!running) {
                    return;
                }
                if (!busy && surface.getHolder().getSurface().isValid()) {
                    busy = true;
                    final Bitmap bmp = Bitmap.createBitmap(src.width(), src.height(), Bitmap.Config.ARGB_8888);
                    final long requestedAt = SystemClock.elapsedRealtime();
                    PixelCopy.request(surface, src, bmp, result -> {
                        busy = false;
                        if (result == PixelCopy.SUCCESS && running) {
                            reader.onFrame(bmp, requestedAt);
                        }
                    }, copyHandler);
                }
                Choreographer.getInstance().postFrameCallback(this);
            }
        });
    }

    /** Luma média (0-255) de um retângulo em pixels do PC, convertido para o bitmap copiado. */
    private int luma(Bitmap bmp, int pcX0, int pcY0, int pcX1, int pcY1) {
        float fx = (float) bmp.getWidth() / CLICK_X1, fy = (float) bmp.getHeight() / STAMP_H;
        int x0 = Math.round(pcX0 * fx), x1 = Math.max(x0 + 1, Math.round(pcX1 * fx));
        int y0 = Math.round(pcY0 * fy), y1 = Math.max(y0 + 1, Math.round(pcY1 * fy));
        long sum = 0;
        int n = 0;
        for (int y = y0; y < y1 && y < bmp.getHeight(); y++) {
            for (int x = x0; x < x1 && x < bmp.getWidth(); x++) {
                int c = bmp.getPixel(x, y);
                sum += (299 * Color.red(c) + 587 * Color.green(c) + 114 * Color.blue(c)) / 1000;
                n++;
            }
        }
        return n == 0 ? 0 : (int) (sum / n);
    }

    /** Lumas das 40 células, lendo o miolo (8x8 px do PC) de cada uma para fugir das bordas. */
    private int[] stampLumas(Bitmap bmp) {
        int[] l = new int[StampCodec.CELLS];
        for (int r = 0; r < StampCodec.ROWS; r++) {
            for (int c = 0; c < StampCodec.COLUMNS; c++) {
                int x = c * CELL + 4, y = r * CELL + 4;
                l[r * StampCodec.COLUMNS + c] = luma(bmp, x, y, x + 8, y + 8);
            }
        }
        return l;
    }

    // ---------- modo Clique ----------

    public void startClick() {
        if (running) {
            return;
        }
        running = true;
        startCopyThread();
        final LatencyStats stats = new LatencyStats();
        final int[] sample = {0};
        final long[] t0 = {0};
        final boolean[] waiting = {false};
        final boolean[] whiteBefore = {false};
        final int[] lastLuma = {-1};

        readEveryFrame((bmp, at) -> {
            int l = luma(bmp, CLICK_X0 + 8, 8, CLICK_X1 - 8, CLICK_H - 8);
            lastLuma[0] = l;
            synchronized (stats) {
                if (waiting[0] && at >= t0[0]) {
                    boolean whiteNow = l > 128;
                    if (whiteNow != whiteBefore[0]) {
                        stats.add(at - t0[0]);
                        waiting[0] = false;
                    }
                }
            }
        });

        final long[] lastSend = {0};
        Runnable tick = new Runnable() {
            @Override
            public void run() {
                if (!running) {
                    return;
                }
                long now = SystemClock.elapsedRealtime();
                boolean send = false;
                synchronized (stats) {
                    if (waiting[0] && now - t0[0] > CLICK_TIMEOUT_MS) {
                        // A resposta não chegou a tempo.
                        stats.addLost();
                        waiting[0] = false;
                    }
                    if (!waiting[0]) {
                        if (sample[0] >= CLICK_SAMPLES) {
                            finish("clique", stats, null);
                            return;
                        }
                        if (lastLuma[0] >= 0 && now - lastSend[0] >= CLICK_INTERVAL_MS) {
                            whiteBefore[0] = lastLuma[0] > 128;
                            sample[0]++;
                            waiting[0] = true;
                            t0[0] = now;
                            lastSend[0] = now;
                            send = true;
                        }
                    }
                }
                if (send) {
                    session.sendF24();
                }
                main.postDelayed(this, 20);
            }
        };
        main.postDelayed(tick, 500);
    }

    // ---------- modo Carimbo ----------

    public void startStamp() {
        if (running) {
            return;
        }
        running = true;
        startCopyThread();
        new Thread(() -> {
            long offset;
            try {
                offset = syncClock();
            } catch (Exception e) {
                finish("carimbo", new LatencyStats(), "Sem resposta do marcador em " + hostAddress + ":" + CLOCK_PORT +
                        " (o marcador está aberto no PC?)");
                return;
            }
            final long clockOffset = offset;
            final LatencyStats stats = new LatencyStats();
            final long end = SystemClock.elapsedRealtime() + STAMP_DURATION_MS;
            main.post(() -> readEveryFrame((bmp, at) -> {
                Long t = StampCodec.decode(stampLumas(bmp));
                synchronized (stats) {
                    if (t == null) {
                        stats.addLost();
                    } else {
                        // O carimbo é uint32 em ms; o marcador roda há menos de 49 dias.
                        long hostNow = at + clockOffset;
                        long diff = (hostNow & 0xFFFFFFFFL) - t;
                        if (diff < 0) {
                            diff += 1L << 32;
                        }
                        stats.add(diff);
                    }
                }
                if (SystemClock.elapsedRealtime() >= end) {
                    finish("carimbo", stats, null);
                }
            }));
        }, "GabsClockSync").start();
    }

    /** 20 pings ao marcador; devolve horaDoMarcador - horaDoTablet. */
    private long syncClock() throws Exception {
        long[] t0 = new long[CLOCK_PINGS], host = new long[CLOCK_PINGS], t1 = new long[CLOCK_PINGS];
        int ok = 0;
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(500);
            InetAddress addr = InetAddress.getByName(hostAddress);
            byte[] reply = new byte[8];
            for (int i = 0; i < CLOCK_PINGS; i++) {
                long start = SystemClock.elapsedRealtime();
                socket.send(new DatagramPacket(new byte[]{0x01}, 1, addr, CLOCK_PORT));
                DatagramPacket p = new DatagramPacket(reply, reply.length);
                try {
                    socket.receive(p);
                } catch (java.net.SocketTimeoutException e) {
                    continue;
                }
                long stop = SystemClock.elapsedRealtime();
                if (p.getLength() != 8) {
                    continue;
                }
                t0[ok] = start;
                t1[ok] = stop;
                host[ok] = ByteBuffer.wrap(reply).order(ByteOrder.LITTLE_ENDIAN).getLong();
                ok++;
            }
        }
        if (ok == 0) {
            throw new java.io.IOException("sem resposta");
        }
        return ClockSync.offsetMs(java.util.Arrays.copyOf(t0, ok), java.util.Arrays.copyOf(host, ok), java.util.Arrays.copyOf(t1, ok));
    }

    // ---------- fim ----------

    private void finish(String mode, LatencyStats stats, String error) {
        main.post(() -> {
            if (!running) {
                return;
            }
            running = false;
            stopCopyThread();
            session.onFinished(mode, stats, error);
        });
    }

    public void cancel() {
        running = false;
        stopCopyThread();
    }
}
