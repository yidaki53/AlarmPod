package de.danoeh.antennapod.alarm;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadServiceInterface;

public class PodcastAlarmPrefetchWorker extends Worker {
    public static final String EXTRA_FEED_ID = "feed_id";

    public PodcastAlarmPrefetchWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        long feedId = getInputData().getLong(EXTRA_FEED_ID, -1L);
        if (feedId <= 0) {
            return Result.success();
        }

        PodcastAlarmEpisodeResolver.Resolution resolution = PodcastAlarmEpisodeResolver.resolve(feedId);
        FeedMedia media = resolution.getPreferredMedia();
        if (media == null || media.fileExists()) {
            return Result.success();
        }
        FeedItem item = media.getItem();
        if (item == null) {
            return Result.success();
        }

        DownloadServiceInterface.get().downloadNow(getApplicationContext(), item, true);
        return Result.success();
    }
}