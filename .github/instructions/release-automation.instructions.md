---
applyTo: ".github/workflows/**/*.yml,app/build.gradle,scripts/compute_next_release.py"
---

# Release automation learnings

## Branch model for this fork
- This fork still follows the upstream AntennaPod branch model where `develop` is the main integration branch.
- Do not assume a `main` branch exists when adding or editing CI workflows in this repository.
- Build and release automation for the fork should target `develop` unless the user explicitly changes the branch strategy.

## Build artifact workflow
- `.github/workflows/develop-apk.yml` publishes a PlayDebug APK artifact for every push to `develop`.
- Keep that workflow lightweight and artifact-focused; it is for continuous validation and easy manual download, not for release publishing.

## Release workflow
- `.github/workflows/release-from-develop.yml` is a manual workflow that computes the next semantic version, builds a versioned PlayRelease APK, and creates a GitHub release.
- Prefer `workflow_dispatch` for release publication so version selection and prerelease status remain explicit.
- The release workflow currently uses a temporary keystore, which is suitable for GitHub release artifacts but not equivalent to a production store-signing setup.

## Versioning strategy
- `app/build.gradle` supports `versionNameOverride` and `versionCodeOverride` Gradle properties so CI can build release artifacts without editing committed source versions.
- `scripts/compute_next_release.py` should prefer PR labels to determine the next bump and fall back to Conventional Commit parsing when labels are absent.
- Supported semantic label intent is major > minor > patch. Keep the mapping explicit in the script instead of spreading it across workflow YAML.
- If the repository has no existing `vX.Y.Z` tags yet, treat the current app version in `app/build.gradle` as the base version and inspect a bounded recent commit window instead of scanning the entire history.

## Validation expectations
- After changing release automation, validate both `:app:assembleDebug` and a `PlayRelease` build with explicit version override properties.
- Keep workflow logic maintainable: put nontrivial version computation in scripts rather than inline shell in YAML.