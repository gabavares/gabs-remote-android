package com.limelight.gabs;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * Gabs Remote: o que o host sabe fazer, pelo campo {@code GabsFeatures} do /serverinfo
 * (palavras separadas por espaço: mic, clipboard, files, actions). Host sem o campo = nenhuma.
 */
public final class GabsFeatures {
    public static final String MIC = "mic";
    public static final String CLIPBOARD = "clipboard";
    public static final String FILES = "files";
    public static final String ACTIONS = "actions";

    private GabsFeatures() {
    }

    public static Set<String> parse(String raw) {
        TreeSet<String> out = new TreeSet<>();
        if (raw != null) {
            for (String w : raw.trim().split("\\s+")) {
                if (!w.isEmpty()) {
                    out.add(w);
                }
            }
        }
        return Collections.unmodifiableSet(out);
    }

    public static String join(Set<String> features) {
        return features == null ? "" : String.join(" ", new TreeSet<>(features));
    }
}
