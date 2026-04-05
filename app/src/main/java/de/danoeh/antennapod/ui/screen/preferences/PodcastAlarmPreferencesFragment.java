package de.danoeh.antennapod.ui.screen.preferences;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.DateFormat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.alarm.PodcastAlarmScheduler;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.preferences.PodcastAlarmPreferences;
import de.danoeh.antennapod.ui.preferences.screen.AnimatedPreferenceFragment;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class PodcastAlarmPreferencesFragment extends AnimatedPreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    private Preference selectedPodcastPreference;
    private Preference timePreference;
    private Preference downloadTimePreference;
    private Preference exactAlarmPermissionPreference;
    private SwitchPreferenceCompat enabledPreference;
    private ListPreference prefetchModePreference;
    private ListPreference prefetchMinutesPreference;
    private Disposable feedSelectionDisposable;
    private Disposable feedSummaryDisposable;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_podcast_alarm);

        selectedPodcastPreference = requirePreference(PodcastAlarmPreferences.PREF_FEED_ID);
        timePreference = requirePreference("prefPodcastAlarmTime");
        downloadTimePreference = requirePreference("prefPodcastAlarmDownloadTime");
        exactAlarmPermissionPreference = requirePreference("prefPodcastAlarmExactAlarmPermission");
        enabledPreference = requirePreference(PodcastAlarmPreferences.PREF_ENABLED);
        prefetchModePreference = requirePreference(PodcastAlarmPreferences.PREF_PREFETCH_MODE);
        prefetchMinutesPreference = requirePreference(PodcastAlarmPreferences.PREF_PREFETCH_MINUTES);

        selectedPodcastPreference.setOnPreferenceClickListener(preference -> {
            openPodcastSelection();
            return true;
        });
        timePreference.setOnPreferenceClickListener(preference -> {
            openTimePicker();
            return true;
        });
        downloadTimePreference.setOnPreferenceClickListener(preference -> {
            openDownloadTimePicker();
            return true;
        });
        exactAlarmPermissionPreference.setOnPreferenceClickListener(preference -> {
            requestExactAlarmPermission();
            return true;
        });
        enabledPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            if (!Boolean.TRUE.equals(newValue)) {
                return true;
            }
            if (!PodcastAlarmPreferences.hasSelectedFeed()) {
                EventBus.getDefault().post(new de.danoeh.antennapod.event.MessageEvent(
                        getString(R.string.podcast_alarm_select_podcast_first)));
                return false;
            }
            if (!PodcastAlarmScheduler.canScheduleExactAlarms(requireContext())) {
                EventBus.getDefault().post(new de.danoeh.antennapod.event.MessageEvent(
                        getString(R.string.podcast_alarm_exact_alarm_required)));
                requestExactAlarmPermission();
                return false;
            }
            return true;
        });
        prefetchModePreference.setOnPreferenceChangeListener((preference, newValue) -> {
            updatePrefetchModeSummary(String.valueOf(newValue));
            updatePrefetchConfigurationVisibility(String.valueOf(newValue));
            return true;
        });
        prefetchMinutesPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            updatePrefetchSummary(String.valueOf(newValue));
            return true;
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        ((PreferenceActivity) requireActivity()).getSupportActionBar().setTitle(R.string.podcast_alarm_title);
        PreferenceManager.getDefaultSharedPreferences(requireContext())
                .registerOnSharedPreferenceChangeListener(this);
        updateSummaries();
    }

    @Override
    public void onStop() {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
                .unregisterOnSharedPreferenceChangeListener(this);
        if (feedSelectionDisposable != null) {
            feedSelectionDisposable.dispose();
        }
        if (feedSummaryDisposable != null) {
            feedSummaryDisposable.dispose();
        }
        super.onStop();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key == null || !key.startsWith("prefPodcastAlarm")) {
            return;
        }
        updateSummaries();
        if (PodcastAlarmPreferences.isEnabled()) {
            PodcastAlarmScheduler.schedule(requireContext());
        } else {
            PodcastAlarmScheduler.cancel(requireContext());
        }
    }

    private void updateSummaries() {
        updateTimeSummary();
        updateDownloadTimeSummary();
        updateFeedSummary();
        updatePrefetchModeSummary(prefetchModePreference.getValue());
        updatePrefetchSummary(prefetchMinutesPreference.getValue());
        updatePrefetchConfigurationVisibility(prefetchModePreference.getValue());
        updatePermissionVisibility();
    }

    private void updateTimeSummary() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, PodcastAlarmPreferences.getHour());
        calendar.set(Calendar.MINUTE, PodcastAlarmPreferences.getMinute());
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        timePreference.setSummary(DateFormat.getTimeFormat(requireContext()).format(calendar.getTime()));
    }

    private void updateDownloadTimeSummary() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, PodcastAlarmPreferences.getDownloadHour());
        calendar.set(Calendar.MINUTE, PodcastAlarmPreferences.getDownloadMinute());
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        downloadTimePreference.setSummary(DateFormat.getTimeFormat(requireContext()).format(calendar.getTime()));
    }

    private void updateFeedSummary() {
        long feedId = PodcastAlarmPreferences.getSelectedFeedId();
        if (feedId <= 0) {
            selectedPodcastPreference.setSummary(R.string.podcast_alarm_no_podcast_selected);
            return;
        }
        if (feedSummaryDisposable != null) {
            feedSummaryDisposable.dispose();
        }
        feedSummaryDisposable = Single.fromCallable(() -> DBReader.getFeed(feedId, false, 0, 0))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        feed -> selectedPodcastPreference.setSummary(
                                feed != null ? feed.getTitle() : getString(R.string.podcast_alarm_no_podcast_selected)),
                        error -> selectedPodcastPreference.setSummary(R.string.podcast_alarm_no_podcast_selected));
    }

    private void updatePrefetchSummary(@NonNull String value) {
        int minutes = Integer.parseInt(value);
        String minuteSummary = getResources().getQuantityString(R.plurals.time_minutes_quantified, minutes, minutes);
        prefetchMinutesPreference.setSummary(getString(R.string.podcast_alarm_prefetch_minutes_summary, minuteSummary));
    }

    private void updatePrefetchModeSummary(@NonNull String value) {
        int index = prefetchModePreference.findIndexOfValue(value);
        if (index >= 0) {
            prefetchModePreference.setSummary(prefetchModePreference.getEntries()[index]);
        }
    }

    private void updatePrefetchConfigurationVisibility(@NonNull String value) {
        boolean exactTime = PodcastAlarmPreferences.PREFETCH_MODE_EXACT_TIME.equals(value);
        prefetchMinutesPreference.setVisible(!exactTime);
        downloadTimePreference.setVisible(exactTime);
    }

    private void updatePermissionVisibility() {
        boolean shouldShow = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && !PodcastAlarmScheduler.canScheduleExactAlarms(requireContext());
        exactAlarmPermissionPreference.setVisible(shouldShow);
    }

    private void openPodcastSelection() {
        if (feedSelectionDisposable != null) {
            feedSelectionDisposable.dispose();
        }
        feedSelectionDisposable = Single.fromCallable(() -> {
                    List<Feed> feeds = DBReader.getFeedList();
                    List<Feed> result = new ArrayList<>();
                    for (Feed feed : feeds) {
                        if (feed.getState() == Feed.STATE_SUBSCRIBED) {
                            result.add(feed);
                        }
                    }
                    return result;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::showPodcastSelectionDialog,
                        error -> EventBus.getDefault().post(new de.danoeh.antennapod.event.MessageEvent(
                                getString(R.string.error_label))));
    }

    private void showPodcastSelectionDialog(@NonNull List<Feed> feeds) {
        if (feeds.isEmpty()) {
            EventBus.getDefault().post(new de.danoeh.antennapod.event.MessageEvent(
                    getString(R.string.home_welcome_text)));
            return;
        }

        CharSequence[] titles = new CharSequence[feeds.size()];
        int checkedItem = -1;
        long selectedFeedId = PodcastAlarmPreferences.getSelectedFeedId();
        for (int i = 0; i < feeds.size(); i++) {
            titles[i] = feeds.get(i).getTitle();
            if (feeds.get(i).getId() == selectedFeedId) {
                checkedItem = i;
            }
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.podcast_alarm_choose_podcast)
                .setSingleChoiceItems(titles, checkedItem, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    int position = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                    if (position >= 0) {
                        PodcastAlarmPreferences.setSelectedFeedId(feeds.get(position).getId());
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void openTimePicker() {
        openTimePicker(PodcastAlarmPreferences.getHour(), PodcastAlarmPreferences.getMinute(),
                (hourOfDay, minute) -> PodcastAlarmPreferences.setTime(hourOfDay, minute),
                R.string.podcast_alarm_time_title);
    }

    private void openDownloadTimePicker() {
        openTimePicker(PodcastAlarmPreferences.getDownloadHour(), PodcastAlarmPreferences.getDownloadMinute(),
                (hourOfDay, minute) -> PodcastAlarmPreferences.setDownloadTime(hourOfDay, minute),
                R.string.podcast_alarm_download_time_title);
    }

    private void openTimePicker(int hour, int minute, @NonNull TimeSelectedListener listener, int titleRes) {
        TimePickerDialog picker = new TimePickerDialog(
                requireContext(),
                (view, hourOfDay, selectedMinute) -> listener.onTimeSelected(hourOfDay, selectedMinute),
                hour,
                minute,
                DateFormat.is24HourFormat(requireContext()));
        picker.setTitle(titleRes);
        picker.show();
    }

    private void requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return;
        }
        Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                .setData(android.net.Uri.parse("package:" + requireContext().getPackageName()));
        startActivity(intent);
    }

    @NonNull
    private <T extends Preference> T requirePreference(@NonNull CharSequence key) {
        T result = findPreference(key);
        if (result == null) {
            throw new IllegalArgumentException("Preference with key '" + key + "' is not found");
        }
        return result;
    }

    private interface TimeSelectedListener {
        void onTimeSelected(int hourOfDay, int minute);
    }
}