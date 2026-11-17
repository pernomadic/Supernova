# AGENTS.md

Working notes for this repository. Read before making changes.

## What this is

`Aryonova` is a **fork** of [tanujnotes/Supernova](https://github.com/tanujnotes/Supernova),
an open-source Android home-screen launcher (Kotlin, no Compose, plain Views +
RecyclerView, DataStore-backed preferences). Licence: **AGPL-3.0** — this fork
stays AGPL-3.0 and its source stays available.

The launcher intentionally declares **no network permission**. Do not add one,
and do not add libraries that need it (no analytics, no remote config, no crash
reporting SDKs). That is the product promise and the reason it is on F-Droid.

## Branch model — keep upstream merges cheap

| Branch   | Purpose |
|----------|---------|
| `main`   | **Pristine mirror of upstream `main`.** Only fast-forwards from `upstream/main`. Never commit here. |
| `custom` | **All of our work.** Default branch for development and the branch we build from. |

Rules that keep `git merge upstream/main` fast and conflict-light:

1. Never commit to `main`; only `git merge --ff-only upstream/main`.
2. Small, localised diffs. Do not reformat, reorder, or mass-rename upstream
   code — whitespace churn turns every future upstream merge into a conflict.
3. Prefer **new files** and a small hook in an existing file over rewriting an
   existing upstream function. One-line integration points merge cleanly.
4. Keep upstream comments, names and file layout. Add our own notes instead of
   replacing theirs.
5. Never regenerate `app/lint-baseline.xml` or `app/detekt-baseline.xml` to make
   a build pass. Baselines exist so upstream's known issues stay quiet; if our
   change adds a warning, fix our code.
6. Do not change `applicationId`, `versionCode` or `versionName` in
   `app/build.gradle.kts` as an unrelated edit. Increase the version once per
   build we ship, and resolve the (single-line) conflict on upstream bumps by
   taking the higher numbers.
7. Optional features we do not want upstream to fight over belong behind a
   small default-off flag, so the merge and the diff both stay boring.

### Syncing with upstream

```powershell
pwsh ./scripts/sync-upstream.ps1            # fetch, update main, merge into custom
pwsh ./scripts/sync-upstream.ps1 -NoMerge   # only refresh main
```

Manual equivalent:

```bash
git fetch upstream --tags --prune
git checkout main && git merge --ff-only upstream/main && git push origin main
git checkout custom && git merge --no-ff upstream/main -m "Merge upstream/main into custom"
```

Remotes: `origin` = `pernomadic/Supernova` (our fork), `upstream` =
`tanujnotes/Supernova`.

Upstream also develops on **`upstream/dev`** between releases. `upstream/main`
is the stable, released line. Merge `upstream/main` by default; merge or
cherry-pick `upstream/dev` only deliberately, for a fix or feature we want early.
That merge is allowed to be untidy — it is not the routine path.

## Build and test

Requirements: JDK 17 (toolchain is pinned; `gradle/gradle-daemon-jvm.properties`
selects it) and the Android SDK with platform **compileSdk 37** and build-tools.

```bash
./gradlew :app:assembleDebug          # APK: app/build/outputs/apk/debug/
./gradlew :app:testDebugUnitTest      # JVM unit tests (fast, no device)
./gradlew :app:lintDebug              # lint; warnings are errors, baseline-backed
./gradlew :app:detekt                 # static analysis, baseline-backed
./gradlew :app:assembleRelease        # release; unsigned unless local/keystore.properties exists
```

The debug build installs as `com.supernova.launcher.debug`, so it can sit next
to the official app. `local/` is git-ignored: put `keystore.properties`
(`storeFile`, `storePassword`, `keyAlias`, `keyPassword`) there, not in the repo.

`app/src/androidTest` is instrumentation testing — it needs a device or emulator
and is not part of the normal loop.
