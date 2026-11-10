package com.limelight.gabs.update;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Confere a última Release do Gabs Remote: baixa o manifesto e a assinatura, confere com a chave
 * embutida e compara com a versão instalada (decisão 0006). Sem rede na thread principal.
 */
public final class UpdateChecker {
    public static final String MANIFEST_URL = "https://github.com/gabavares/gabs-remote/releases/latest/download/manifest.json";
    public static final String SIGNATURE_URL = MANIFEST_URL + ".sig";
    private static final String RELEASE_BASE = "https://github.com/gabavares/gabs-remote/releases/download/v";
    private static final int MAX_MANIFEST_BYTES = 1024 * 1024;

    /** Busca uma URL. Lança NotFoundException no 404. */
    public interface Fetcher {
        byte[] get(String url) throws IOException;
    }

    /** O GitHub respondeu 404: nenhuma Release publicada ainda. */
    public static final class NotFoundException extends IOException {
        public NotFoundException() {
            super("404");
        }
    }

    public static final class Result {
        public enum Kind { UP_TO_DATE, AVAILABLE, INVALID, FAILED, NOT_PUBLISHED }

        public final Kind kind;
        public final UpdateManifest manifest;

        Result(Kind kind, UpdateManifest manifest) {
            this.kind = kind;
            this.manifest = manifest;
        }
    }

    private final Fetcher fetcher;
    private final byte[] publicKeyRaw;
    private final Version installed;

    public UpdateChecker(Fetcher fetcher, byte[] publicKeyRaw, Version installed) {
        this.fetcher = fetcher;
        this.publicKeyRaw = publicKeyRaw;
        this.installed = installed;
    }

    /** Fetcher e chave de verdade, para a versão instalada do app. */
    public static UpdateChecker forApp(String installedVersion) {
        return new UpdateChecker(new HttpFetcher(), android.util.Base64.decode(UpdateKey.PUBLIC_KEY_BASE64, android.util.Base64.NO_WRAP),
                Version.parse(installedVersion));
    }

    public static String artifactUrl(Version version, String file) {
        return RELEASE_BASE + version + "/" + file;
    }

    public Result check() {
        byte[] json;
        byte[] sig;
        try {
            json = fetcher.get(MANIFEST_URL);
            sig = fetcher.get(SIGNATURE_URL);
        } catch (NotFoundException e) {
            return new Result(Result.Kind.NOT_PUBLISHED, null);
        } catch (IOException | RuntimeException e) {
            return new Result(Result.Kind.FAILED, null);
        }
        if (json == null || sig == null || json.length > MAX_MANIFEST_BYTES || !ManifestVerifier.verify(json, sig, publicKeyRaw)) {
            return new Result(Result.Kind.INVALID, null);
        }
        UpdateManifest m = UpdateManifest.parse(new String(json, StandardCharsets.UTF_8));
        if (m == null) {
            return new Result(Result.Kind.INVALID, null);
        }
        if (installed != null && m.version.compareTo(installed) <= 0) {
            return new Result(Result.Kind.UP_TO_DATE, m);
        }
        return new Result(Result.Kind.AVAILABLE, m);
    }

    /** OkHttp: só HTTPS, redirecionamento só para HTTPS, 15 s. */
    static final class HttpFetcher implements Fetcher {
        private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(false)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .callTimeout(30, TimeUnit.SECONDS)
                .build();

        @Override
        public byte[] get(String url) throws IOException {
            Request request = new Request.Builder().url(url).header("User-Agent", "GabsRemote-App").build();
            try (Response response = CLIENT.newCall(request).execute()) {
                if (response.code() == 404) {
                    throw new NotFoundException();
                }
                if (!response.isSuccessful() || !response.request().url().isHttps()) {
                    throw new IOException("HTTP " + response.code());
                }
                ResponseBody body = response.body();
                if (body == null) {
                    throw new IOException("sem corpo");
                }
                long len = body.contentLength();
                if (len > MAX_MANIFEST_BYTES) {
                    throw new IOException("grande demais");
                }
                return body.bytes();
            }
        }
    }
}
