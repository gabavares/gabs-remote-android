package com.limelight.gabs.update;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

public class ManifestVerifierTest {
    @Test
    public void acceptsFixture() {
        assertTrue(ManifestVerifier.verify(Fixture.bytes("manifest.json"), Fixture.bytes("manifest.json.sig"), Fixture.testKey()));
    }

    @Test
    public void rejectsTampered() {
        assertFalse(ManifestVerifier.verify(Fixture.bytes("manifest-adulterado.json"), Fixture.bytes("manifest.json.sig"), Fixture.testKey()));
    }

    @Test
    public void rejectsOtherKey() {
        assertFalse(ManifestVerifier.verify(Fixture.bytes("manifest.json"), Fixture.bytes("manifest-outra-chave.json.sig"), Fixture.testKey()));
    }

    @Test
    public void rejectsBadSizes() {
        byte[] sig = Fixture.bytes("manifest.json.sig");
        assertFalse(ManifestVerifier.verify(Fixture.bytes("manifest.json"), Arrays.copyOf(sig, 63), Fixture.testKey()));
        assertFalse(ManifestVerifier.verify(Fixture.bytes("manifest.json"), sig, Arrays.copyOf(Fixture.testKey(), 31)));
        assertFalse(ManifestVerifier.verify(null, sig, Fixture.testKey()));
    }
}
