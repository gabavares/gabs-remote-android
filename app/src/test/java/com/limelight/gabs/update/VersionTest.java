package com.limelight.gabs.update;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VersionTest {
    @Test
    public void ordersVersions() {
        assertTrue(Version.parse("0.4.1").compareTo(Version.parse("0.4.0")) > 0);
        assertTrue(Version.parse("0.10.0").compareTo(Version.parse("0.9.9")) > 0);
        assertEquals(0, Version.parse("1.2.3").compareTo(Version.parse("1.2.3")));
        assertEquals("0.4.1", Version.parse("0.4.1").toString());
        assertEquals(Version.parse("1.2.3"), Version.parse("1.2.3"));
    }

    @Test
    public void rejectsInvalid() {
        assertNull(Version.parse("v0.4.0"));
        assertNull(Version.parse("0.4"));
        assertNull(Version.parse("1.2.3-beta"));
        assertNull(Version.parse("1000.0.0"));
        assertNull(Version.parse(""));
        assertNull(Version.parse(null));
    }

    @Test
    public void versionCodeFormula() {
        assertEquals(4000, Version.parse("0.4.0").versionCode());
        assertEquals(1002003, Version.parse("1.2.3").versionCode());
    }
}
