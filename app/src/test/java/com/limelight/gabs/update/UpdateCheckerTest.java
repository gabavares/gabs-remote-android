package com.limelight.gabs.update;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class UpdateCheckerTest {
    private static final class FakeFetcher implements UpdateChecker.Fetcher {
        final byte[] manifest;
        final byte[] signature;
        final boolean fail;
        final List<String> urls = new ArrayList<>();

        FakeFetcher(byte[] manifest, byte[] signature, boolean fail) {
            this.manifest = manifest;
            this.signature = signature;
            this.fail = fail;
        }

        @Override
        public byte[] get(String url) throws IOException {
            urls.add(url);
            if (fail) {
                throw new IOException("sem rede");
            }
            if (url.endsWith("/manifest.json")) {
                return manifest;
            }
            if (url.endsWith("/manifest.json.sig")) {
                return signature;
            }
            throw new IOException("url inesperada: " + url);
        }
    }

    private static UpdateChecker.Result check(FakeFetcher f, String installed) {
        return new UpdateChecker(f, Fixture.testKey(), Version.parse(installed)).check();
    }

    @Test
    public void newerIsAvailable() {
        FakeFetcher f = new FakeFetcher(Fixture.bytes("manifest.json"), Fixture.bytes("manifest.json.sig"), false);
        UpdateChecker.Result r = check(f, "0.4.0");
        assertEquals(UpdateChecker.Result.Kind.AVAILABLE, r.kind);
        assertNotNull(r.manifest);
        assertEquals("0.4.1", r.manifest.version.toString());
        assertEquals(UpdateChecker.MANIFEST_URL, f.urls.get(0));
        assertEquals("https://github.com/gabavares/gabs-remote/releases/latest/download/manifest.json", UpdateChecker.MANIFEST_URL);
        assertEquals("https://github.com/gabavares/gabs-remote/releases/download/v0.4.1/GabsRemote-0.4.1.apk",
                UpdateChecker.artifactUrl(r.manifest.version, r.manifest.appFile));
    }

    @Test
    public void ignoresOlderOrEqual() {
        FakeFetcher f = new FakeFetcher(Fixture.bytes("manifest.json"), Fixture.bytes("manifest.json.sig"), false);
        assertEquals(UpdateChecker.Result.Kind.UP_TO_DATE, check(f, "0.4.1").kind);
        assertEquals(UpdateChecker.Result.Kind.UP_TO_DATE, check(f, "0.5.0").kind);
    }

    @Test
    public void wrongSignatureIsInvalid() {
        FakeFetcher f = new FakeFetcher(Fixture.bytes("manifest.json"), Fixture.bytes("manifest-outra-chave.json.sig"), false);
        UpdateChecker.Result r = check(f, "0.4.0");
        assertEquals(UpdateChecker.Result.Kind.INVALID, r.kind);
        assertNull(r.manifest);
    }

    @Test
    public void tamperedIsInvalid() {
        FakeFetcher f = new FakeFetcher(Fixture.bytes("manifest-adulterado.json"), Fixture.bytes("manifest.json.sig"), false);
        assertEquals(UpdateChecker.Result.Kind.INVALID, check(f, "0.4.0").kind);
    }

    @Test
    public void networkErrorFails() {
        FakeFetcher f = new FakeFetcher(null, null, true);
        assertEquals(UpdateChecker.Result.Kind.FAILED, check(f, "0.4.0").kind);
    }

    @Test
    public void notPublishedYet() {
        UpdateChecker.Fetcher f = url -> {
            throw new UpdateChecker.NotFoundException();
        };
        assertEquals(UpdateChecker.Result.Kind.NOT_PUBLISHED, new UpdateChecker(f, Fixture.testKey(), Version.parse("0.4.0")).check().kind);
    }
}
