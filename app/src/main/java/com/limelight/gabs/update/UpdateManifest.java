package com.limelight.gabs.update;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.regex.Pattern;

/**
 * Manifesto da Release (manifest.json), com as mesmas regras de validação do host e da ferramenta
 * de release (plano da fase 4; decisão 0006). Qualquer falha = manifesto inválido.
 */
public final class UpdateManifest {
    private static final Pattern FILE = Pattern.compile("^[A-Za-z0-9._-]+$");
    private static final Pattern SHA256 = Pattern.compile("^[0-9a-f]{64}$");

    public final Version version;
    public final String date;
    public final String notes;
    public final Version hostVersion;
    public final Version minApp;
    public final String appFile;
    public final String appSha256;
    public final long appSize;
    public final int versionCode;
    public final Version minHost;

    private UpdateManifest(Version version, String date, String notes, Version minApp, String appFile,
                           String appSha256, long appSize, int versionCode, Version minHost) {
        this.version = version;
        this.date = date;
        this.notes = notes;
        this.hostVersion = version;
        this.minApp = minApp;
        this.appFile = appFile;
        this.appSha256 = appSha256;
        this.appSize = appSize;
        this.versionCode = versionCode;
        this.minHost = minHost;
    }

    /** @return null se o JSON não seguir o formato. */
    public static UpdateManifest parse(String json) {
        if (json == null) {
            return null;
        }
        try {
            Object root = new JSONTokener(json).nextValue();
            if (!(root instanceof JSONObject)) {
                return null;
            }
            JSONObject doc = (JSONObject) root;
            if (!isInteger(doc.opt("schema")) || ((Number) doc.get("schema")).longValue() != 1) {
                return null;
            }
            Version version = Version.parse(string(doc, "version"));
            String date = string(doc, "date");
            String notes = string(doc, "notes");
            JSONObject host = doc.optJSONObject("host");
            JSONObject app = doc.optJSONObject("app");
            if (version == null || date == null || notes == null || host == null || app == null) {
                return null;
            }
            if (!validArtifact(host) || !validArtifact(app)) {
                return null;
            }
            Version minApp = Version.parse(string(host, "minApp"));
            Version minHost = Version.parse(string(app, "minHost"));
            Object code = app.opt("versionCode");
            if (minApp == null || minHost == null || !isInteger(code) || ((Number) code).longValue() != version.versionCode()) {
                return null;
            }
            return new UpdateManifest(version, date, notes, minApp, app.getString("file"), app.getString("sha256"),
                    ((Number) app.get("size")).longValue(), version.versionCode(), minHost);
        } catch (JSONException | ClassCastException e) {
            return null;
        }
    }

    private static boolean validArtifact(JSONObject a) {
        String file = string(a, "file");
        String sha = string(a, "sha256");
        Object size = a.opt("size");
        return file != null && FILE.matcher(file).matches() && !file.contains("..")
                && sha != null && SHA256.matcher(sha).matches()
                && isInteger(size) && ((Number) size).longValue() > 0;
    }

    private static String string(JSONObject o, String key) {
        Object v = o.opt(key);
        return v instanceof String ? (String) v : null;
    }

    private static boolean isInteger(Object v) {
        return v instanceof Integer || v instanceof Long;
    }
}
