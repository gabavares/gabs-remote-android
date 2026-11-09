package com.limelight.gabs.wake;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import com.limelight.R;
import com.limelight.nvstream.http.ComputerDetails;

import java.util.Locale;

/**
 * Gabs Remote: "Ligando o PC… mm:ss" até o PC responder (então chama {@code onOnline}) ou até
 * 3 minutos, quando desiste com uma mensagem clara.
 */
public final class WakeDialog {
    public interface OnOnline {
        void run(ComputerDetails pc);
    }

    private final Activity activity;
    private final String uuid;
    private final OnOnline onOnline;
    private final WakeFlow flow = new WakeFlow();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private AlertDialog dialog;
    private boolean finished;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (finished) {
                return;
            }
            if (flow.onPoll(false, SystemClock.elapsedRealtime()) == WakeFlow.Status.TIMEOUT) {
                finish();
                new AlertDialog.Builder(activity)
                        .setTitle(R.string.gr_wake_title)
                        .setMessage(R.string.gr_wake_timeout)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                return;
            }
            updateMessage();
            handler.postDelayed(this, 1000);
        }
    };

    public WakeDialog(Activity activity, String uuid, OnOnline onOnline) {
        this.activity = activity;
        this.uuid = uuid;
        this.onOnline = onOnline;
    }

    public void show() {
        flow.start(SystemClock.elapsedRealtime());
        dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.gr_wake_title)
                .setMessage("")
                .setNegativeButton(android.R.string.cancel, (d, w) -> finish())
                .setCancelable(false)
                .show();
        updateMessage();
        handler.postDelayed(tick, 1000);
    }

    public boolean isActive() {
        return !finished;
    }

    /** Chamar a cada atualização de PC da lista (thread da interface). */
    public void onComputerUpdated(ComputerDetails pc) {
        if (finished || pc.uuid == null || !pc.uuid.equals(uuid)) {
            return;
        }
        boolean online = pc.state == ComputerDetails.State.ONLINE;
        if (flow.onPoll(online, SystemClock.elapsedRealtime()) == WakeFlow.Status.ONLINE) {
            finish();
            onOnline.run(pc);
        }
    }

    private void updateMessage() {
        if (dialog == null) {
            return;
        }
        long s = flow.elapsedMs(SystemClock.elapsedRealtime()) / 1000;
        dialog.setMessage(activity.getString(R.string.gr_wake_message,
                String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60)));
    }

    public void finish() {
        finished = true;
        handler.removeCallbacks(tick);
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}
