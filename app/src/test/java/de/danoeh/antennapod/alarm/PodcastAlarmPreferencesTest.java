package de.danoeh.antennapod.alarm;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import de.danoeh.antennapod.storage.preferences.PodcastAlarmPreferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class PodcastAlarmPreferencesTest {

    @Before
    public void setUp() {
        PodcastAlarmPreferences.init(InstrumentationRegistry.getInstrumentation().getTargetContext());
        PodcastAlarmPreferences.setEnabled(false);
        PodcastAlarmPreferences.setTime(7, 0);
        PodcastAlarmPreferences.setDownloadTime(7, 0);
        InstrumentationRegistry.getInstrumentation().getTargetContext()
                .getSharedPreferences("", 0);
        androidx.preference.PreferenceManager.getDefaultSharedPreferences(
                InstrumentationRegistry.getInstrumentation().getTargetContext())
                .edit()
                .putBoolean(PodcastAlarmPreferences.PREF_PREFETCH_ENABLED, false)
                .putString(PodcastAlarmPreferences.PREF_PREFETCH_MODE, PodcastAlarmPreferences.PREFETCH_MODE_LEAD_TIME)
                .putString(PodcastAlarmPreferences.PREF_PREFETCH_MINUTES, "60")
                .remove(PodcastAlarmPreferences.PREF_DOWNLOAD_HOUR)
                .remove(PodcastAlarmPreferences.PREF_DOWNLOAD_MINUTE)
                .apply();
    }

    @Test
    public void prefetchDefaultsToLeadTimeMode() {
        assertEquals(PodcastAlarmPreferences.PREFETCH_MODE_LEAD_TIME, PodcastAlarmPreferences.getPrefetchMode());
        assertFalse(PodcastAlarmPreferences.isPrefetchAtExactTime());
    }

    @Test
    public void downloadTimeDefaultsToAlarmTimeWhenUnset() {
        PodcastAlarmPreferences.setTime(6, 45);

        assertEquals(6, PodcastAlarmPreferences.getDownloadHour());
        assertEquals(45, PodcastAlarmPreferences.getDownloadMinute());
    }

    @Test
    public void exactDownloadModeAndTimePersist() {
        androidx.preference.PreferenceManager.getDefaultSharedPreferences(
                InstrumentationRegistry.getInstrumentation().getTargetContext())
                .edit()
                .putBoolean(PodcastAlarmPreferences.PREF_PREFETCH_ENABLED, true)
                .putString(PodcastAlarmPreferences.PREF_PREFETCH_MODE, PodcastAlarmPreferences.PREFETCH_MODE_EXACT_TIME)
                .apply();
        PodcastAlarmPreferences.setDownloadTime(5, 20);

        assertTrue(PodcastAlarmPreferences.isPrefetchEnabled());
        assertTrue(PodcastAlarmPreferences.isPrefetchAtExactTime());
        assertEquals(5, PodcastAlarmPreferences.getDownloadHour());
        assertEquals(20, PodcastAlarmPreferences.getDownloadMinute());
    }
}