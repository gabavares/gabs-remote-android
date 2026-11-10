package com.limelight.gabs.update;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import com.limelight.BuildConfig;
import com.limelight.R;

/**
 * Diálogo "Gabs Remote x.y.z disponível" e a conferência da Release em segundo plano (decisão 0006).
 * Só componentes padrão do Android: o layout próprio é da fase 7.
 */
public final class UpdateDialog {
    private static boolean checkedThisProcess;

    private UpdateDialog() {}

    public static void show(final Activity activity, final UpdateManifest manifest) {
        if (activity.isFinishing()) {
            return;
        }
        new AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.gr_update_available_title, manifest.version.toString()))
                .setMessage(manifest.notes)
                .setPositiveButton(R.string.gr_update_now, (d, w) -> startUpdate(activity, manifest))
                .setNegativeButton(R.string.gr_update_later, null)
                .show();
    }

    private static void startUpdate(Activity activity, UpdateManifest manifest) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !activity.getPackageManager().canRequestPackageInstalls()) {
            Toast.makeText(activity, R.string.gr_update_allow_source, Toast.LENGTH_LONG).show();
            activity.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + activity.getPackageName())));
            return;
        }
        ApkInstaller.downloadAndInstall(activity, manifest);
    }

    /** Ao abrir o app: confere uma vez por processo e só mostra algo se houver versão nova. */
    public static void checkOnStart(Activity activity) {
        if (checkedThisProcess) {
            return;
        }
        checkedThisProcess = true;
        check(activity, false);
    }

    /** "Procurar atualizações": sempre responde com um Toast ou o diálogo. */
    public static void checkManually(Activity activity) {
        check(activity, true);
    }

    private static void check(final Activity activity, final boolean manual) {
        new Thread(() -> {
            final UpdateChecker.Result r = UpdateChecker.forApp(BuildConfig.VERSION_NAME).check();
            activity.runOnUiThread(() -> {
                if (activity.isFinishing()) {
                    return;
                }
                if (r.kind == UpdateChecker.Result.Kind.AVAILABLE) {
                    show(activity, r.manifest);
                    return;
                }
                if (!manual) {
                    return;
                }
                int msg;
                switch (r.kind) {
                    case UP_TO_DATE:
                        Toast.makeText(activity, activity.getString(R.string.gr_update_up_to_date, BuildConfig.VERSION_NAME), Toast.LENGTH_LONG).show();
                        return;
                    case NOT_PUBLISHED:
                        msg = R.string.gr_update_not_published;
                        break;
                    case INVALID:
                        msg = R.string.gr_update_invalid;
                        break;
                    default:
                        msg = R.string.gr_update_no_connection;
                        break;
                }
                Toast.makeText(activity, msg, Toast.LENGTH_LONG).show();
            });
        }, "gabs-update-check").start();
    }
}
