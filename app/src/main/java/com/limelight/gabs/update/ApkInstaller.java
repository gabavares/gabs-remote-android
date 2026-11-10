package com.limelight.gabs.update;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.os.Build;
import android.widget.Toast;

import com.limelight.LimeLog;
import com.limelight.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Baixa o APK da Release, confere SHA-256, pacote e versionCode com o manifesto assinado e instala
 * por PackageInstaller (decisão 0006). O resultado chega no InstallResultReceiver.
 */
public final class ApkInstaller {
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(false)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.MINUTES)
            .build();

    private static volatile boolean running;

    private ApkInstaller() {}

    public static void downloadAndInstall(final Activity activity, final UpdateManifest manifest) {
        if (running) {
            return;
        }
        running = true;
        final Context app = activity.getApplicationContext();
        Toast.makeText(activity, app.getString(R.string.gr_update_downloading, manifest.version.toString()), Toast.LENGTH_LONG).show();
        new Thread(() -> {
            String error = null;
            try {
                File apk = download(app, manifest);
                checkPackage(app, apk, manifest);
                install(app, apk);
            } catch (IOException | RuntimeException e) {
                LimeLog.warning("Gabs Remote: atualização do app falhou: " + e);
                error = e.getMessage();
            } finally {
                running = false;
            }
            if (error != null) {
                final String msg = app.getString(R.string.gr_update_failed, error);
                activity.runOnUiThread(() -> Toast.makeText(app, msg, Toast.LENGTH_LONG).show());
            }
        }, "gabs-update").start();
    }

    private static File download(Context app, UpdateManifest m) throws IOException {
        File dir = new File(app.getCacheDir(), "updates");
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("sem pasta de cache");
        }
        File[] old = dir.listFiles();
        if (old != null) {
            for (File f : old) {
                //noinspection ResultOfMethodCallIgnored
                f.delete();
            }
        }
        File part = new File(dir, m.appFile + ".part");
        File apk = new File(dir, m.appFile);
        Request request = new Request.Builder().url(UpdateChecker.artifactUrl(m.version, m.appFile))
                .header("User-Agent", "GabsRemote-App").build();
        MessageDigest sha;
        try {
            sha = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
        long total = 0;
        try (Response response = CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful() || !response.request().url().isHttps()) {
                throw new IOException("HTTP " + response.code());
            }
            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("sem corpo");
            }
            try (InputStream in = body.byteStream(); OutputStream out = new FileOutputStream(part)) {
                byte[] buf = new byte[64 * 1024];
                int n;
                while ((n = in.read(buf)) > 0) {
                    total += n;
                    if (total > m.appSize) {
                        throw new IOException("arquivo maior que o do manifesto");
                    }
                    sha.update(buf, 0, n);
                    out.write(buf, 0, n);
                }
            }
        } catch (IOException e) {
            //noinspection ResultOfMethodCallIgnored
            part.delete();
            throw e;
        }
        if (total != m.appSize || !hex(sha.digest()).equals(m.appSha256)) {
            //noinspection ResultOfMethodCallIgnored
            part.delete();
            throw new IOException("o APK baixado não confere com o manifesto");
        }
        if (!part.renameTo(apk)) {
            throw new IOException("não consegui renomear o APK");
        }
        return apk;
    }

    @SuppressWarnings("deprecation")
    private static void checkPackage(Context app, File apk, UpdateManifest m) throws IOException {
        PackageInfo info = app.getPackageManager().getPackageArchiveInfo(apk.getPath(), 0);
        if (info == null || !app.getPackageName().equals(info.packageName)) {
            throw new IOException("o APK não é do Gabs Remote");
        }
        long code = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? info.getLongVersionCode() : info.versionCode;
        if (code != m.versionCode) {
            throw new IOException("versionCode " + code + " diferente do manifesto (" + m.versionCode + ")");
        }
    }

    private static void install(Context app, File apk) throws IOException {
        PackageInstaller installer = app.getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        params.setAppPackageName(app.getPackageName());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED);
        }
        int id = installer.createSession(params);
        try (PackageInstaller.Session session = installer.openSession(id)) {
            try (InputStream in = new FileInputStream(apk); OutputStream out = session.openWrite("base.apk", 0, apk.length())) {
                byte[] buf = new byte[64 * 1024];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                }
                session.fsync(out);
            }
            Intent intent = new Intent(app, InstallResultReceiver.class);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                flags |= PendingIntent.FLAG_MUTABLE;  // o sistema preenche o status
            }
            PendingIntent pending = PendingIntent.getBroadcast(app, id, intent, flags);
            session.commit(pending.getIntentSender());
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
