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
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.net.download.service.feed.FeedUpdateManagerImpl;
import de.danoeh.antennapod.net.download.service.feed.FeedUpdateWorker;
import de.danoeh.antennapod.storage.preferences.PodcastAlarmPreferences;

public final class PodcastAlarmScheduler {
    private static final int REQUEST_CODE_TRIGGER = 4001;
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

        long triggerAtMillis = getNextTriggerAtMillis(
                System.currentTimeMillis(),
                PodcastAlarmPreferences.getHour(),
                PodcastAlarmPreferences.getMinute());

        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        if (alarmManager == null) {
            return false;
        }

        PendingIntent pendingIntent = getTriggerPendingIntent(context);
        cancelLegacyReceiverAlarm(context, alarmManager);
        alarmManager.cancel(pendingIntent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        }

        schedulePrefetch(context, triggerAtMillis);
        return true;
    }

    public static void cancel(@NonNull Context context) {
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        if (alarmManager != null) {
            alarmManager.cancel(getTriggerPendingIntent(context));
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

    private static void schedulePrefetch(@NonNull Context context, long triggerAtMillis) {
        cancelPrefetch(context);
        if (!PodcastAlarmPreferences.isPrefetchEnabled()) {
            return;
        }

        long feedId = PodcastAlarmPreferences.getSelectedFeedId();
        if (feedId <= 0) {
            return;
        }

        long prefetchAtMillis = triggerAtMillis - TimeUnit.MINUTES.toMillis(PodcastAlarmPreferences.getPrefetchMinutes());
        long initialDelay = Math.max(0L, prefetchAtMillis - System.currentTimeMillis());

        Data refreshData = new Data.Builder()
                .putLong(FeedUpdateManagerImpl.EXTRA_FEED_ID, feedId)
                .putBoolean(FeedUpdateManagerImpl.EXTRA_EVEN_ON_MOBILE, true)
                .putBoolean(FeedUpdateManagerImpl.EXTRA_MANUAL, true)
                .build();
        OneTimeWorkRequest refreshWork = new OneTimeWorkRequest.Builder(FeedUpdateWorker.class)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setInputData(refreshData)
                .build();

        Data prefetchData = new Data.Builder()
                .putLong(PodcastAlarmPrefetchWorker.EXTRA_FEED_ID, feedId)
                .build();
        OneTimeWorkRequest prefetchWork = new OneTimeWorkRequest.Builder(PodcastAlarmPrefetchWorker.class)
                .setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setInputData(prefetchData)
                .build();

        WorkManager.getInstance(context)
                .beginUniqueWork(UNIQUE_WORK_PREFETCH, ExistingWorkPolicy.REPLACE, refreshWork)
                .then(prefetchWork)
                .enqueue();
    }

    private static void cancelPrefetch(@NonNull Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_PREFETCH);
    }

    private static PendingIntent getTriggerPendingIntent(@NonNull Context context) {
        Intent intent = PodcastAlarmExecutionService.createTriggerIntent(context);
        return PendingIntent.getForegroundService(
                context,
                REQUEST_CODE_TRIGGER,
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
}