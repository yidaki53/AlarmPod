package de.danoeh.antennapod.storage.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

public class PodcastAlarmPreferences {
    private static final String TAG = "PodcastAlarmPreferences";

    public static final String PREF_ENABLED = "prefPodcastAlarmEnabled";
    public static final String PREF_FEED_ID = "prefPodcastAlarmFeedId";
    public static final String PREF_HOUR = "prefPodcastAlarmHour";
    public static final String PREF_MINUTE = "prefPodcastAlarmMinute";
    public static final String PREF_PREFETCH_ENABLED = "prefPodcastAlarmPrefetchEnabled";
    public static final String PREF_PREFETCH_MINUTES = "prefPodcastAlarmPrefetchMinutes";

    private static final int DEFAULT_HOUR = 7;
    private static final int DEFAULT_MINUTE = 0;
    private static final int DEFAULT_PREFETCH_MINUTES = 60;

    private static SharedPreferences prefs;

    public static void init(@NonNull Context context) {
        Log.d(TAG, "Creating new instance of PodcastAlarmPreferences");
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static boolean isEnabled() {
        return prefs.getBoolean(PREF_ENABLED, false);
    }

    public static void setEnabled(boolean enabled) {
        prefs.edit().putBoolean(PREF_ENABLED, enabled).apply();
    }

    public static long getSelectedFeedId() {
        return prefs.getLong(PREF_FEED_ID, -1L);
    }

    public static void setSelectedFeedId(long feedId) {
        prefs.edit().putLong(PREF_FEED_ID, feedId).apply();
    }

    public static boolean hasSelectedFeed() {
        return getSelectedFeedId() > 0;
    }

    public static int getHour() {
        return prefs.getInt(PREF_HOUR, DEFAULT_HOUR);
    }

    public static int getMinute() {
        return prefs.getInt(PREF_MINUTE, DEFAULT_MINUTE);
    }

    public static void setTime(int hourOfDay, int minute) {
        prefs.edit()
                .putInt(PREF_HOUR, hourOfDay)
                .putInt(PREF_MINUTE, minute)
                .apply();
    }

    public static boolean isPrefetchEnabled() {
        return prefs.getBoolean(PREF_PREFETCH_ENABLED, false);
    }

    public static int getPrefetchMinutes() {
        return Integer.parseInt(prefs.getString(PREF_PREFETCH_MINUTES, String.valueOf(DEFAULT_PREFETCH_MINUTES)));
    }
}