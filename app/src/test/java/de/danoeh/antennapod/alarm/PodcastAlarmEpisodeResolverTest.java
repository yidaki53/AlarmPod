package de.danoeh.antennapod.alarm;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

@RunWith(RobolectricTestRunner.class)
public class PodcastAlarmEpisodeResolverTest {

    @Test
    public void selectFromItemsPrefersUnplayedEpisodeOverNewerPlayedEpisode() {
        Feed feed = createFeed();
        FeedItem newerPlayed = createItem(1, feed, FeedItem.PLAYED, null, 1_000L);
        FeedItem olderUnplayed = createItem(2, feed, FeedItem.UNPLAYED, null, 2_000L);

        PodcastAlarmEpisodeResolver.Resolution resolution =
                PodcastAlarmEpisodeResolver.selectFromItems(Arrays.asList(newerPlayed, olderUnplayed));

        assertSame(olderUnplayed.getMedia(), resolution.getPreferredMedia());
        assertEquals(null, resolution.getDownloadedFallback());
    }

    @Test
    public void selectFromItemsProvidesDownloadedFallback() throws IOException {
        Feed feed = createFeed();
        FeedItem streamOnly = createItem(1, feed, FeedItem.UNPLAYED, null, 1_000L);
        File downloadedFile = File.createTempFile("podcast-alarm", ".mp3");
        downloadedFile.deleteOnExit();
        FeedItem downloaded = createItem(2, feed, FeedItem.PLAYED, downloadedFile, 2_000L);

        PodcastAlarmEpisodeResolver.Resolution resolution =
                PodcastAlarmEpisodeResolver.selectFromItems(Arrays.asList(streamOnly, downloaded));

        assertSame(streamOnly.getMedia(), resolution.getPreferredMedia());
        assertSame(downloaded.getMedia(), resolution.getDownloadedFallback());
    }

    private Feed createFeed() {
        return new Feed(1, "", "Feed", "https://example.com", "", "", "", "", Feed.TYPE_RSS2,
                "feed-1", null, null, "https://example.com/feed.xml", 0L);
    }

    private FeedItem createItem(long id, Feed feed, int state, File localFile, long mediaId) {
        FeedItem item = new FeedItem(id, "Episode " + id, "item-" + id, "https://example.com/" + id,
                new Date(), state, feed);
        String localFilePath = localFile != null ? localFile.getAbsolutePath() : null;
        long downloadDate = localFile != null ? System.currentTimeMillis() : 0L;
        FeedMedia media = new FeedMedia(mediaId, item, 0, 0, 0, "audio/mpeg", localFilePath,
                "https://example.com/media-" + id + ".mp3", downloadDate, null, 0, 0);
        item.setMedia(media);
        item.setFeed(feed);
        return item;
    }
}