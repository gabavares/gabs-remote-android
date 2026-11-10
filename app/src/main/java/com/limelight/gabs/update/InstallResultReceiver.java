package com.limelight.gabs.update;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.widget.Toast;

import com.limelight.LimeLog;
import com.limelight.R;

/** Resultado da instalação pelo PackageInstaller (decisão 0006). */
public class InstallResultReceiver extends BroadcastReceiver {
    @Override
    @SuppressWarnings("deprecation")
    public void onReceive(Context context, Intent intent) {
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        switch (status) {
            case PackageInstaller.STATUS_PENDING_USER_ACTION: {
                // Confirmação do sistema (primeira vez, ou quando o Android não dispensa o toque)
                Intent confirm = intent.getParcelableExtra(Intent.EXTRA_INTENT);
                if (confirm != null) {
                    confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(confirm);
                }
                break;
            }
            case PackageInstaller.STATUS_SUCCESS:
                LimeLog.info("Gabs Remote: app atualizado");
                break;
            default: {
                String msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
                LimeLog.warning("Gabs Remote: instalação falhou (" + status + "): " + msg);
                Toast.makeText(context, context.getString(R.string.gr_update_failed, msg != null ? msg : String.valueOf(status)),
                        Toast.LENGTH_LONG).show();
                break;
            }
        }
    }
}
