package com.limelight.gabs.latency;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.Random;

public class StampCodecTest {
    private static final int WHITE = 235, BLACK = 16;

    /** Mesmo padrão do lado C# para 0x89ABCDEF: calibração BPBPBPBP + bits do mais alto ao mais baixo. */
    private static int[] lumasFor(long t) {
        int[] l = new int[40];
        for (int c = 0; c < 8; c++) {
            l[c] = c % 2 == 0 ? WHITE : BLACK;
        }
        for (int bit = 0; bit < 32; bit++) {
            l[8 + bit] = ((t >> (31 - bit)) & 1) == 1 ? WHITE : BLACK;
        }
        return l;
    }

    @Test
    public void decodificaOVetorComum() {
        assertEquals(Long.valueOf(0x89ABCDEFL), StampCodec.decode(lumasFor(0x89ABCDEFL)));
    }

    @Test
    public void aguentaRuidoDe40() {
        Random r = new Random(7);
        int[] l = lumasFor(0x89ABCDEFL);
        for (int i = 0; i < l.length; i++) {
            l[i] += r.nextInt(81) - 40;
        }
        assertEquals(Long.valueOf(0x89ABCDEFL), StampCodec.decode(l));
    }

    @Test
    public void calibracaoFracaNaoDecodifica() {
        int[] l = lumasFor(0x89ABCDEFL);
        for (int c = 0; c < 8; c++) {
            l[c] = c % 2 == 0 ? 120 : 70; // diferença 50 < 64
        }
        assertNull(StampCodec.decode(l));
    }
}
