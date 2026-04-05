package de.danoeh.antennapod.alarm;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

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
}