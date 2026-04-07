package de.danoeh.antennapod.alarm;

import android.content.Context;

import androidx.annotation.NonNull;

import de.danoeh.antennapod.storage.preferences.PodcastAlarmPreferences;

public final class PodcastAlarmStatusEvaluator {
    private PodcastAlarmStatusEvaluator() {
    }

    @NonNull
    public static ScheduleStatus getPlaybackStatus(@NonNull Context context, long nowMillis) {
        return evaluatePlaybackStatus(
                PodcastAlarmPreferences.isEnabled(),
                PodcastAlarmPreferences.hasSelectedFeed(),
                PodcastAlarmScheduler.canScheduleExactAlarms(context),
                PodcastAlarmScheduler.getNextPlaybackTriggerAtMillis(nowMillis));
    }

    @NonNull
    public static ScheduleStatus getDownloadStatus(@NonNull Context context, long nowMillis) {
        boolean exactTime = PodcastAlarmPreferences.isPrefetchAtExactTime();
        return evaluateDownloadStatus(
                PodcastAlarmPreferences.isEnabled(),
                PodcastAlarmPreferences.hasSelectedFeed(),
                PodcastAlarmPreferences.isPrefetchEnabled(),
                exactTime,
                PodcastAlarmScheduler.canScheduleExactAlarms(context),
                exactTime ? PodcastAlarmScheduler.getNextDownloadTriggerAtMillis(nowMillis) : -1L);
    }

    @NonNull
    public static ExactAlarmRequirement getExactAlarmRequirement(@NonNull Context context) {
        return evaluateExactAlarmRequirement(
                PodcastAlarmScheduler.canScheduleExactAlarms(context),
                PodcastAlarmPreferences.isPrefetchEnabled(),
                PodcastAlarmPreferences.isPrefetchAtExactTime());
    }

    @NonNull
    static ScheduleStatus evaluatePlaybackStatus(boolean enabled,
                                                 boolean hasSelectedFeed,
                                                 boolean canScheduleExactAlarms,
                                                 long triggerAtMillis) {
        if (!enabled) {
            return ScheduleStatus.of(ScheduleState.DISABLED);
        }
        if (!hasSelectedFeed) {
            return ScheduleStatus.of(ScheduleState.MISSING_PODCAST);
        }
        if (!canScheduleExactAlarms) {
            return ScheduleStatus.of(ScheduleState.EXACT_ALARM_PERMISSION_REQUIRED);
        }
        return ScheduleStatus.scheduled(triggerAtMillis);
    }

    @NonNull
    static ScheduleStatus evaluateDownloadStatus(boolean enabled,
                                                 boolean hasSelectedFeed,
                                                 boolean prefetchEnabled,
                                                 boolean exactTime,
                                                 boolean canScheduleExactAlarms,
                                                 long triggerAtMillis) {
        if (!enabled) {
            return ScheduleStatus.of(ScheduleState.DISABLED);
        }
        if (!hasSelectedFeed) {
            return ScheduleStatus.of(ScheduleState.MISSING_PODCAST);
        }
        if (!prefetchEnabled) {
            return ScheduleStatus.of(ScheduleState.AUTOMATIC_DOWNLOAD_DISABLED);
        }
        if (!exactTime) {
            return ScheduleStatus.of(ScheduleState.LEAD_TIME_MODE);
        }
        if (!canScheduleExactAlarms) {
            return ScheduleStatus.of(ScheduleState.EXACT_ALARM_PERMISSION_REQUIRED);
        }
        return ScheduleStatus.scheduled(triggerAtMillis);
    }

    @NonNull
    static ExactAlarmRequirement evaluateExactAlarmRequirement(boolean canScheduleExactAlarms,
                                                               boolean prefetchEnabled,
                                                               boolean exactTime) {
        if (canScheduleExactAlarms) {
            return ExactAlarmRequirement.NOT_REQUIRED;
        }
        if (prefetchEnabled && exactTime) {
            return ExactAlarmRequirement.PLAYBACK_AND_EXACT_DOWNLOAD;
        }
        return ExactAlarmRequirement.PLAYBACK_ONLY;
    }

    public enum ExactAlarmRequirement {
        NOT_REQUIRED,
        PLAYBACK_ONLY,
        PLAYBACK_AND_EXACT_DOWNLOAD,
    }

    public enum ScheduleState {
        DISABLED,
        MISSING_PODCAST,
        EXACT_ALARM_PERMISSION_REQUIRED,
        AUTOMATIC_DOWNLOAD_DISABLED,
        LEAD_TIME_MODE,
        SCHEDULED,
    }

    public static final class ScheduleStatus {
        @NonNull
        private final ScheduleState state;
        private final long triggerAtMillis;

        private ScheduleStatus(@NonNull ScheduleState state, long triggerAtMillis) {
            this.state = state;
            this.triggerAtMillis = triggerAtMillis;
        }

        @NonNull
        public static ScheduleStatus of(@NonNull ScheduleState state) {
            return new ScheduleStatus(state, -1L);
        }

        @NonNull
        public static ScheduleStatus scheduled(long triggerAtMillis) {
            return new ScheduleStatus(ScheduleState.SCHEDULED, triggerAtMillis);
        }

        @NonNull
        public ScheduleState getState() {
            return state;
        }

        public long getTriggerAtMillis() {
            return triggerAtMillis;
        }
    }
}