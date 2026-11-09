package com.limelight.gabs.net;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Gabs Remote: lê a página de presença na hospedagem do Gab (decisão 0005). O que importa é o
 * IPv4 público de casa, para o WoL de fora com o PC desligado.
 */
public final class PresenceClient {
    public static final int TIMEOUT_MS = 3000;
    private static final Pattern IPV4 = Pattern.compile("^((25[0-5]|2[0-4][0-9]|1?[0-9]?[0-9])[.]){3}(25[0-5]|2[0-4][0-9]|1?[0-9]?[0-9])$");

    public static final class Info {
        public final String ipv4;
        public final List<String> ipv6;
        public final List<String> tailscale;
        public final int porta;
        public final long atualizado;

        Info(String ipv4, List<String> ipv6, List<String> tailscale, int porta, long atualizado) {
            this.ipv4 = ipv4;
            this.ipv6 = ipv6;
            this.tailscale = tailscale;
            this.porta = porta;
            this.atualizado = atualizado;
        }
    }

    private PresenceClient() {
    }

    /** @return o registro, ou null se o JSON não seguir o contrato */
    public static Info parse(String json) {
        if (json == null) {
            return null;
        }
        try {
            JSONObject o = new JSONObject(json);
            String ipv4 = o.has("ipv4") ? o.getString("ipv4") : null;
            if (ipv4 != null && !IPV4.matcher(ipv4).matches()) {
                return null;
            }
            return new Info(ipv4, strings(o.getJSONArray("ipv6")), strings(o.getJSONArray("tailscale")),
                    o.getInt("porta"), o.getLong("atualizado"));
        } catch (JSONException e) {
            return null;
        }
    }

    private static List<String> strings(JSONArray a) throws JSONException {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < a.length(); i++) {
            out.add(a.getString(i));
        }
        return Collections.unmodifiableList(out);
    }

    /** Pede o registro do PC. Roda fora da thread da interface. @return null se falhar */
    public static Info fetch(String url, String token, String uuid) {
        if (url == null || url.isEmpty() || token == null || token.isEmpty() || uuid == null) {
            return null;
        }
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url + "?uuid=" + URLEncoder.encode(uuid, "UTF-8")).openConnection();
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestProperty("Authorization", "Bearer " + token);
            if (conn.getResponseCode() != 200) {
                return null;
            }
            try (InputStream in = conn.getInputStream()) {
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                byte[] chunk = new byte[1024];
                int n;
                while ((n = in.read(chunk)) > 0 && buf.size() < 8192) {
                    buf.write(chunk, 0, n);
                }
                return parse(buf.toString("UTF-8"));
            }
        } catch (IOException | RuntimeException e) {
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
