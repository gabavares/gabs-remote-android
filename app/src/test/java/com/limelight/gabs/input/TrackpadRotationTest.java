package com.limelight.gabs.input;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TrackpadRotationTest {
    private static final float EPS = 1e-6f;

    @Test
    public void casosRelatadosPeloGabNoBookCover() {
        // Arrastar para a esquerda chegava como "para cima"; com 90° volta a ser esquerda.
        assertArrayEquals(new float[]{-1f, 0f}, TrackpadRotation.rotate(90, 0f, -1f), EPS);
        // Arrastar para baixo chegava como "para a esquerda"; com 90° volta a ser baixo.
        assertArrayEquals(new float[]{0f, 1f}, TrackpadRotation.rotate(90, -1f, 0f), EPS);
    }

    @Test
    public void outrasRotacoes() {
        assertArrayEquals(new float[]{3f, 4f}, TrackpadRotation.rotate(0, 3f, 4f), EPS);
        assertArrayEquals(new float[]{-3f, -4f}, TrackpadRotation.rotate(180, 3f, 4f), EPS);
        assertArrayEquals(new float[]{-4f, 3f}, TrackpadRotation.rotate(270, 3f, 4f), EPS);
        // 270 desfaz 90
        float[] r = TrackpadRotation.rotate(90, 3f, 4f);
        assertArrayEquals(new float[]{3f, 4f}, TrackpadRotation.rotate(270, r[0], r[1]), EPS);
    }

    @Test
    public void preferencia() {
        assertEquals(90, TrackpadRotation.fromPreference(null));
        assertEquals(90, TrackpadRotation.fromPreference("abc"));
        assertEquals(0, TrackpadRotation.fromPreference("0"));
        assertEquals(270, TrackpadRotation.fromPreference("270"));
        assertEquals(90, TrackpadRotation.fromPreference("45"));
    }
}
