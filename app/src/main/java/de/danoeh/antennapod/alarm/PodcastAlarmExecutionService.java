package de.danoeh.antennapod.alarm;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.net.common.NetworkUtils;
import de.danoeh.antennapod.playback.service.PlaybackServiceStarter;
import de.danoeh.antennapod.storage.preferences.PodcastAlarmPreferences;
import de.danoeh.antennapod.ui.notifications.NotificationUtils;

public class PodcastAlarmExecutionService extends Service {
    private static final String ACTION_TRIGGER = "de.danoeh.antennapod.intent.action.TRIGGER_PODCAST_ALARM";
    private static final String PREF_LAST_STAGE = "prefPodcastAlarmLastStage";
    private static final int NOTIFICATION_ID = 4003;

    public static void start(Context context) {
        ContextCompat.startForegroundService(context, createTriggerIntent(context));
    }

    static Intent createTriggerIntent(Context context) {
        return new Intent(context, PodcastAlarmExecutionService.class).setAction(ACTION_TRIGGER);
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
        setLastStage("service-started");
        Thread worker = new Thread(() -> {
            try {
                setLastStage("scheduling-next");
                PodcastAlarmScheduler.schedule(getApplicationContext());
                setLastStage("scheduled-next");
                if (!PodcastAlarmPreferences.isEnabled()) {
                    setLastStage("disabled");
                    return;
                }

                setLastStage("resolving-episode");
                PodcastAlarmEpisodeResolver.Resolution resolution =
                        PodcastAlarmEpisodeResolver.resolve(PodcastAlarmPreferences.getSelectedFeedId());
                setLastStage("resolved-episode");
                FeedMedia mediaToPlay = chooseMedia(resolution);
                if (mediaToPlay == null) {
                    setLastStage("starting-fallback");
                    PodcastAlarmFallbackService.start(getApplicationContext());
                    setLastStage("fallback-started");
                    return;
                }

                boolean streamThisTime = !mediaToPlay.fileExists();
                setLastStage("starting-playback");
                new PlaybackServiceStarter(getApplicationContext(), mediaToPlay)
                        .callEvenIfRunning(true)
                        .shouldStreamThisTime(streamThisTime)
                        .start();
                setLastStage("playback-started");
            } catch (Throwable e) {
                setLastStage("failed:" + e.getClass().getSimpleName());
                throw e;
            } finally {
                stopForegroundCompat();
                stopSelf(startId);
                setLastStage("finished");
            }
        }, "PodcastAlarmExecutionService");
        worker.start();
        return START_NOT_STICKY;
    }

    private FeedMedia chooseMedia(PodcastAlarmEpisodeResolver.Resolution resolution) {
        FeedMedia preferred = resolution.getPreferredMedia();
        if (preferred == null) {
            FeedMedia fallback = resolution.getDownloadedFallback();
            return fallback != null && fallback.fileExists() ? fallback : null;
        }
        if (preferred.fileExists() || NetworkUtils.networkAvailable()) {
            return preferred;
        }
        FeedMedia fallback = resolution.getDownloadedFallback();
        return fallback != null && fallback.fileExists() ? fallback : null;
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, NotificationUtils.CHANNEL_ID_USER_ACTION)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.podcast_alarm_title))
                .setContentText(getString(R.string.podcast_alarm_summary))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSilent(true)
                .build();
    }

    private void setLastStage(String stage) {
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putString(PREF_LAST_STAGE, stage)
                .apply();
    }

    private void stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else {
            stopForeground(true);
        }
    }
}