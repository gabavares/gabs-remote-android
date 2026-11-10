package com.limelight.gabs.update;

/**
 * Compatibilidade entre o app e o host antes de conectar (spec §8; decisão 0006). Host anterior à
 * fase 4 (sem GabsVersion no /serverinfo) conecta como antes.
 */
public final class Compat {
    public enum Result { OK, APP_TOO_OLD, HOST_TOO_OLD }

    private Compat() {}

    public static Result check(String appVersion, String appMinHost, String hostVersion, String hostMinApp) {
        Version host = Version.parse(hostVersion);
        Version app = Version.parse(appVersion);
        if (host == null || app == null) {
            return Result.OK;
        }
        Version minApp = Version.parse(hostMinApp);
        if (minApp != null && app.compareTo(minApp) < 0) {
            return Result.APP_TOO_OLD;
        }
        Version minHost = Version.parse(appMinHost);
        if (minHost != null && host.compareTo(minHost) < 0) {
            return Result.HOST_TOO_OLD;
        }
        return Result.OK;
    }
}
