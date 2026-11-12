package com.pumpkin.panel;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/** Start the panel (and the server if autoStart is on) after the phone boots. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        Store st = new Store(context);
        if (!st.autoStart()) return;
        Intent svc = new Intent(context, PanelService.class).setAction(PanelService.ACTION_BOOT);
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(svc);
        else context.startService(svc);
    }
}
