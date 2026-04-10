package de.danoeh.antennapod.alarm;

import android.content.Intent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class PodcastAlarmBootReceiverTest {

    @Test
    public void shouldRescheduleReturnsTrueForBootCompleted() {
        assertTrue(PodcastAlarmBootReceiver.shouldReschedule(Intent.ACTION_BOOT_COMPLETED));
    }

    @Test
    public void shouldRescheduleReturnsTrueForPackageReplaced() {
        assertTrue(PodcastAlarmBootReceiver.shouldReschedule(Intent.ACTION_MY_PACKAGE_REPLACED));
    }

    @Test
    public void shouldRescheduleReturnsTrueForTimeChanged() {
        assertTrue(PodcastAlarmBootReceiver.shouldReschedule(Intent.ACTION_TIME_CHANGED));
    }

    @Test
    public void shouldRescheduleReturnsTrueForTimezoneChanged() {
        assertTrue(PodcastAlarmBootReceiver.shouldReschedule(Intent.ACTION_TIMEZONE_CHANGED));
    }

    @Test
    public void shouldRescheduleReturnsFalseForUnknownAction() {
        assertFalse(PodcastAlarmBootReceiver.shouldReschedule("de.danoeh.antennapod.UNKNOWN"));
    }

    @Test
    public void shouldRescheduleReturnsFalseForNullAction() {
        assertFalse(PodcastAlarmBootReceiver.shouldReschedule((String) null));
    }

    @Test
    public void shouldRescheduleReturnsFalseForNullIntent() {
        assertFalse(PodcastAlarmBootReceiver.shouldReschedule((Intent) null));
    }
}