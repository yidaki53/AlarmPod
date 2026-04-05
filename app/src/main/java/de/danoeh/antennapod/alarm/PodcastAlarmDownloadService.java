package de.danoeh.antennapod.alarm;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.storage.preferences.PodcastAlarmPreferences;
import de.danoeh.antennapod.ui.notifications.NotificationUtils;

public class PodcastAlarmDownloadService extends Service {
    private static final String ACTION_TRIGGER = "de.danoeh.antennapod.intent.action.TRIGGER_PODCAST_ALARM_DOWNLOAD";
    private static final int NOTIFICATION_ID = 4004;

    public static void start(Context context) {
        ContextCompat.startForegroundService(context, createTriggerIntent(context));
    }

    static Intent createTriggerIntent(Context context) {
        return new Intent(context, PodcastAlarmDownloadService.class).setAction(ACTION_TRIGGER);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || !ACTION_TRIGGER.equals(intent.getAction())) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        startForeground(NOTIFICATION_ID, buildNotification());
        Thread worker = new Thread(() -> {
            try {
                PodcastAlarmScheduler.schedule(getApplicationContext());
                if (!PodcastAlarmPreferences.isEnabled()
                        || !PodcastAlarmPreferences.isPrefetchEnabled()
                        || !PodcastAlarmPreferences.isPrefetchAtExactTime()) {
                    return;
                }

                long feedId = PodcastAlarmPreferences.getSelectedFeedId();
                if (feedId <= 0) {
                    return;
                }

                PodcastAlarmScheduler.enqueuePrefetchWork(getApplicationContext(), feedId, 0L);
            } finally {
                stopForeground(STOP_FOREGROUND_REMOVE);
                stopSelf(startId);
            }
        }, "PodcastAlarmDownloadService");
        worker.start();
        return START_NOT_STICKY;
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, NotificationUtils.CHANNEL_ID_REFRESHING)
                .setSmallIcon(R.drawable.ic_notification_sync)
                .setContentTitle(getString(R.string.podcast_alarm_title))
                .setContentText(getString(R.string.podcast_alarm_download_notification_summary))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSilent(true)
                .build();
    }
}