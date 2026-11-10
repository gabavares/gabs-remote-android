package com.limelight.gabs.update;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class UpdateManifestTest {
    @Test
    public void parsesFixture() {
        UpdateManifest m = UpdateManifest.parse(Fixture.text("manifest.json"));
        assertNotNull(m);
        assertEquals("0.4.1", m.version.toString());
        assertEquals("0.4.1", m.hostVersion.toString());
        assertEquals(4001, m.versionCode);
        assertEquals("GabsRemote-0.4.1.apk", m.appFile);
        assertEquals(64, m.appSha256.length());
        assertTrue(m.appSize > 0);
        assertEquals("0.4.0", m.minHost.toString());
        assertEquals("0.4.0", m.minApp.toString());
        assertEquals("2026-11-20", m.date);
        assertTrue(m.notes.contains("atualização"));
    }

    @Test
    public void validJsonParses() {
        assertNotNull(UpdateManifest.parse(Fixture.validJson()));
    }

    @Test
    public void rejectsBadFields() {
        String base = Fixture.validJson();
        assertNull(UpdateManifest.parse(base.replace("\"GabsRemote-0.4.1.apk\"", "\"..\\\\x.apk\"")));
        assertNull(UpdateManifest.parse(base.replace("\"GabsRemote-Setup-0.4.1.exe\"", "\"a..exe\"")));
        assertNull(UpdateManifest.parse(base.replace(Fixture.repeat('b', 64), Fixture.repeat('B', 64))));
        assertNull(UpdateManifest.parse(base.replace(Fixture.repeat('a', 64), Fixture.repeat('a', 63))));
        assertNull(UpdateManifest.parse(base.replace("\"schema\": 1", "\"schema\": 2")));
        assertNull(UpdateManifest.parse(base.replace("\"versionCode\": 4001", "\"versionCode\": 4000")));
        assertNull(UpdateManifest.parse(base.replace("\"versionCode\": 4001", "\"versionCode\": \"4001\"")));
        assertNull(UpdateManifest.parse(base.replace("\"size\": 20", "\"size\": 0")));
        assertNull(UpdateManifest.parse(base.replace("\"minHost\": \"0.4.0\"", "\"minHost\": \"0.4\"")));
        assertNull(UpdateManifest.parse("{\"schema\": 1"));
        assertNull(UpdateManifest.parse("[]"));
        assertNull(UpdateManifest.parse(null));
    }
}
