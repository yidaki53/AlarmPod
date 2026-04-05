package de.danoeh.antennapod.alarm;

import org.junit.Test;

import java.util.Calendar;

import static org.junit.Assert.assertEquals;

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
}