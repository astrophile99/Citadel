# Releasing Citadel to Google Play

Everything in the repository is ready. What remains happens in your own accounts — the
upload key, the Play Console listing, and the declarations only the developer can make.
Work through this once; later releases are steps 2–3 and 9 only.

---

## 1. Create your upload key (once)

From the project root, in PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\create-upload-key.ps1
```

This creates `citadel-upload.jks` and `keystore.properties`. Both are gitignored and must
never be committed. **Back up the `.jks` file and its password now** (a password manager is
ideal). Play App Signing lets you reset a lost upload key through support, but it takes days.

## 2. Set the version

In `app/build.gradle.kts`, `versionCode` must be higher than any build you have uploaded
before (start at 1). `versionName` is what people see — `1.0.0` for the first release.

## 3. Build the bundle

```bash
./gradlew :app:bundleRelease
```

The signed bundle is `app/build/outputs/bundle/release/app-release.aab`. The release build
is shrunk with R8; it has been run on a device, but always install the release build once
yourself before uploading:

```bash
./gradlew :app:assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```

## 4. Publish the privacy policy URL

Play requires a privacy policy URL. The policy lives at `docs/privacy/index.html`.

1. Make the GitHub repository public (GitHub Pages on a free account needs it).
2. GitHub → **Settings → Pages** → Source: *Deploy from a branch* → Branch: `main`, folder `/docs`.
3. After a minute it is live at **https://astrophile99.github.io/Citadel/privacy/**

If you would rather keep the repository private, host `docs/privacy/index.html` anywhere
public and use that URL instead. If you ever change the policy, edit
`ui/sanctuary/PrivacyPolicy.kt` and run `python scripts/generate-privacy-pages.py` so the
in-app copy and the hosted copy stay identical.

## 5. Create the app in Play Console

- **App name:** `Citadel` (the store title below is longer)
- **Default language:** English (United States) — en-US
- **App or game:** App · **Free or paid:** Free
- Accept the declarations.

## 6. App content — the exact answers

| Section | Answer |
|---|---|
| Privacy policy | `https://astrophile99.github.io/Citadel/privacy/` |
| Ads | No, the app does not contain ads |
| App access | All functionality is available without special access |
| Content rating | Category *Utility, Productivity, Communication, or Other*. Answer **No** to every question (violence, sexuality, language, controlled substances, gambling, user-to-user interaction, location sharing, digital purchases). Expected result: Everyone / PEGI 3 |
| Target audience | 13 and over (selecting younger ages brings in the Families policy, which does not suit this app) |
| News app | No |
| Health apps | Not a health app |
| Financial features | None |
| Government app | No |
| Advertising ID | No — the app does not use it |
| Data safety → collects or shares user data? | **No.** All data is processed and stored only on the device, and the app has no internet permission. Android's own device backup is operated by Google, not by the app |

## 7. Store listing

Copy each field from `fastlane/metadata/android/en-US/`:

| Field | File | Limit |
|---|---|---|
| App title | `title.txt` | 30 |
| Short description | `short_description.txt` | 80 |
| Full description | `full_description.txt` | 4000 |
| App icon (512 × 512) | `images/icon.png` | — |
| Feature graphic (1024 × 500) | `images/featureGraphic.png` | — |
| Phone screenshots (1080 × 1920) | `images/phoneScreenshots/1–8.png` | 2–8 |

- **Category:** Productivity
- **Contact email:** required — use an address you are happy to show publicly
- **Website:** optional — the GitHub repository works

(If you use fastlane, `fastlane supply` reads this folder directly.)

## 8. Testing track requirement for new developer accounts

Personal developer accounts created after November 2023 must run a **closed test with at
least 12 testers who stay opted in for 14 consecutive days** before they can apply for
production access. Plan for this:

1. **Testing → Internal testing** — upload the bundle, add yourself, install from the Play link, check it.
2. **Testing → Closed testing** — create a track, add at least 12 testers (an email list or Google Group), upload the same bundle, share the opt-in link.
3. After 14 days, apply for production access from the dashboard.

## 9. Release

1. Upload `app-release.aab` to the track.
2. Release notes: paste `fastlane/metadata/android/en-US/changelogs/1.txt` (for later
   releases, add `changelogs/<versionCode>.txt`).
3. Accept **Play App Signing** when asked (the default).
4. Review the **pre-launch report** Play generates; then roll out.

## Before every release

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug :app:bundleRelease
```

All three must pass. Then bump `versionCode`, add a changelog, install the release build
on a device, and upload.
