package com.limelight.gabs.update;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Versão do Gabs Remote, x.y.z com cada parte de 0 a 999 (decisão 0006). */
public final class Version implements Comparable<Version> {
    private static final Pattern PATTERN = Pattern.compile("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$");

    public final int major;
    public final int minor;
    public final int patch;

    private Version(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    /** @return null se o texto não for x.y.z. */
    public static Version parse(String text) {
        if (text == null) {
            return null;
        }
        Matcher m = PATTERN.matcher(text);
        if (!m.matches()) {
            return null;
        }
        return new Version(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
    }

    /** versionCode do APK: major·1000000 + minor·1000 + patch. */
    public int versionCode() {
        return major * 1000000 + minor * 1000 + patch;
    }

    @Override
    public int compareTo(Version o) {
        return Integer.compare(versionCode(), o.versionCode());
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Version && compareTo((Version) o) == 0;
    }

    @Override
    public int hashCode() {
        return versionCode();
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
