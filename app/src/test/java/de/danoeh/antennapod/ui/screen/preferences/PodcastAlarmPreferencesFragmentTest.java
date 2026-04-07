package de.danoeh.antennapod.ui.screen.preferences;

import android.content.Context;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import de.danoeh.antennapod.alarm.PodcastAlarmStatusEvaluator;
import de.danoeh.antennapod.storage.preferences.PodcastAlarmPreferences;

import static org.junit.Assert.assertEquals;
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

    @Test
    public void getLastOutcomeSummaryDescribesPlaybackStarted() {
        CharSequence summary = PodcastAlarmPreferencesFragment.getLastOutcomeSummary(context, "playback-started");

        assertEquals(context.getString(de.danoeh.antennapod.R.string.podcast_alarm_last_outcome_playback_started), summary);
    }

    @Test
    public void getLastOutcomeSummaryDescribesFailures() {
        CharSequence summary = PodcastAlarmPreferencesFragment.getLastOutcomeSummary(context, "failed:IllegalStateException");

        assertEquals(
                context.getString(de.danoeh.antennapod.R.string.podcast_alarm_last_outcome_failed, "IllegalStateException"),
                summary);
    }

    @Test
    public void getScheduleSummaryDescribesLeadTimeMode() {
        CharSequence summary = PodcastAlarmPreferencesFragment.getScheduleSummary(
                context,
                PodcastAlarmStatusEvaluator.ScheduleStatus.of(
                        PodcastAlarmStatusEvaluator.ScheduleState.LEAD_TIME_MODE),
                de.danoeh.antennapod.R.string.podcast_alarm_next_download_disabled,
                de.danoeh.antennapod.R.string.podcast_alarm_next_download_missing_podcast,
                de.danoeh.antennapod.R.string.podcast_alarm_next_download_permission_needed,
                de.danoeh.antennapod.R.string.podcast_alarm_next_download_scheduled,
                de.danoeh.antennapod.R.string.podcast_alarm_next_download_prefetch_disabled,
                de.danoeh.antennapod.R.string.podcast_alarm_next_download_lead_time_mode);

        assertEquals(
                context.getString(de.danoeh.antennapod.R.string.podcast_alarm_next_download_lead_time_mode),
                summary);
    }

        @Test
        public void getExactAlarmPermissionSummaryDescribesPlaybackOnlyImpact() {
        CharSequence summary = PodcastAlarmPreferencesFragment.getExactAlarmPermissionSummary(
            context,
            PodcastAlarmStatusEvaluator.ExactAlarmRequirement.PLAYBACK_ONLY);

        assertEquals(
            context.getString(de.danoeh.antennapod.R.string.podcast_alarm_exact_alarm_summary_playback_only),
            summary);
        }

        @Test
        public void getExactAlarmPermissionSummaryDescribesPlaybackAndDownloadImpact() {
        CharSequence summary = PodcastAlarmPreferencesFragment.getExactAlarmPermissionSummary(
            context,
            PodcastAlarmStatusEvaluator.ExactAlarmRequirement.PLAYBACK_AND_EXACT_DOWNLOAD);

        assertEquals(
            context.getString(de.danoeh.antennapod.R.string.podcast_alarm_exact_alarm_summary_playback_and_download),
            summary);
        }
}