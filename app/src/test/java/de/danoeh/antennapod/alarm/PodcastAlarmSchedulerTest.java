package de.danoeh.antennapod.alarm;

import android.app.AlarmManager;
import android.content.Context;

import androidx.preference.PreferenceManager;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.work.Configuration;
import androidx.work.WorkManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlarmManager;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.storage.preferences.PodcastAlarmPreferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class PodcastAlarmSchedulerTest {
    @Test
    public void getNextTriggerAtMillisKeepsTodayForFutureTime() {
        Calendar now = Calendar.getInstance();
        now.set(2026, Calendar.APRIL, 5, 8, 30, 0);
        now.set(Calendar.MILLISECOND, 0);

        long triggerAt = PodcastAlarmScheduler.getNextTriggerAtMillis(now.getTimeInMillis(), 9, 15);

        Calendar expected = (Calendar) now.clone();
        expected.set(Calendar.HOUR_OF_DAY, 9);
        expected.set(Calendar.MINUTE, 15);
        assertEquals(expected.getTimeInMillis(), triggerAt);
    }

    @Test
    public void getNextTriggerAtMillisMovesToTomorrowForPastTime() {
        Calendar now = Calendar.getInstance();
        now.set(2026, Calendar.APRIL, 5, 8, 30, 0);
        now.set(Calendar.MILLISECOND, 0);

        long triggerAt = PodcastAlarmScheduler.getNextTriggerAtMillis(now.getTimeInMillis(), 7, 45);

        Calendar expected = (Calendar) now.clone();
        expected.add(Calendar.DAY_OF_YEAR, 1);
        expected.set(Calendar.HOUR_OF_DAY, 7);
        expected.set(Calendar.MINUTE, 45);
        expected.set(Calendar.SECOND, 0);
        expected.set(Calendar.MILLISECOND, 0);
        assertEquals(expected.getTimeInMillis(), triggerAt);
    }

    @Test
    public void getNextTriggerAtMillisMovesToTomorrowWhenTimeMatchesNow() {
        Calendar now = Calendar.getInstance();
        now.set(2026, Calendar.APRIL, 5, 8, 30, 0);
        now.set(Calendar.MILLISECOND, 0);

        long triggerAt = PodcastAlarmScheduler.getNextTriggerAtMillis(now.getTimeInMillis(), 8, 30);

        Calendar expected = (Calendar) now.clone();
        expected.add(Calendar.DAY_OF_YEAR, 1);
        expected.set(Calendar.SECOND, 0);
        expected.set(Calendar.MILLISECOND, 0);
        assertEquals(expected.getTimeInMillis(), triggerAt);
    }

    @Test
    public void getPrefetchPlanReturnsNoneWhenPrefetchIsDisabled() {
        PodcastAlarmScheduler.PrefetchPlan prefetchPlan = PodcastAlarmScheduler.getPrefetchPlan(
                1_000L,
                5_000L,
                false,
                42L,
                false,
                30);

        assertFalse(prefetchPlan.schedulesWork());
        assertFalse(prefetchPlan.schedulesExactDownload());
    }

    @Test
    public void getPrefetchPlanReturnsLeadTimeWorkWithClampedDelay() {
        PodcastAlarmScheduler.PrefetchPlan prefetchPlan = PodcastAlarmScheduler.getPrefetchPlan(
                TimeUnit.MINUTES.toMillis(80),
                TimeUnit.MINUTES.toMillis(100),
                true,
                42L,
                false,
                30);

        assertTrue(prefetchPlan.schedulesWork());
        assertFalse(prefetchPlan.schedulesExactDownload());
        assertEquals(TimeUnit.MINUTES.toMillis(0), prefetchPlan.getInitialDelayMillis());
    }

    @Test
    public void getPrefetchPlanReturnsExactDownloadForExactTimeMode() {
        PodcastAlarmScheduler.PrefetchPlan prefetchPlan = PodcastAlarmScheduler.getPrefetchPlan(
                1_000L,
                5_000L,
                true,
                42L,
                true,
                30);

        assertFalse(prefetchPlan.schedulesWork());
        assertTrue(prefetchPlan.schedulesExactDownload());
    }

    @Test
    public void getPrefetchPlanReturnsNoneWhenFeedIsMissing() {
        PodcastAlarmScheduler.PrefetchPlan prefetchPlan = PodcastAlarmScheduler.getPrefetchPlan(
                1_000L,
                5_000L,
                true,
                0L,
                false,
                30);

        assertFalse(prefetchPlan.schedulesWork());
        assertFalse(prefetchPlan.schedulesExactDownload());
    }

    @Test
    @Config(sdk = android.os.Build.VERSION_CODES.R)
    public void scheduleCancelsStaleExactDownloadAlarmWhenSwitchingBackToLeadTimeMode() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        initializeWorkManager(context);
        PodcastAlarmPreferences.init(context);
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(PodcastAlarmPreferences.PREF_ENABLED, true)
                .putLong(PodcastAlarmPreferences.PREF_FEED_ID, 42L)
                .putInt(PodcastAlarmPreferences.PREF_HOUR, 8)
                .putInt(PodcastAlarmPreferences.PREF_MINUTE, 0)
                .putBoolean(PodcastAlarmPreferences.PREF_PREFETCH_ENABLED, true)
                .putString(PodcastAlarmPreferences.PREF_PREFETCH_MODE, PodcastAlarmPreferences.PREFETCH_MODE_EXACT_TIME)
                .putString(PodcastAlarmPreferences.PREF_PREFETCH_MINUTES, "30")
                .putInt(PodcastAlarmPreferences.PREF_DOWNLOAD_HOUR, 7)
                .putInt(PodcastAlarmPreferences.PREF_DOWNLOAD_MINUTE, 0)
                .apply();

        PodcastAlarmScheduler.schedule(context);

        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        ShadowAlarmManager shadowAlarmManager = Shadows.shadowOf(alarmManager);
        assertEquals(2, countScheduledServiceAlarms(shadowAlarmManager.getScheduledAlarms()));

        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(PodcastAlarmPreferences.PREF_PREFETCH_MODE, PodcastAlarmPreferences.PREFETCH_MODE_LEAD_TIME)
                .apply();

        PodcastAlarmScheduler.schedule(context);

        assertEquals(1, countScheduledServiceAlarms(shadowAlarmManager.getScheduledAlarms()));
    }

    private static void initializeWorkManager(Context context) {
        try {
            WorkManager.initialize(context, new Configuration.Builder().build());
        } catch (IllegalStateException ignored) {
        }
    }

    private static int countScheduledServiceAlarms(List<ShadowAlarmManager.ScheduledAlarm> alarms) {
        int count = 0;
        for (ShadowAlarmManager.ScheduledAlarm alarm : alarms) {
            if (alarm.operation != null) {
                count++;
            }
        }
        return count;
    }
}