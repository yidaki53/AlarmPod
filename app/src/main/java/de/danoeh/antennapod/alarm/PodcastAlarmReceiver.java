package de.danoeh.antennapod.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class PodcastAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        PodcastAlarmExecutionService.start(context.getApplicationContext());
    }
}