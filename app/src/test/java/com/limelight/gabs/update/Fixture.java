package com.limelight.gabs.update;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Lê a fixture do manifesto (gerada por tools/release/gerar-fixture.ps1). */
final class Fixture {
    private Fixture() {}

    static byte[] bytes(String name) {
        try (InputStream in = Fixture.class.getResourceAsStream("/update/" + name)) {
            if (in == null) {
                throw new IllegalStateException("fixture ausente: " + name);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static String text(String name) {
        return new String(bytes(name), StandardCharsets.UTF_8);
    }

    static byte[] testKey() {
        return Base64.getDecoder().decode(text("chave-de-teste.pub.b64").trim());
    }

    static String validJson() {
        return "{\"schema\": 1, \"version\": \"0.4.1\", \"date\": \"2026-11-20\", \"notes\": \"x\","
                + " \"host\": {\"file\": \"GabsRemote-Setup-0.4.1.exe\", \"sha256\": \"" + repeat('a', 64) + "\", \"size\": 10, \"minApp\": \"0.4.0\"},"
                + " \"app\": {\"file\": \"GabsRemote-0.4.1.apk\", \"sha256\": \"" + repeat('b', 64) + "\", \"size\": 20, \"versionCode\": 4001, \"minHost\": \"0.4.0\"}}";
    }

    static String repeat(char c, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
