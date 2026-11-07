package com.limelight.gabs.input;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

public class MenuAnchorTest {
    private static final float EPS = 0.001f;

    @Test
    public void anchorsAtTheCenterOfTheFingers() {
        float[] p = MenuAnchor.place(new float[]{100, 200, 300}, new float[]{400, 500, 600}, 2800, 1752);
        assertArrayEquals(new float[]{200, 500}, p, EPS);
    }

    @Test
    public void staysInsideTheScreen() {
        float[] p = MenuAnchor.place(new float[]{-50, -10, -30}, new float[]{1800, 1900, 2000}, 2800, 1752);
        assertArrayEquals(new float[]{0, 1751}, p, EPS);
    }

    @Test
    public void withoutFingersUsesTheCenterOfTheScreen() {
        float[] p = MenuAnchor.place(new float[0], new float[0], 2800, 1752);
        assertArrayEquals(new float[]{1400, 876}, p, EPS);
    }
}
