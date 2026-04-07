---
applyTo: "app/src/main/java/de/danoeh/antennapod/alarm/**/*.java,app/src/main/java/de/danoeh/antennapod/ui/screen/preferences/**/*,storage/preferences/src/main/java/de/danoeh/antennapod/storage/preferences/**/*.java,app/src/test/java/de/danoeh/antennapod/alarm/**/*.java,ui/preferences/src/main/res/xml/preferences_podcast_alarm.xml,ui/preferences/src/main/res/xml/preferences_playback.xml,ui/preferences/src/main/res/values/arrays.xml,ui/i18n/src/main/res/values/strings.xml,app/src/main/AndroidManifest.xml"
---

# Podcast alarm learnings

## Product shape
- The current design is one global daily podcast alarm, not per-feed alarms.
- The user selects exactly one subscribed podcast feed and one alarm time.
- The dedicated UI entry point lives under playback settings and opens its own preference screen.

## Storage and initialization
- Global alarm state is stored in `PodcastAlarmPreferences` under the `:storage:preferences` module.
- `ClientConfigurator` initializes `PodcastAlarmPreferences` during app startup.
- Keep alarm persistence global unless the product direction explicitly changes to feed-specific alarms.

## Scheduling flow
- `PodcastAlarmScheduler` is the central orchestration point.
- Use `AlarmManager` exact alarms for the trigger path and `WorkManager` for optional prefetch work.
- `PodcastAlarmBootReceiver` restores the schedule after boot and package replacement.
- `PodcastAlarmBootReceiver` should treat the received action as an input that needs validation and only reschedule when the protected-broadcast action matches the manifest contract.
- Daily wall-clock alarms need a reschedule path for manual time changes and timezone changes; otherwise the stored exact trigger can drift away from the user-selected local time.
- Existing enabled alarms should also be rescheduled on app startup so any trigger-path migration takes effect without waiting for reboot or manual reconfiguration.
- On Android 12 and above, exact-alarm permission gating must remain part of the enable flow.
- This app already has `minSdk 23`, so `setExactAndAllowWhileIdle(...)` is part of the supported sample space and does not need a pre-M compatibility branch.
- The scheduler still has to guard `PendingIntent.getForegroundService(...)` behind API 26 and fall back to `PendingIntent.getService(...)` on older devices because the pending-intent transport changes across that boundary.

## Playback and fallback behavior
- Keep `PodcastAlarmReceiver` as a thin compatibility shim only; real alarm execution should happen in `PodcastAlarmExecutionService`.
- Use a foreground service trigger for real playback execution on device models where broadcast delivery can get stuck in-flight.
- `PodcastAlarmExecutionService` reschedules the next alarm, resolves the episode, and starts playback through `PlaybackServiceStarter`.
- Foreground alarm services still need pre-N shutdown compatibility. Model that branch explicitly and select the `stopForeground(...)` signature that is valid for the device API level.
- Episode selection order is: newest unplayed episode, otherwise newest played episode, otherwise generic device alarm fallback.
- If streaming is unavailable and there is a downloaded episode fallback, prefer the downloaded fallback.
- If no playable episode is available, `PodcastAlarmFallbackService` must provide an audible alarm path.

## Prefetch behavior
- Prefetch is optional and currently means: refresh the selected feed, then try to download the preferred alarm episode before trigger time.
- The current implementation uses one unique WorkManager chain so later reschedules replace prior prefetch work.
- Keep the prefetch path network-constrained.
- Use exact wake-up alarms for anything that must happen at a precise user-selected time; WorkManager is best-effort and should only be used for lead-time prefetch.
- When users choose an exact download clock time instead of a lead time, schedule a separate exact wake-up alarm that starts a silent foreground service and immediately enqueues the refresh-download work.

## UI wiring
- The launcher preference belongs in playback preferences.
- The dedicated screen is mapped in `PreferenceActivity` and indexed in `MainPreferencesFragment` so settings search can find it.
- The exact-alarm permission preference should only be visible when the permission is actually needed.
- When exact alarms are unavailable, the permission summary should reflect whether the current settings need the permission for playback only or for both playback and exact-time downloads.
- On this device, the platform `TimePickerDialog` is more reliable than `MaterialTimePicker` for persisting the selected alarm hour correctly.
- The automation UI supports two download strategies: lead time before playback, or a separate exact download time of day.
- Keep playback and exact-download schedule-state evaluation in a dedicated helper so the preferences fragment only renders summaries instead of duplicating enable, feed-selection, prefetch-mode, and exact-alarm gating logic.
- Keep the settings search breadcrumbs and the Rx-backed feed selection block formatted to satisfy the repository's strict checkstyle indentation rules. Reviewers will look for both semantic correctness and stable formatting in these touchpoints.

## Tests and validation
- Focused unit coverage exists for episode resolution and next-trigger calculation in `app/src/test/java/de/danoeh/antennapod/alarm`.
- If alarm selection logic changes, update `PodcastAlarmEpisodeResolverTest` first.
- If alarm timing logic changes, update `PodcastAlarmSchedulerTest` first.
- Before considering the branch ready, run the repository quality gate `./gradlew checkstyle lint spotbugsPlayDebug spotbugsDebug` with JDK 21. In recent alarm iterations, this gate surfaced API-level and manifest-permission regressions that were not detectable from unit-test outcomes alone.
- For Android 14+ foreground alarm services, declare the matching foreground-service permissions in the manifest, including `FOREGROUND_SERVICE_MEDIA_PLAYBACK` for playback alarm services, because the runtime permission model is now coupled to the declared foreground-service type.