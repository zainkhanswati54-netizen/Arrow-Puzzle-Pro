# Building the APK with GitHub Actions

Two workflows live in `.github/workflows/`:

| Workflow | Trigger | Output |
| --- | --- | --- |
| `build-debug.yml` | every push / PR to `main`, or manual | debug APK as a build artifact |
| `release.yml` | pushing a tag like `v0.1.0`, or manual | signed release APK + AAB, attached to a GitHub Release |

---

## 1. Push the project to GitHub

```bash
cd ArrowPuzzle
git init
git add .
git commit -m "Arrow Puzzle scaffold"
git branch -M main
git remote add origin https://github.com/<you>/<repo>.git
git push -u origin main
```

That is all the setup the debug build needs. Open the **Actions** tab and the run
starts on its own.

## 2. Download the APK

Actions tab → click the run → **Artifacts** section at the bottom →
`arrow-puzzle-debug-apk`. It downloads as a zip; the APK is inside.

Artifacts are kept 30 days. The filename carries the short commit SHA so you can
always tell two builds apart.

---

## About the Gradle wrapper

The archive has `gradle/wrapper/gradle-wrapper.properties` but **not**
`gradle-wrapper.jar` — that is a binary Gradle generates, and it could not be
fetched offline. Both workflows handle this: if the jar is missing they run
`gradle wrapper --gradle-version 8.9` before building.

It is still worth committing the real wrapper once, so local builds and CI use an
identical Gradle. Open the project in Android Studio (it offers to generate the
wrapper on first sync), then:

```bash
git add gradlew gradlew.bat gradle/wrapper/
git commit -m "Add Gradle wrapper"
git push
```

After that the generate step in CI becomes a no-op.

---

## 3. Signed release builds (optional)

Without a keystore the release workflow still runs, but signs with the **debug
key**. That APK installs fine for testing and cannot be uploaded to Play.

### Create a keystore

```bash
keytool -genkey -v -keystore release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 -alias arrowpuzzle
```

Keep `release.jks` safe and **never commit it**. Losing it means you can never
update the app on Play under the same listing.

### Turn it into a secret

```bash
base64 -w 0 release.jks > release.jks.base64   # macOS: base64 -i release.jks -o release.jks.base64
```

Repo → **Settings → Secrets and variables → Actions → New repository secret**.
Add four:

| Secret | Value |
| --- | --- |
| `KEYSTORE_BASE64` | contents of `release.jks.base64` |
| `KEYSTORE_PASSWORD` | the store password you chose |
| `KEY_ALIAS` | `arrowpuzzle` |
| `KEY_PASSWORD` | the key password you chose |

`app/build.gradle.kts` reads these through environment variables and falls back
to the debug key when they are absent, so local builds and forked PRs keep
working with no extra configuration.

### Cut a release

```bash
git tag v0.1.0
git push origin v0.1.0
```

The workflow builds, signs, and publishes a GitHub Release with the APK and AAB
attached. A tag containing a hyphen (`v0.2.0-beta`) is marked as a pre-release.

---

## When the build fails

This scaffold has never been compiled — there was no network in the environment
it was written in, so Gradle could never resolve dependencies. **The first CI run
is effectively the first compile.** Expect a small number of import-level errors.

The debug workflow uploads `app/build/reports/` on failure, but the fastest path
is usually the raw log: click the failed run → **Build debug APK** step → look for
lines starting with `e: file:///`. Those are Kotlin compile errors with exact file
and line numbers.

Send those lines over and they are quick to fix.

---

## Version numbers

`versionCode` and `versionName` are currently hard-coded in
`app/build.gradle.kts`. Once you are releasing regularly, drive them from CI:

```kotlin
versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
versionName = System.getenv("GITHUB_REF_NAME")?.removePrefix("v") ?: "0.1.0"
```

Worth doing before the first Play upload, not after — Play rejects a duplicate
`versionCode`, and fixing it retroactively is annoying.
