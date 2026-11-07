package com.limelight.gabs.input;

import static org.junit.Assert.assertEquals;

import android.view.MotionEvent;

import com.limelight.gabs.input.TouchModePolicy.Action;

import org.junit.Test;

public class TouchModePolicyTest {
    @Test
    public void semCanetaCadaModoFazOSeu() {
        TouchModePolicy p = new TouchModePolicy();
        assertEquals(Action.NATIVE_TOUCH, p.fingerAction(TouchMode.DIRETO, 1, 0));
        assertEquals(Action.UPSTREAM_CONTEXT, p.fingerAction(TouchMode.TRACKPAD, 1, 0));
        assertEquals(Action.DROP, p.fingerAction(TouchMode.SO_CANETA, 1, 0));
        assertEquals(Action.GESTURE, p.fingerAction(TouchMode.SO_CANETA, 2, 0));
    }

    @Test
    public void canetaPertoDescartaODedo() {
        TouchModePolicy p = new TouchModePolicy();
        p.onPenEvent(MotionEvent.ACTION_HOVER_ENTER, 1000);
        assertEquals(Action.DROP, p.fingerAction(TouchMode.DIRETO, 1, 1100));
        assertEquals(Action.DROP, p.fingerAction(TouchMode.TRACKPAD, 1, 1100));
    }

    @Test
    public void canetaEncostadaTambemDescarta() {
        TouchModePolicy p = new TouchModePolicy();
        p.onPenEvent(MotionEvent.ACTION_DOWN, 1000);
        assertEquals(Action.DROP, p.fingerAction(TouchMode.DIRETO, 1, 5000));
    }

    @Test
    public void depoisQueACanetaSaiEspera300ms() {
        TouchModePolicy p = new TouchModePolicy();
        p.onPenEvent(MotionEvent.ACTION_HOVER_ENTER, 1000);
        p.onPenEvent(MotionEvent.ACTION_HOVER_EXIT, 2000);
        assertEquals(Action.DROP, p.fingerAction(TouchMode.DIRETO, 1, 2299));
        assertEquals(Action.NATIVE_TOUCH, p.fingerAction(TouchMode.DIRETO, 1, 2300));
    }

    @Test
    public void canetaLevantadaSemHoverContaComoSaida() {
        TouchModePolicy p = new TouchModePolicy();
        p.onPenEvent(MotionEvent.ACTION_DOWN, 1000);
        p.onPenEvent(MotionEvent.ACTION_UP, 1500);
        assertEquals(Action.DROP, p.fingerAction(TouchMode.DIRETO, 1, 1799));
        assertEquals(Action.NATIVE_TOUCH, p.fingerAction(TouchMode.DIRETO, 1, 1800));
    }

    @Test
    public void tresDedosSempreGesto() {
        TouchModePolicy p = new TouchModePolicy();
        for (TouchMode m : TouchMode.values()) {
            assertEquals(m.name(), Action.GESTURE, p.fingerAction(m, 3, 0));
        }
    }

    @Test
    public void modoLidoDaPreferencia() {
        assertEquals(TouchMode.DIRETO, TouchMode.fromPreference(null));
        assertEquals(TouchMode.DIRETO, TouchMode.fromPreference("xyz"));
        assertEquals(TouchMode.SO_CANETA, TouchMode.fromPreference("SO_CANETA"));
    }
}
