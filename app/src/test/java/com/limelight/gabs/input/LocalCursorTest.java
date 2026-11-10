package com.limelight.gabs.input;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class LocalCursorTest {
    private static final float EPS = 1e-4f;

    private final List<int[]> sent = new ArrayList<>();
    private LocalCursor cursor;

    @Before
    public void setUp() {
        sent.clear();
        // Imagem do PC em 2800x1752.
        cursor = new LocalCursor(2800, 1752, (x, y, w, h) -> sent.add(new int[]{x, y, w, h}));
    }

    @Test
    public void comecaNoCentroEscondida() {
        assertFalse(cursor.visible(0));
        assertEquals(1400, cursor.x(), EPS);
        assertEquals(876, cursor.y(), EPS);
        assertTrue(sent.isEmpty());
    }

    @Test
    public void moverMandaPosicaoAbsolutaEMostra() {
        cursor.moveBy(10, -6, 0);
        assertEquals(1, sent.size());
        int[] p = sent.get(0);
        assertEquals(1410, p[0]);
        assertEquals(870, p[1]);
        assertEquals(2800, p[2]);
        assertEquals(1752, p[3]);
        assertTrue(cursor.visible(0));
    }

    @Test
    public void ficaDentroDaImagem() {
        cursor.moveBy(-5000, 5000, 0);
        int[] p = sent.get(0);
        assertEquals(0, p[0]);
        assertEquals(1751, p[1]);
        cursor.moveBy(9000, -9000, 0);
        p = sent.get(1);
        assertEquals(2799, p[0]);
        assertEquals(0, p[1]);
    }

    @Test
    public void deslocamentoFracionarioAcumula() {
        cursor.moveBy(0.4f, 0, 0);
        cursor.moveBy(0.4f, 0, 0);
        cursor.moveBy(0.4f, 0, 0);
        assertEquals(1401, sent.get(2)[0]);
    }

    @Test
    public void semAceleracaoRapidoTambemE1Para1() {
        cursor.moveBy(1, 0, 1000);
        cursor.moveBy(40, 0, 1001); // rápido: continua 1 para 1
        assertEquals(1441, sent.get(1)[0]);
    }

    @Test
    public void velocidadeMultiplica() {
        cursor.setSpeed(1.5f);
        cursor.moveBy(10, -4, 0);
        assertEquals(1415, sent.get(0)[0]);
        assertEquals(870, sent.get(0)[1]);
    }

    @Test
    public void velocidadeDaPreferencia() {
        assertEquals(1f, LocalCursor.speedFromPercent(100), 1e-6);
        assertEquals(0.5f, LocalCursor.speedFromPercent(50), 1e-6);
        assertEquals(LocalCursor.SPEED_MIN_PERCENT / 100f, LocalCursor.speedFromPercent(0), 1e-6);
        assertEquals(LocalCursor.SPEED_MAX_PERCENT / 100f, LocalCursor.speedFromPercent(9999), 1e-6);
    }

    @Test
    public void dedoNaTelaAndaOMesmoQueOCursorNaTela() {
        // Imagem de 2800 px mostrada em 1400 px de tela: 10 px de dedo = 20 px do PC = 10 px na tela.
        cursor.moveByScreen(10, 5, 1400, 876, 0);
        assertEquals(1420, sent.get(0)[0]);
        assertEquals(886, sent.get(0)[1]);
    }

    @Test
    public void dedoNaTelaComZoomContinua1Para1NaTela() {
        // Zoom 2x: a imagem aparece com 2800 px de tela; 10 px de dedo = 10 px do PC.
        cursor.moveByScreen(10, 0, 2800, 1752, 0);
        assertEquals(1410, sent.get(0)[0]);
    }

    @Test
    public void movimentosPequenosDoDedoNaoSePerdem() {
        // Imagem mostrada maior que o PC (zoom): 1 px de dedo = 0,25 px do PC; quatro somam 1.
        for (int i = 0; i < 4; i++) {
            cursor.moveByScreen(1, 0, 11200, 7008, 0);
        }
        assertEquals(1401, sent.get(3)[0]);
    }

    @Test
    public void semMovimentoNaoManda() {
        cursor.moveBy(0, 0, 0);
        assertTrue(sent.isEmpty());
        assertFalse(cursor.visible(0));
    }

    @Test
    public void toqueDiretoLevaASetaAoPontoEEsconde() {
        cursor.moveBy(5, 5, 0);
        cursor.onDirectInput(0.25f, 0.5f);
        assertFalse(cursor.visible(0));
        assertEquals(0.25f * 2799, cursor.x(), EPS);
        assertEquals(0.5f * 1751, cursor.y(), EPS);
        assertEquals(1, sent.size()); // o toque já foi ao PC pelo caminho dele
        cursor.moveBy(1, 0, 0);
        assertEquals(Math.round(0.25f * 2799 + 1), sent.get(1)[0]);
    }

    @Test
    public void pontoNaTelaSegueOTamanhoDaView() {
        cursor.moveBy(1400, 0, 0); // x = 2799 (borda direita)
        float[] p = cursor.viewPoint(1400, 876);
        assertEquals(1400 * 2799f / 2800f, p[0], EPS);
        // Rotação: a mesma posição normalizada cai no lugar certo da view nova.
        float[] r = cursor.viewPoint(876, 548);
        assertEquals(876 * 2799f / 2800f, r[0], EPS);
    }

    @Test
    public void formatoDoHost() {
        cursor.moveBy(1, 0, 0);
        assertEquals(CursorShape.ARROW, cursor.shape());
        cursor.onHostShape(CursorShape.IBEAM, 0);
        assertEquals(CursorShape.IBEAM, cursor.shape());
        assertTrue(cursor.visible(0));
    }

    @Test
    public void hostEscondeOCursor() {
        cursor.moveBy(1, 0, 0);
        cursor.onHostShape(CursorShape.HIDDEN, 1000);
        assertFalse(cursor.visible(1000));
    }

    @Test
    public void moverDepoisDeEscondidoMostraNaHoraComOUltimoFormato() {
        cursor.moveBy(1, 0, 0);
        cursor.onHostShape(CursorShape.HAND, 0);
        cursor.onHostShape(CursorShape.HIDDEN, 100);
        cursor.moveBy(1, 0, 1000);
        assertTrue(cursor.visible(1000));
        assertEquals(CursorShape.HAND, cursor.shape());
        // O host continua escondendo (jogo): depois da carência, vale o host.
        cursor.moveBy(1, 0, 1000 + LocalCursor.WAKE_GRACE_MS);
        assertFalse(cursor.visible(1000 + LocalCursor.WAKE_GRACE_MS));
    }

    @Test
    public void formatoNovoDoHostDepoisDeAcordarVale() {
        cursor.onHostShape(CursorShape.HIDDEN, 0);
        cursor.moveBy(1, 0, 1000);
        cursor.onHostShape(CursorShape.IBEAM, 1020);
        assertTrue(cursor.visible(5000));
        assertEquals(CursorShape.IBEAM, cursor.shape());
    }

    @Test
    public void formatoOutroDesenhaASeta() {
        cursor.moveBy(1, 0, 0);
        cursor.onHostShape(CursorShape.OTHER, 0);
        assertEquals(CursorShape.ARROW, cursor.drawnShape(0));
        cursor.onHostShape(CursorShape.HELP, 0);
        assertEquals(CursorShape.ARROW, cursor.drawnShape(0));
    }

    @Test
    public void escondidaNaoTemFormatoDesenhado() {
        assertNull(cursor.drawnShape(0));
    }

    @Test
    public void respostaDoHost() {
        CursorShape.Reply r = CursorShape.parseReply("7 ibeam");
        assertEquals(7, r.version);
        assertEquals(CursorShape.IBEAM, r.shape);
        assertEquals(CursorShape.SIZE_NWSE, CursorShape.parseReply("8 size_nwse\n").shape);
        assertEquals(CursorShape.APP_STARTING, CursorShape.parseReply("9 app_starting").shape);
        assertEquals(CursorShape.OTHER, CursorShape.parseReply("10 algo_novo").shape);
        assertNull(CursorShape.parseReply(""));
        assertNull(CursorShape.parseReply("x arrow"));
        assertNull(CursorShape.parseReply(null));
    }

    @Test
    public void parametroDoLaunch() {
        assertEquals("local", LocalCursor.launchParam(true));
        assertNull(LocalCursor.launchParam(false));
    }
}
