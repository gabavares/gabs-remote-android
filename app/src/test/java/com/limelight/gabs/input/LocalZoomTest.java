package com.limelight.gabs.input;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LocalZoomTest {
    private static final float EPS = 0.001f;

    @Test
    public void limitaEm4x() {
        LocalZoom z = new LocalZoom();
        z.onScale(10f, 1400, 876);
        assertEquals(4.0f, z.scale(), EPS);
    }

    @Test
    public void naoDiminuiAbaixoDe1x() {
        LocalZoom z = new LocalZoom();
        z.onScale(0.1f, 1400, 876);
        assertEquals(1.0f, z.scale(), EPS);
    }

    @Test
    public void semZoomNaoHaTranslacao() {
        LocalZoom z = new LocalZoom();
        z.onScale(0.5f, 100, 100);
        z.onPan(300, 300);
        assertArrayEquals(new float[]{0f, 0f}, z.translation(2800, 1752), EPS);
    }

    @Test
    public void translacaoNaoMostraForaDaImagem() {
        LocalZoom z = new LocalZoom();
        z.onScale(2f, 1400, 876);
        z.onPan(100000, -100000);
        float[] t = z.translation(2800, 1752);
        assertTrue(t[0] >= -1400 && t[0] <= 1400);
        assertEquals(1400f, t[0], EPS);
        assertEquals(-876f, t[1], EPS);
    }

    @Test
    public void pincaNoCentroNaoDesloca() {
        LocalZoom z = new LocalZoom();
        z.onScale(2f, 1400, 876);
        assertArrayEquals(new float[]{0f, 0f}, z.translation(2800, 1752), EPS);
    }

    @Test
    public void pincaNoCantoMantemOPontoNoLugar() {
        LocalZoom z = new LocalZoom();
        // Foco no canto superior esquerdo: ele continua no mesmo lugar da tela.
        z.onScale(2f, 0, 0);
        assertArrayEquals(new float[]{1400f, 876f}, z.translation(2800, 1752), EPS);
    }

    @Test
    public void resetVoltaA1x() {
        LocalZoom z = new LocalZoom();
        z.onScale(3f, 10, 10);
        z.onPan(50, 50);
        z.reset();
        assertEquals(1.0f, z.scale(), EPS);
        assertArrayEquals(new float[]{0f, 0f}, z.translation(2800, 1752), EPS);
    }
}
