package com.pumpkin.panel;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

/** Foreground service: runs the HTTP API + Pumpkin process. */
public class PanelService extends Service {

    public static final String ACTION_BOOT = "com.pumpkin.panel.BOOT";
    public static final String ACTION_START = "com.pumpkin.panel.START";
    public static final String ACTION_STOP_SERVER = "com.pumpkin.panel.STOP_SERVER";
    public static final String ACTION_SHUTDOWN = "com.pumpkin.panel.SHUTDOWN";

    private static final String CHANNEL = "panel";
    private static final int NOTIF_ID = 1;

    private Store st;
    private Pumpkin pk;
    private ApiServer api;
    private Handler handler;
    private PowerManager.WakeLock wakeLock;
    private String lastNotifiedText;
    /** false = service stays alive (panel API) but without a notification. */
    private boolean notificationShown = true;

    @Override
    public void onCreate() {
        super.onCreate();
        st = new Store(this);
        pk = new Pumpkin(this, st);
        api = new ApiServer(this, st, pk);
        pk.setApi(api);

        createChannel();
        startForeground(NOTIF_ID, buildNotification("Menyiapkan panel..."));

        try {
            api.start();
            pk.pushLog("[panel] API aktif di " + st.host() + ":" + st.port(), "panel");
        } catch (Exception e) {
            pk.pushLog("[panel] gagal membuka port " + st.port() + ": " + e.getMessage(), "panel");
        }

        pk.primeLogs();
        pk.refreshWorldSizeAsync();

        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PumpkinPanel::server");
            wakeLock.setReferenceCounted(false);
        }

        handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(tick, 3000);

        if (st.autoStart()) {
            handler.postDelayed(() -> pk.start(), 1500);
        }
    }

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            try {
                api.broadcast("status", pk.status());
            } catch (Exception ignored) {
            }
            boolean running = pk.isRunning();
            applyWakeLock(running);
            if (running) updateNotification(true);
            else hideNotification();
            handler.postDelayed(this, 3000);
        }
    };

    private void applyWakeLock(boolean running) {
        if (wakeLock == null) return;
        try {
            if (running && !wakeLock.isHeld()) wakeLock.acquire();
            if (!running && wakeLock.isHeld()) wakeLock.release();
        } catch (Exception ignored) {
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_START : intent.getAction();
        if (ACTION_STOP_SERVER.equals(action)) {
            pk.stop(false);
        } else if (ACTION_SHUTDOWN.equals(action)) {
            pk.stop(true);
            stopSelf();
            return START_NOT_STICKY;
        } else if (ACTION_BOOT.equals(action) || ACTION_START.equals(action)) {
            if (intent != null && intent.getBooleanExtra("startServer", false) && !pk.isRunning()) {
                pk.start();
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (handler != null) handler.removeCallbacksAndMessages(null);
        try {
            api.stop();
        } catch (Exception ignored) {
        }
        try {
            if (pk.isRunning()) pk.stop(true);
        } catch (Exception ignored) {
        }
        applyWakeLock(false);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /* ---------------------------------------------------------- notification */

    private void createChannel() {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm == null) return;
        NotificationChannel ch = new NotificationChannel(CHANNEL, "Pumpkin Panel",
                NotificationManager.IMPORTANCE_LOW);
        ch.setDescription("Status server Pumpkin");
        ch.setShowBadge(false);
        nm.createNotificationChannel(ch);
    }

    private PendingIntent openIntent() {
        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getActivity(this, 0, i, flags);
    }

    private PendingIntent stopIntent() {
        Intent i = new Intent(this, PanelService.class);
        i.setAction(ACTION_STOP_SERVER);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getService(this, 1, i, flags);
    }

    private Notification buildNotification(String text) {
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            b = new Notification.Builder(this, CHANNEL);
        } else {
            b = new Notification.Builder(this);
        }
        b.setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(st.name())
                .setContentText(text)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(openIntent())
                .setPriority(Notification.PRIORITY_LOW);
        if (Build.VERSION.SDK_INT >= 24) {
            b.setColor(0xffff8a3d);
        }
        b.addAction(new Notification.Action.Builder(
                android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_notification),
                "Hentikan server", stopIntent()).build());
        return b.build();
    }

    /** Show/refresh the notification only while the server is running. */
    private void updateNotification(boolean running) {
        if (!running) {
            hideNotification();
            return;
        }
        org.json.JSONObject ports = pk.status().optJSONObject("ports");
        int javaPort = ports == null ? 25565 : ports.optInt("java", 25565);
        int bedrockPort = ports == null ? 19132 : ports.optInt("bedrock", 19132);
        String text = "Server berjalan · Java :" + javaPort + " · Bedrock :" + bedrockPort;

        if (!notificationShown) {
            // Server started again: return the service to the foreground.
            try {
                startForeground(NOTIF_ID, buildNotification(text));
                notificationShown = true;
                lastNotifiedText = text;
            } catch (Exception ignored) {
                notificationShown = false;
            }
            return;
        }
        if (text.equals(lastNotifiedText)) return;
        lastNotifiedText = text;
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(NOTIF_ID, buildNotification(text));
    }

    /**
     * Leave foreground mode and remove the notification. The service stays alive
     * so the panel API remains usable; the notification returns when the server starts.
     */
    private void hideNotification() {
        if (!notificationShown) return;
        notificationShown = false;
        lastNotifiedText = null;
        try {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } catch (Exception ignored) {
        }
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.cancel(NOTIF_ID);
    }
}
