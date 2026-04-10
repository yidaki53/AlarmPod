package de.danoeh.antennapod.alarm;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.feed.SortOrder;
import de.danoeh.antennapod.storage.database.DBReader;

public final class PodcastAlarmEpisodeResolver {
    private PodcastAlarmEpisodeResolver() {
    }

    @NonNull
    public static Resolution resolve(long feedId) {
        Feed feed = DBReader.getFeed(feedId, false, 0, 0);
        if (feed == null) {
            return Resolution.empty();
        }
        List<FeedItem> items = DBReader.getFeedItemList(
                feed,
                FeedItemFilter.unfiltered(),
                SortOrder.DATE_NEW_OLD,
                0,
                100);
        return selectFromItems(items);
    }

    @NonNull
    static Resolution selectFromItems(@Nullable List<FeedItem> items) {
        if (items == null) {
            items = Collections.emptyList();
        }

        FeedMedia preferred = null;
        FeedMedia downloadedFallback = null;

        for (FeedItem item : items) {
            FeedMedia media = item.getMedia();
            if (media == null) {
                continue;
            }
            if (downloadedFallback == null && media.fileExists()) {
                downloadedFallback = media;
            }
            if (preferred == null && !item.isPlayed()) {
                preferred = media;
            }
            if (preferred != null && downloadedFallback != null) {
                return new Resolution(preferred, downloadedFallback);
            }
        }

        if (preferred == null) {
            for (FeedItem item : items) {
                if (item.getMedia() != null) {
                    preferred = item.getMedia();
                    break;
                }
            }
        }

        return new Resolution(preferred, downloadedFallback);
    }

    public static final class Resolution {
        @Nullable
        private final FeedMedia preferredMedia;
        @Nullable
        private final FeedMedia downloadedFallback;

        Resolution(@Nullable FeedMedia preferredMedia, @Nullable FeedMedia downloadedFallback) {
            this.preferredMedia = preferredMedia;
            this.downloadedFallback = downloadedFallback;
        }

        @NonNull
        static Resolution empty() {
            return new Resolution(null, null);
        }

        @Nullable
        public FeedMedia getPreferredMedia() {
            return preferredMedia;
        }

        @Nullable
        public FeedMedia getDownloadedFallback() {
            return downloadedFallback;
        }
    }
}