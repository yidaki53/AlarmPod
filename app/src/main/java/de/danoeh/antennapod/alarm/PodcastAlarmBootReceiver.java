package de.danoeh.antennapod.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import de.danoeh.antennapod.storage.preferences.PodcastAlarmPreferences;

public class PodcastAlarmBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) {
            return;
        }
        String action = intent.getAction();
        if (!shouldReschedule(action)) {
            return;
        }
        if (PodcastAlarmPreferences.isEnabled()) {
            PodcastAlarmScheduler.schedule(context.getApplicationContext());
        }
    }

    static boolean shouldReschedule(Intent intent) {
        if (intent == null) {
            return false;
        }
        return shouldReschedule(intent.getAction());
    }

    static boolean shouldReschedule(String action) {
        return Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action);
    }
}