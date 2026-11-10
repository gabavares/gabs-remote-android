package com.limelight.gabs.update;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CompatTest {
    @Test
    public void hostWithoutVersionConnectsAsBefore() {
        assertEquals(Compat.Result.OK, Compat.check("0.4.0", "0.4.0", null, null));
        assertEquals(Compat.Result.OK, Compat.check("0.4.0", "0.4.0", null, "0.9.0"));
    }

    @Test
    public void appTooOld() {
        assertEquals(Compat.Result.APP_TOO_OLD, Compat.check("0.4.0", "0.4.0", "0.5.0", "0.5.0"));
    }

    @Test
    public void hostTooOld() {
        assertEquals(Compat.Result.HOST_TOO_OLD, Compat.check("0.5.0", "0.5.0", "0.4.0", "0.4.0"));
    }

    @Test
    public void equalVersionsAreOk() {
        assertEquals(Compat.Result.OK, Compat.check("0.4.0", "0.4.0", "0.4.0", "0.4.0"));
        assertEquals(Compat.Result.OK, Compat.check("0.4.1", "0.4.0", "0.4.0", "0.4.0"));
    }

    @Test
    public void garbageHostVersionIsOk() {
        assertEquals(Compat.Result.OK, Compat.check("0.4.0", "0.4.0", "7.1.431.-1", "0.5.0"));
        assertEquals(Compat.Result.OK, Compat.check("0.4.0", "0.4.0", "", ""));
    }

    @Test
    public void hostWithoutMinAppOnlyChecksMinHost() {
        assertEquals(Compat.Result.OK, Compat.check("0.4.0", "0.4.0", "0.4.0", null));
        assertEquals(Compat.Result.HOST_TOO_OLD, Compat.check("0.4.0", "0.5.0", "0.4.0", null));
    }
}
