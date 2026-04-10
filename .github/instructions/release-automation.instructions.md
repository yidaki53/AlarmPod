---
applyTo: ".github/workflows/**/*.yml,app/build.gradle,scripts/compute_next_release.py"
---

# Release automation learnings

## Branch model for this fork
- This fork still follows the upstream AntennaPod branch model where `develop` is the main integration branch.
- Do not assume a `main` branch exists when adding or editing CI workflows in this repository.
- Continuous debug artifact automation for the fork should target `develop`.
- Versioned release automation for the fork should target `master`.

## Build artifact workflow
- `.github/workflows/develop-apk.yml` publishes a PlayDebug APK artifact for every push to `develop`.
- Keep that workflow lightweight and artifact-focused; it is for continuous validation and easy manual download, not for release publishing.

## Release workflow
- `.github/workflows/release-from-master.yml` computes the next semantic version, builds a versioned PlayRelease APK, and creates a GitHub release from `master`.
- Automatic release publication should run from pushes to `master`.
- Keep `workflow_dispatch` available for manual release runs and override scenarios.
- The release workflow currently uses a temporary keystore, which is suitable for GitHub release artifacts but not equivalent to a production store-signing setup.

## Versioning strategy
- `app/build.gradle` supports `versionNameOverride` and `versionCodeOverride` Gradle properties so CI can build release artifacts without editing committed source versions.
- `scripts/compute_next_release.py` should prefer PR labels to determine the next bump and fall back to Conventional Commit parsing when labels are absent.
- Supported semantic label intent is major > minor > patch. Keep the mapping explicit in the script instead of spreading it across workflow YAML.
- Automatic master releases should skip publication when the merged work does not indicate a feature, fix, performance change, refactor patch, revert, or breaking change.
- If the repository has no existing `vX.Y.Z` tags yet, treat the current app version in `app/build.gradle` as the base version and inspect a bounded recent commit window instead of scanning the entire history.

## Validation expectations
- After changing release automation, validate both `:app:assembleDebug` and a `PlayRelease` build with explicit version override properties.
- Keep workflow logic maintainable: put nontrivial version computation in scripts rather than inline shell in YAML.

## Fork update checks in the app
- The fork can check GitHub releases for updates automatically at app startup.
- Keep the update check fork-specific by using BuildConfig release URLs rather than hardcoding upstream AntennaPod endpoints into UI code.
- The automatic check should be lightweight, network-aware, and rate-limited so it does not show repeated prompts on every launch.