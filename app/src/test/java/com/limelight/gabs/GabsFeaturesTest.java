package com.limelight.gabs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;

public class GabsFeaturesTest {
    @Test
    public void parsesWordsInAnyOrder() {
        Set<String> f = GabsFeatures.parse("files  mic\tclipboard");
        assertTrue(f.contains("mic"));
        assertTrue(f.contains("clipboard"));
        assertTrue(f.contains("files"));
        assertFalse(f.contains("actions"));
        assertEquals(3, f.size());
    }

    @Test
    public void missingIsEmpty() {
        assertTrue(GabsFeatures.parse(null).isEmpty());
        assertTrue(GabsFeatures.parse("").isEmpty());
        assertTrue(GabsFeatures.parse("   ").isEmpty());
    }

    @Test
    public void joinRoundTrips() {
        assertEquals(GabsFeatures.parse("mic files"), GabsFeatures.parse(GabsFeatures.join(GabsFeatures.parse("files mic"))));
    }
}
