package com.limelight.gabs.latency;

/**
 * Gabs Remote: lê o carimbo de tempo que o marcador desenha na tela do PC (docs/latencia.md).
 *
 * São 40 células na ordem de leitura (8 colunas x 5 linhas): a linha 0 é a calibração
 * (pares brancas, ímpares pretas) e as linhas 1 a 4 trazem os 32 bits de t, do mais alto
 * para o mais baixo (branco = 1).
 */
public final class StampCodec {
    public static final int COLUMNS = 8, ROWS = 5, CELLS = COLUMNS * ROWS;
    /** Diferença mínima entre o branco e o preto da calibração para confiar na leitura. */
    static final int MIN_CONTRAST = 64;

    private StampCodec() {
    }

    /** @return t em milissegundos (0 a 2^32-1), ou null se a calibração não confere. */
    public static Long decode(int[] cellLuma) {
        if (cellLuma == null || cellLuma.length < CELLS) {
            return null;
        }
        double white = 0, black = 0;
        for (int c = 0; c < COLUMNS; c++) {
            if (c % 2 == 0) {
                white += cellLuma[c];
            } else {
                black += cellLuma[c];
            }
        }
        white /= COLUMNS / 2.0;
        black /= COLUMNS / 2.0;
        if (white - black < MIN_CONTRAST) {
            return null;
        }
        double threshold = (white + black) / 2;
        long t = 0;
        for (int bit = 0; bit < 32; bit++) {
            t = (t << 1) | (cellLuma[COLUMNS + bit] > threshold ? 1 : 0);
        }
        return t;
    }
}
