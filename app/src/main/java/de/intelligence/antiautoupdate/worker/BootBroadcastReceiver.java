package de.intelligence.antiautoupdate.worker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class BootBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            context.startForegroundService(new Intent(context, WorkerService.class));
        }
    }

}
