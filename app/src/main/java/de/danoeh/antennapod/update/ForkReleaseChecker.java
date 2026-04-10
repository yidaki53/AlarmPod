package de.danoeh.antennapod.update;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.danoeh.antennapod.BuildConfig;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.net.common.AntennapodHttpClient;
import de.danoeh.antennapod.net.common.NetworkUtils;
import de.danoeh.antennapod.ui.common.IntentUtils;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class ForkReleaseChecker {
    private static final String TAG = "ForkReleaseChecker";
    private static final String PREF_NAME = "ForkReleaseChecker";
    private static final String PREF_LAST_CHECKED_AT = "lastCheckedAt";
    private static final String PREF_LAST_NOTIFIED_VERSION = "lastNotifiedVersion";
    private static final long AUTO_CHECK_INTERVAL_MILLIS = 24L * 60L * 60L * 1000L;
    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)");

    private ForkReleaseChecker() {
    }

    public static void maybeCheckForUpdates(Activity activity) {
        if (!BuildConfig.FORK_UPDATES_ENABLED || !NetworkUtils.networkAvailable()) {
            return;
        }
        SharedPreferences preferences = activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        long now = System.currentTimeMillis();
        long lastCheckedAt = preferences.getLong(PREF_LAST_CHECKED_AT, 0L);
        if (now - lastCheckedAt < AUTO_CHECK_INTERVAL_MILLIS) {
            return;
        }
        preferences.edit().putLong(PREF_LAST_CHECKED_AT, now).apply();
        checkForUpdates(activity, preferences);
    }

    private static void checkForUpdates(Activity activity, SharedPreferences preferences) {
        Single.fromCallable(ForkReleaseChecker::fetchLatestRelease)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(releaseInfo -> showUpdateIfNeeded(activity, preferences, releaseInfo),
                        throwable -> Log.d(TAG, "Could not check for fork releases", throwable));
    }

    private static ReleaseInfo fetchLatestRelease() throws IOException, JSONException {
        OkHttpClient client = AntennapodHttpClient.getHttpClient();
        Request request = new Request.Builder()
                .url(BuildConfig.FORK_RELEASES_API_URL)
                .header("Accept", "application/vnd.github+json")
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Unexpected release lookup response: " + response.code());
            }
            JSONObject payload = new JSONObject(response.body().string());
            String version = normalizeVersion(payload.optString("tag_name"));
            if (isNullOrEmpty(version)) {
                throw new IOException("Latest release tag did not contain a semantic version");
            }
            String url = payload.optString("html_url", BuildConfig.FORK_RELEASES_PAGE_URL);
            if (isNullOrEmpty(url)) {
                url = BuildConfig.FORK_RELEASES_PAGE_URL;
            }
            return new ReleaseInfo(version, url);
        }
    }

    private static void showUpdateIfNeeded(Activity activity,
                                           SharedPreferences preferences,
                                           ReleaseInfo releaseInfo) {
        if (activity.isFinishing() || !isNewerVersion(releaseInfo.version, BuildConfig.VERSION_NAME)) {
            return;
        }
        String lastNotifiedVersion = preferences.getString(PREF_LAST_NOTIFIED_VERSION, "");
        if (releaseInfo.version.equals(lastNotifiedVersion)) {
            return;
        }
        View anchor = activity.findViewById(R.id.main_view);
        if (anchor == null) {
            return;
        }
        preferences.edit().putString(PREF_LAST_NOTIFIED_VERSION, releaseInfo.version).apply();
        Snackbar.make(anchor,
                activity.getString(R.string.fork_update_available, releaseInfo.version),
                Snackbar.LENGTH_LONG)
                .setAction(R.string.fork_update_action_view,
                        view -> IntentUtils.openInBrowser(activity, releaseInfo.url))
                .show();
    }

    static String normalizeVersion(String rawVersion) {
        if (rawVersion == null) {
            return "";
        }
        Matcher matcher = VERSION_PATTERN.matcher(rawVersion);
        if (!matcher.find()) {
            return "";
        }
        return matcher.group(1) + "." + matcher.group(2) + "." + matcher.group(3);
    }

    static boolean isNewerVersion(String latestVersion, String currentVersion) {
        int[] latest = parseVersion(latestVersion);
        int[] current = parseVersion(currentVersion);
        if (latest == null || current == null) {
            return false;
        }
        for (int index = 0; index < latest.length; index++) {
            if (latest[index] > current[index]) {
                return true;
            }
            if (latest[index] < current[index]) {
                return false;
            }
        }
        return false;
    }

    private static int[] parseVersion(String version) {
        String normalizedVersion = normalizeVersion(version);
        if (isNullOrEmpty(normalizedVersion)) {
            return null;
        }
        String[] parts = normalizedVersion.split("\\.");
        if (parts.length != 3) {
            return null;
        }
        int[] parsed = new int[3];
        for (int index = 0; index < parts.length; index++) {
            parsed[index] = Integer.parseInt(parts[index]);
        }
        return parsed;
    }

    private static boolean isNullOrEmpty(String value) {
        return value == null || value.isEmpty();
    }

    private static final class ReleaseInfo {
        private final String version;
        private final String url;

        private ReleaseInfo(String version, String url) {
            this.version = version;
            this.url = url;
        }
    }
}