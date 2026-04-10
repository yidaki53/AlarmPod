package de.danoeh.antennapod.alarm;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

@RunWith(RobolectricTestRunner.class)
public class PodcastAlarmDownloadServiceTest {
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void shouldEnqueueImmediatePrefetchReturnsTrueForExactDownloadMode() {
        assertTrue(PodcastAlarmDownloadService.shouldEnqueueImmediatePrefetch(true, true, true, 42L));
    }

    @Test
    public void shouldEnqueueImmediatePrefetchReturnsFalseWhenFeedIsMissing() {
        assertFalse(PodcastAlarmDownloadService.shouldEnqueueImmediatePrefetch(true, true, true, -1L));
    }

    @Test
    public void onStartCommandWithoutTriggerIntentDoesNotStartTriggerPath() {
        PodcastAlarmDownloadService service = Robolectric.buildService(PodcastAlarmDownloadService.class)
                .create()
                .get();

        int result = service.onStartCommand(new Intent(context, PodcastAlarmDownloadService.class), 0, 1);

        assertEquals(PodcastAlarmDownloadService.START_NOT_STICKY, result);
    }
}