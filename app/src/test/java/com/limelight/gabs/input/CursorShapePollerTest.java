package com.limelight.gabs.input;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class CursorShapePollerTest {
    private final Deque<Object> replies = new ArrayDeque<>();
    private final List<Long> asked = new ArrayList<>();
    private final List<CursorShape> shapes = new ArrayList<>();
    private final List<Long> sleeps = new ArrayList<>();

    private CursorShapePoller poller() {
        return new CursorShapePoller(known -> {
            asked.add(known);
            Object r = replies.removeFirst();
            if (r instanceof IOException) {
                throw (IOException) r;
            }
            return (String) r;
        }, shapes::add, sleeps::add);
    }

    @Test
    public void perguntaComAVersaoQueJaTem() {
        replies.add("1 arrow");
        replies.add("2 ibeam");
        CursorShapePoller p = poller();
        assertTrue(p.step());
        assertTrue(p.step());
        assertEquals(java.util.Arrays.asList(0L, 1L), asked);
        assertEquals(java.util.Arrays.asList(CursorShape.ARROW, CursorShape.IBEAM), shapes);
    }

    @Test
    public void mesmaVersaoNaoAvisa() {
        replies.add("3 hand");
        replies.add("3 hand"); // tempo esgotado no host
        CursorShapePoller p = poller();
        p.step();
        p.step();
        assertEquals(1, shapes.size());
        assertTrue(sleeps.isEmpty());
    }

    @Test
    public void hostAntigoSemARotaPara() {
        replies.add(new FileNotFoundException("404"));
        assertFalse(poller().step());
    }

    @Test
    public void erroDeRedeEsperaETentaDeNovo() {
        replies.add(new IOException("timeout"));
        CursorShapePoller p = poller();
        assertTrue(p.step());
        assertEquals(java.util.Arrays.asList(CursorShapePoller.RETRY_MS), sleeps);
    }

    @Test
    public void respostaEstranhaEsperaETentaDeNovo() {
        replies.add("<root/>");
        CursorShapePoller p = poller();
        assertTrue(p.step());
        assertEquals(1, sleeps.size());
        assertTrue(shapes.isEmpty());
    }

    @Test
    public void pararInterrompeOLaco() {
        replies.add("1 arrow");
        CursorShapePoller p = poller();
        p.stop();
        assertFalse(p.step());
    }
}
