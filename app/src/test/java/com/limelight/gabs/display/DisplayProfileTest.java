package com.limelight.gabs.display;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DisplayProfileTest {
    @Test
    public void nativoUsaATelaInteira() {
        assertArrayEquals(new int[]{2800, 1752}, DisplayProfile.resolve(DisplayProfile.NATIVO, 100, 2800, 1752));
    }

    @Test
    public void escala67EDoisTercosExatos() {
        assertArrayEquals(new int[]{1866, 1168}, DisplayProfile.resolve(DisplayProfile.NATIVO, 67, 2800, 1752));
    }

    @Test
    public void escala50ArredondaParaBaixoAtePar() {
        assertArrayEquals(new int[]{1400, 876}, DisplayProfile.resolve(DisplayProfile.NATIVO, 50, 2800, 1752));
    }

    @Test
    public void retratoViraPaisagem() {
        assertArrayEquals(new int[]{2800, 1752}, DisplayProfile.resolve(DisplayProfile.NATIVO, 100, 1752, 2800));
    }

    @Test
    public void perfisFixos() {
        assertArrayEquals(new int[]{1920, 1080}, DisplayProfile.resolve(DisplayProfile.FIXO_1080, 100, 2800, 1752));
        assertArrayEquals(new int[]{1280, 720}, DisplayProfile.resolve(DisplayProfile.FIXO_720, 50, 2800, 1752));
        assertArrayEquals(new int[]{2560, 1440}, DisplayProfile.resolve(DisplayProfile.ESPELHAR, 67, 2800, 1752));
    }

    @Test
    public void parametroDoLaunch() {
        assertEquals("mirror", DisplayProfile.ESPELHAR.launchParam());
        assertEquals("virtual", DisplayProfile.NATIVO.launchParam());
        assertEquals("virtual", DisplayProfile.FIXO_1080.launchParam());
        assertEquals("virtual", DisplayProfile.FIXO_720.launchParam());
    }

    @Test
    public void lePreferenciaComPadrao() {
        assertEquals(DisplayProfile.NATIVO, DisplayProfile.fromPreference(null));
        assertEquals(DisplayProfile.NATIVO, DisplayProfile.fromPreference("qualquer"));
        assertEquals(DisplayProfile.FIXO_720, DisplayProfile.fromPreference("FIXO_720"));
    }

    @Test
    public void escalaDoWindowsSoComTelaVirtual() {
        assertEquals("150", DisplayProfile.NATIVO.windowsScaleParam("150"));
        assertEquals("125", DisplayProfile.FIXO_1080.windowsScaleParam("125"));
        assertEquals(null, DisplayProfile.ESPELHAR.windowsScaleParam("150"));
        assertEquals(null, DisplayProfile.NATIVO.windowsScaleParam("0"));
        assertEquals(null, DisplayProfile.NATIVO.windowsScaleParam(null));
        assertEquals(null, DisplayProfile.NATIVO.windowsScaleParam("abc"));
    }
}
