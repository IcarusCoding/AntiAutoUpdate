package de.intelligence.antiautoupdate.worker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.time.Duration;

import com.google.common.io.ByteStreams;
import com.topjohnwu.superuser.ipc.RootService;
import com.topjohnwu.superuser.nio.ExtendedFile;
import com.topjohnwu.superuser.nio.FileSystemManager;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import androidx.annotation.Nullable;

import de.intelligence.antiautoupdate.AntiAutoUpdate;
import de.intelligence.antiautoupdate.ISystemRootService;
import de.intelligence.antiautoupdate.R;
import de.intelligence.antiautoupdate.activity.MainActivity;
import de.intelligence.antiautoupdate.persistence.ILocalDatabaseAccess;
import de.intelligence.antiautoupdate.persistence.LocalDatabaseAccess;
import de.intelligence.antiautoupdate.persistence.VendingDatabaseAccess;
import de.intelligence.antiautoupdate.root.SystemRootService;
import de.intelligence.antiautoupdate.root.SystemRootServiceConnection;

public final class WorkerService extends Service implements SystemRootServiceConnection.SystemRootServiceConnectionListener {

    private static final long SCHEDULE_DELAY_MS = Duration.ofSeconds(10).toMillis();
    private static final int NOTIFICATION_ID = 4242;

    @SuppressLint("SdCardPath")
    private static final String VENDING_DB_PATH = "/data/data/com.android.vending/databases/library.db";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Notification.Builder notificationBuilder;
    private NotificationManager notificationManager;
    private ILocalDatabaseAccess localDatabaseAccess;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.localDatabaseAccess = new LocalDatabaseAccess(this);
        final Intent notificationIntent = new Intent(this, MainActivity.class);
        final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        final NotificationChannel channel = new NotificationChannel("9898", "ChannelAAU",
                NotificationManager.IMPORTANCE_LOW);
        this.notificationManager = super.getSystemService(NotificationManager.class);
        this.notificationManager.createNotificationChannel(channel);
        this.notificationBuilder = new Notification.Builder(this, channel.getId())
                .setContentTitle(super.getString(R.string.notification_title))
                .setContentText(super.getString(R.string.notification_waiting_root))
                .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
                .setSmallIcon(R.drawable.ic_block)
                .setContentIntent(pendingIntent);
        final Notification notification = this.notificationBuilder.build();
        super.startForeground(NOTIFICATION_ID, notification);

        // init root service
        final Intent rootServiceIntent = new Intent(this, SystemRootService.class);
        RootService.bind(rootServiceIntent, new SystemRootServiceConnection(this));

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        this.handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onRootServiceConnected(ComponentName componentName, ISystemRootService systemRootService,
                                       FileSystemManager fileSystemManager) {
        this.updateNotificationText(R.string.notification_active_root);
        final Runnable task = new Runnable() {
            @Override
            public void run() {
                if (WorkerService.this.localDatabaseAccess.hasExclusionsOrChanges()) {
                    Log.i(AntiAutoUpdate.LOG_TAG, "Force updating vending database");
                    try {
                        systemRootService.forceStopPackage(AntiAutoUpdate.VENDING_PACKAGE);
                        // We need to copy the database, update it and move it back currently
                        final ExtendedFile vendingDbExtFile = fileSystemManager.getFile(VENDING_DB_PATH);
                        // 1. Move database to temp file
                        try (InputStream vendingDbExtFileIn = vendingDbExtFile.newInputStream()) {
                            File vendingDbFileTmp = null;
                            try {
                                vendingDbFileTmp = File.createTempFile("aau-vending-temp", ".db");
                                try (FileOutputStream vendingDbFileTmpOut = new FileOutputStream(vendingDbFileTmp)) {
                                    ByteStreams.copy(vendingDbExtFileIn, vendingDbFileTmpOut);
                                }
                                // 2.1 Initialize the database connection to our copy
                                try (VendingDatabaseAccess vendingDatabaseAccess = new VendingDatabaseAccess(vendingDbFileTmp)) {
                                    // 2.2 Get exclusions from app database and update vending copy
                                    WorkerService.this.localDatabaseAccess.getChangedAndExcluded().forEach((excluded, app) -> {
                                        if (excluded) {
                                            vendingDatabaseAccess.excludePackage(app);
                                        } else {
                                            vendingDatabaseAccess.allowPackage(app);
                                        }
                                    });
                                    // 2.3 Remove changed flag
                                    WorkerService.this.localDatabaseAccess.removeChanges();
                                    // 3. Move copy back
                                    final OutputStream vendingDbExtFileOut = vendingDbExtFile.newOutputStream();
                                    final FileInputStream vendingDbFileTmpIn = new FileInputStream(vendingDbFileTmp);
                                    try (vendingDbExtFileOut; vendingDbFileTmpIn) {
                                        ByteStreams.copy(vendingDbFileTmpIn, vendingDbExtFileOut);
                                    }
                                }
                            } finally {
                                if (vendingDbFileTmp != null) {
                                    Files.delete(vendingDbFileTmp.toPath());
                                }
                            }

                        }
                    } catch (RemoteException | IOException ex) {
                        Log.e(AntiAutoUpdate.LOG_TAG, "Failed to update database");
                    }
                } else {
                    Log.i(AntiAutoUpdate.LOG_TAG, "No exclusions found");
                }
                WorkerService.this.handler.postDelayed(this, SCHEDULE_DELAY_MS);
            }
        };
        this.handler.removeCallbacksAndMessages(null);
        this.handler.postDelayed(task, SCHEDULE_DELAY_MS);
    }

    @Override
    public void onRootServiceDisconnected(ComponentName componentName) {
        this.updateNotificationText(R.string.notification_disconnected_root);
        this.handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onRootServiceException(Throwable throwable) {
        this.updateNotificationText(R.string.notification_error_root);
        this.handler.removeCallbacksAndMessages(null);
    }

    private void updateNotificationText(int resourceId) {
        this.notificationBuilder.setContentText(super.getText(resourceId));
        this.notificationManager.notify(NOTIFICATION_ID, this.notificationBuilder.build());
    }

}
