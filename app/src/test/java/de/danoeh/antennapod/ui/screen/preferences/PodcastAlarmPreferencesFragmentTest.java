package de.danoeh.antennapod.ui.screen.preferences;

import android.content.Context;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import de.danoeh.antennapod.storage.preferences.PodcastAlarmPreferences;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class PodcastAlarmPreferencesFragmentTest {
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void exactDownloadModeShowsDownloadTimeAndHidesLeadTimePreference() {
        ListPreference prefetchMinutesPreference = new ListPreference(context);
        Preference downloadTimePreference = new Preference(context);

        PodcastAlarmPreferencesFragment.updatePrefetchConfigurationVisibility(
            prefetchMinutesPreference,
            downloadTimePreference,
            PodcastAlarmPreferences.PREFETCH_MODE_EXACT_TIME);

        assertFalse(prefetchMinutesPreference.isVisible());
        assertTrue(downloadTimePreference.isVisible());
    }

    @Test
    public void leadTimeModeShowsLeadTimePreferenceAndHidesDownloadTime() {
        ListPreference prefetchMinutesPreference = new ListPreference(context);
        Preference downloadTimePreference = new Preference(context);

        PodcastAlarmPreferencesFragment.updatePrefetchConfigurationVisibility(
            prefetchMinutesPreference,
            downloadTimePreference,
            PodcastAlarmPreferences.PREFETCH_MODE_LEAD_TIME);

        assertTrue(prefetchMinutesPreference.isVisible());
        assertFalse(downloadTimePreference.isVisible());
    }
}