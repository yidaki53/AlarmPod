package de.danoeh.antennapod.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import de.danoeh.antennapod.storage.preferences.PodcastAlarmPreferences;

public class PodcastAlarmBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (PodcastAlarmPreferences.isEnabled()) {
            PodcastAlarmScheduler.schedule(context.getApplicationContext());
        }
    }
}