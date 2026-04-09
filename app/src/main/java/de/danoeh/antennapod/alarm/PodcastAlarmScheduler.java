package de.danoeh.antennapod.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.net.download.service.feed.FeedUpdateManagerImpl;
import de.danoeh.antennapod.net.download.service.feed.FeedUpdateWorker;
import de.danoeh.antennapod.storage.preferences.PodcastAlarmPreferences;

public final class PodcastAlarmScheduler {
    private static final int REQUEST_CODE_TRIGGER = 4001;
    private static final int REQUEST_CODE_DOWNLOAD_TRIGGER = 4002;
    private static final String UNIQUE_WORK_PREFETCH = "podcastAlarmPrefetch";

    private PodcastAlarmScheduler() {
    }

    public static boolean schedule(@NonNull Context context) {
        if (!PodcastAlarmPreferences.isEnabled() || !PodcastAlarmPreferences.hasSelectedFeed()) {
            cancel(context);
            return false;
        }
        if (!canScheduleExactAlarms(context)) {
            cancelPrefetch(context);
            return false;
        }
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        if (alarmManager == null) {
            return false;
        }

        long triggerAtMillis = getNextTriggerAtMillis(
                System.currentTimeMillis(),
                PodcastAlarmPreferences.getHour(),
                PodcastAlarmPreferences.getMinute());

        PendingIntent pendingIntent = getTriggerPendingIntent(context);
        cancelLegacyReceiverAlarm(context, alarmManager);
        alarmManager.cancel(pendingIntent);
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);

        schedulePrefetch(context, triggerAtMillis);
        return true;
    }

    public static void cancel(@NonNull Context context) {
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        if (alarmManager != null) {
            alarmManager.cancel(getTriggerPendingIntent(context));
            alarmManager.cancel(getDownloadTriggerPendingIntent(context));
            cancelLegacyReceiverAlarm(context, alarmManager);
        }
        cancelPrefetch(context);
    }

    public static boolean canScheduleExactAlarms(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true;
        }
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        return alarmManager != null && alarmManager.canScheduleExactAlarms();
    }

    public static long getNextPlaybackTriggerAtMillis(long nowMillis) {
        return getNextTriggerAtMillis(
                nowMillis,
                PodcastAlarmPreferences.getHour(),
                PodcastAlarmPreferences.getMinute());
    }

    public static long getNextDownloadTriggerAtMillis(long nowMillis) {
        return getNextTriggerAtMillis(
                nowMillis,
                PodcastAlarmPreferences.getDownloadHour(),
                PodcastAlarmPreferences.getDownloadMinute());
    }

    static long getNextTriggerAtMillis(long nowMillis, int hourOfDay, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(nowMillis);
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        if (calendar.getTimeInMillis() <= nowMillis) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        return calendar.getTimeInMillis();
    }

    static PrefetchPlan getPrefetchPlan(long nowMillis,
                                        long triggerAtMillis,
                                        boolean prefetchEnabled,
                                        long feedId,
                                        boolean exactTime,
                                        int prefetchMinutes) {
        if (!prefetchEnabled || feedId <= 0) {
            return PrefetchPlan.none();
        }
        if (exactTime) {
            return PrefetchPlan.exactDownload();
        }

        long prefetchAtMillis = triggerAtMillis - TimeUnit.MINUTES.toMillis(prefetchMinutes);
        long initialDelay = Math.max(0L, prefetchAtMillis - nowMillis);
        return PrefetchPlan.leadTime(initialDelay);
    }

    private static void schedulePrefetch(@NonNull Context context, long triggerAtMillis) {
        cancelPrefetch(context);
        cancelExactDownload(context);
        long feedId = PodcastAlarmPreferences.getSelectedFeedId();
        PrefetchPlan prefetchPlan = getPrefetchPlan(
                System.currentTimeMillis(),
                triggerAtMillis,
                PodcastAlarmPreferences.isPrefetchEnabled(),
                feedId,
                PodcastAlarmPreferences.isPrefetchAtExactTime(),
                PodcastAlarmPreferences.getPrefetchMinutes());
        if (prefetchPlan.schedulesExactDownload()) {
            scheduleExactDownload(context);
            return;
        }
        if (!prefetchPlan.schedulesWork()) {
            return;
        }

        enqueuePrefetchWork(context, feedId, prefetchPlan.getInitialDelayMillis());
    }

    static void enqueuePrefetchWork(@NonNull Context context, long feedId, long initialDelay) {
        OneTimeWorkRequest.Builder refreshWorkBuilder = new OneTimeWorkRequest.Builder(FeedUpdateWorker.class)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build());
        if (initialDelay == 0L) {
            refreshWorkBuilder.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST);
        }

        Data refreshData = new Data.Builder()
                .putLong(FeedUpdateManagerImpl.EXTRA_FEED_ID, feedId)
                .putBoolean(FeedUpdateManagerImpl.EXTRA_EVEN_ON_MOBILE, true)
                .putBoolean(FeedUpdateManagerImpl.EXTRA_MANUAL, true)
                .build();
        OneTimeWorkRequest refreshWork = refreshWorkBuilder
                .setInputData(refreshData)
                .build();

        OneTimeWorkRequest.Builder prefetchWorkBuilder =
                new OneTimeWorkRequest.Builder(PodcastAlarmPrefetchWorker.class)
                .setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build());
        if (initialDelay == 0L) {
            prefetchWorkBuilder.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST);
        }

        Data prefetchData = new Data.Builder()
                .putLong(PodcastAlarmPrefetchWorker.EXTRA_FEED_ID, feedId)
                .build();
        OneTimeWorkRequest prefetchWork = prefetchWorkBuilder
                .setInputData(prefetchData)
                .build();

        WorkManager.getInstance(context)
                .beginUniqueWork(UNIQUE_WORK_PREFETCH, ExistingWorkPolicy.REPLACE, refreshWork)
                .then(prefetchWork)
                .enqueue();
    }

    private static void scheduleExactDownload(@NonNull Context context) {
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        if (alarmManager == null) {
            return;
        }

        PendingIntent pendingIntent = getDownloadTriggerPendingIntent(context);
        alarmManager.cancel(pendingIntent);
        long downloadTriggerAtMillis = getNextDownloadTriggerAtMillis(System.currentTimeMillis());
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, downloadTriggerAtMillis, pendingIntent);
    }

    private static void cancelExactDownload(@NonNull Context context) {
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        if (alarmManager == null) {
            return;
        }
        alarmManager.cancel(getDownloadTriggerPendingIntent(context));
    }

    private static void cancelPrefetch(@NonNull Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_PREFETCH);
    }

    private static PendingIntent getTriggerPendingIntent(@NonNull Context context) {
        Intent intent = PodcastAlarmExecutionService.createTriggerIntent(context);
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            ? PendingIntent.getForegroundService(
                context,
                REQUEST_CODE_TRIGGER,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE)
            : PendingIntent.getService(
                context,
                REQUEST_CODE_TRIGGER,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent getDownloadTriggerPendingIntent(@NonNull Context context) {
        Intent intent = PodcastAlarmDownloadService.createTriggerIntent(context);
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            ? PendingIntent.getForegroundService(
                context,
                REQUEST_CODE_DOWNLOAD_TRIGGER,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE)
            : PendingIntent.getService(
                context,
                REQUEST_CODE_DOWNLOAD_TRIGGER,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static void cancelLegacyReceiverAlarm(@NonNull Context context, @NonNull AlarmManager alarmManager) {
        Intent legacyIntent = new Intent(context, PodcastAlarmReceiver.class);
        PendingIntent legacyPendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_TRIGGER,
                legacyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        alarmManager.cancel(legacyPendingIntent);
    }

    static final class PrefetchPlan {
        private final boolean scheduleExactDownload;
        private final long initialDelayMillis;

        private PrefetchPlan(boolean scheduleExactDownload, long initialDelayMillis) {
            this.scheduleExactDownload = scheduleExactDownload;
            this.initialDelayMillis = initialDelayMillis;
        }

        static PrefetchPlan none() {
            return new PrefetchPlan(false, -1L);
        }

        static PrefetchPlan exactDownload() {
            return new PrefetchPlan(true, -1L);
        }

        static PrefetchPlan leadTime(long initialDelayMillis) {
            return new PrefetchPlan(false, initialDelayMillis);
        }

        boolean schedulesExactDownload() {
            return scheduleExactDownload;
        }

        boolean schedulesWork() {
            return !scheduleExactDownload && initialDelayMillis >= 0L;
        }

        long getInitialDelayMillis() {
            return initialDelayMillis;
        }
    }
}