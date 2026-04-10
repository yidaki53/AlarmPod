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
    public static final String PREF_PREFETCH_MODE = "prefPodcastAlarmPrefetchMode";
    public static final String PREF_PREFETCH_MINUTES = "prefPodcastAlarmPrefetchMinutes";
    public static final String PREF_DOWNLOAD_HOUR = "prefPodcastAlarmDownloadHour";
    public static final String PREF_DOWNLOAD_MINUTE = "prefPodcastAlarmDownloadMinute";
    public static final String PREF_LAST_STAGE = "prefPodcastAlarmLastStage";

    public static final String PREFETCH_MODE_LEAD_TIME = "lead_time";
    public static final String PREFETCH_MODE_EXACT_TIME = "exact_time";

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

    @NonNull
    public static String getPrefetchMode() {
        return prefs.getString(PREF_PREFETCH_MODE, PREFETCH_MODE_LEAD_TIME);
    }

    public static boolean isPrefetchAtExactTime() {
        return PREFETCH_MODE_EXACT_TIME.equals(getPrefetchMode());
    }

    public static int getPrefetchMinutes() {
        return Integer.parseInt(prefs.getString(PREF_PREFETCH_MINUTES, String.valueOf(DEFAULT_PREFETCH_MINUTES)));
    }

    public static int getDownloadHour() {
        return prefs.getInt(PREF_DOWNLOAD_HOUR, getHour());
    }

    public static int getDownloadMinute() {
        return prefs.getInt(PREF_DOWNLOAD_MINUTE, getMinute());
    }

    public static void setDownloadTime(int hourOfDay, int minute) {
        prefs.edit()
                .putInt(PREF_DOWNLOAD_HOUR, hourOfDay)
                .putInt(PREF_DOWNLOAD_MINUTE, minute)
                .apply();
    }

    @NonNull
    public static String getLastStage() {
        return prefs.getString(PREF_LAST_STAGE, "");
    }

    public static void setLastStage(@NonNull String stage) {
        prefs.edit().putString(PREF_LAST_STAGE, stage).apply();
    }
}