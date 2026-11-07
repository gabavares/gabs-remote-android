package com.limelight.gabs.input;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class KeyboardLayoutTest {
    @Test
    public void portuguesAbnt2PorPadrao() {
        assertEquals("00010416", KeyboardLayout.DEFAULT);
        assertEquals("00010416", KeyboardLayout.launchParam("00010416"));
    }

    @Test
    public void naoMudarNaoMandaParametro() {
        assertNull(KeyboardLayout.launchParam("0"));
        assertNull(KeyboardLayout.launchParam(""));
        assertNull(KeyboardLayout.launchParam(null));
    }

    @Test
    public void soAceitaKlidDeOitoDigitosHex() {
        assertEquals("00000409", KeyboardLayout.launchParam("00000409"));
        assertEquals("0001041A", KeyboardLayout.launchParam("0001041a"));
        assertNull(KeyboardLayout.launchParam("1041"));
        assertNull(KeyboardLayout.launchParam("0001041G"));
        assertNull(KeyboardLayout.launchParam("pt-BR&x=1"));
    }
}
