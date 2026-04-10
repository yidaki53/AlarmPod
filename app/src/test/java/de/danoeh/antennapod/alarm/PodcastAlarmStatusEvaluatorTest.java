package de.danoeh.antennapod.alarm;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PodcastAlarmStatusEvaluatorTest {
    @Test
    public void playbackStatusIsScheduledWhenAlarmCanRun() {
        PodcastAlarmStatusEvaluator.ScheduleStatus status = PodcastAlarmStatusEvaluator.evaluatePlaybackStatus(
                true,
                true,
                true,
                1234L);

        assertEquals(PodcastAlarmStatusEvaluator.ScheduleState.SCHEDULED, status.getState());
        assertEquals(1234L, status.getTriggerAtMillis());
    }

    @Test
    public void playbackStatusRequiresSelectedPodcast() {
        PodcastAlarmStatusEvaluator.ScheduleStatus status = PodcastAlarmStatusEvaluator.evaluatePlaybackStatus(
                true,
                false,
                true,
                1234L);

        assertEquals(PodcastAlarmStatusEvaluator.ScheduleState.MISSING_PODCAST, status.getState());
    }

    @Test
    public void downloadStatusUsesLeadTimeStateWhenExactTimeIsDisabled() {
        PodcastAlarmStatusEvaluator.ScheduleStatus status = PodcastAlarmStatusEvaluator.evaluateDownloadStatus(
                true,
                true,
                true,
                false,
                true,
                1234L);

        assertEquals(PodcastAlarmStatusEvaluator.ScheduleState.LEAD_TIME_MODE, status.getState());
    }

    @Test
    public void downloadStatusIsScheduledForExactTimeMode() {
        PodcastAlarmStatusEvaluator.ScheduleStatus status = PodcastAlarmStatusEvaluator.evaluateDownloadStatus(
                true,
                true,
                true,
                true,
                true,
                5678L);

        assertEquals(PodcastAlarmStatusEvaluator.ScheduleState.SCHEDULED, status.getState());
        assertEquals(5678L, status.getTriggerAtMillis());
    }

    @Test
    public void exactAlarmRequirementIsPlaybackOnlyWithoutExactDownloadMode() {
        PodcastAlarmStatusEvaluator.ExactAlarmRequirement requirement =
                PodcastAlarmStatusEvaluator.evaluateExactAlarmRequirement(true, false, true, false);

        assertEquals(PodcastAlarmStatusEvaluator.ExactAlarmRequirement.PLAYBACK_ONLY, requirement);
    }

    @Test
    public void exactAlarmRequirementIncludesDownloadsForExactDownloadMode() {
        PodcastAlarmStatusEvaluator.ExactAlarmRequirement requirement =
                PodcastAlarmStatusEvaluator.evaluateExactAlarmRequirement(true, false, true, true);

        assertEquals(PodcastAlarmStatusEvaluator.ExactAlarmRequirement.PLAYBACK_AND_EXACT_DOWNLOAD, requirement);
    }

    @Test
    public void exactAlarmRequirementIsNotRequiredWhenPermissionExists() {
        PodcastAlarmStatusEvaluator.ExactAlarmRequirement requirement =
                PodcastAlarmStatusEvaluator.evaluateExactAlarmRequirement(true, true, true, true);

        assertEquals(PodcastAlarmStatusEvaluator.ExactAlarmRequirement.NOT_REQUIRED, requirement);
    }

    @Test
    public void exactAlarmRequirementIsNotRequiredWhenAlarmIsDisabled() {
        PodcastAlarmStatusEvaluator.ExactAlarmRequirement requirement =
                PodcastAlarmStatusEvaluator.evaluateExactAlarmRequirement(false, false, true, true);

        assertEquals(PodcastAlarmStatusEvaluator.ExactAlarmRequirement.NOT_REQUIRED, requirement);
    }

    @Test
    public void shouldShowExactAlarmPermissionRequiresAndroid12AndNeed() {
        boolean shouldShow = PodcastAlarmStatusEvaluator.evaluateShouldShowExactAlarmPermission(
                android.os.Build.VERSION_CODES.S,
                PodcastAlarmStatusEvaluator.ExactAlarmRequirement.PLAYBACK_ONLY);

        assertEquals(true, shouldShow);
    }

    @Test
    public void shouldShowExactAlarmPermissionIsHiddenWhenNotRequired() {
        boolean shouldShow = PodcastAlarmStatusEvaluator.evaluateShouldShowExactAlarmPermission(
                android.os.Build.VERSION_CODES.S,
                PodcastAlarmStatusEvaluator.ExactAlarmRequirement.NOT_REQUIRED);

        assertEquals(false, shouldShow);
    }

    @Test
    public void shouldShowExactAlarmPermissionIsHiddenBeforeAndroid12() {
        boolean shouldShow = PodcastAlarmStatusEvaluator.evaluateShouldShowExactAlarmPermission(
                android.os.Build.VERSION_CODES.R,
                PodcastAlarmStatusEvaluator.ExactAlarmRequirement.PLAYBACK_AND_EXACT_DOWNLOAD);

        assertEquals(false, shouldShow);
    }
}