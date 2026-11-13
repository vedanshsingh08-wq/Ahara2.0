# AHARA · आहार
**Understanding Ahara Through Ayurveda.**

AHARA is a native Android app for monitoring food timing and eating behaviour through the classical verse:

> याममध्ये न भोक्तव्यं यामयुग्मं न लंघयेत् ।
> याममध्ये रसोद्वेगो युग्मातीते बलक्षयः ॥ (क्षे.कु.)

> Verify the exact textual source, chapter and edition before academic publication.

## Download the APK (no computer needed)
1. Open the repository on GitHub → **Releases** (right side, or `/releases`).
2. Open the latest **AHARA build N** and tap the `.apk` file.
3. On your phone, allow *Install unknown apps* for your browser if Android asks, then install.

Every push to `main` builds a new APK automatically (`.github/workflows/build-apk.yml`). You can also run it manually: **Actions → Build AHARA APK → Run workflow**.

## Features
| Area | What it does |
|---|---|
| 4 shloka modules | Meal intervals (याममध्ये न भोक्तव्यम्), extended gaps & missed meals (यामयुग्मं न लंघयेत्), post-meal digestive response (याममध्ये रसोद्वेगः), energy & fatigue (युग्मातीते बलक्षयः) |
| Food diary | Breakfast / lunch / dinner / snack, food & ingredients, time, quantity, hunger, appetite, energy, symptoms, notes. Edit & delete. Skipped meals. |
| Observation engine | Configurable Yama (default 3 h). Classifies intervals < 1 Yama, 1–2 Yamas, > 2 Yamas; overnight gaps optional. Produces descriptive, non-diagnostic observations. |
| Assessments | Optional Prakriti (Vata, Pitta, Kapha, Dvandvaja, Tridoshaja) and Agni (Sama, Vishama, Tikshna, Manda) — questionnaire or recorded result, self vs practitioner-assisted. |
| Dashboard | Today's meals, last meal, live time-since-last-meal with Yama status, average interval, regularity, hunger pattern, discomfort frequency, energy trend, hydration & sleep. |
| Analytics | Daily (timeline, intervals, hunger, responses, energy), weekly & monthly (trends + comparison with previous period). |
| Reports | PDF reports (daily/weekly/monthly), meal CSV, full backup CSV, anonymised research CSV. |
| Research dashboard | Consent-based, random participant ID, anonymised export, interval distribution, digestive response frequency, energy by interval class, import of other participants' CSVs for participant-wise and Prakriti/Agni group comparison. |
| Reminders | Optional missing-meal reminder, daily summary, weekly report (WorkManager). No fixed meal times are prescribed. |
| Languages | English, हिंदी, संस्कृतम् — switch any time in Settings. |
| Privacy | All data stays on the device (Room database), no network access, delete-all option, privacy policy in-app. |

## Tech stack
Kotlin · Jetpack Compose (Material 3) · MVVM (`AppViewModel`) · Room · Coroutines/Flow · DataStore · WorkManager · Android `PdfDocument` for PDFs · Compose Canvas charts (no third-party chart dependency).

## Project layout
```
app/src/main/java/com/ahara/app/
  data/        Room entities, DAO, database, DataStore settings
  engine/      ObservationEngine — shloka-based interval logic (pure Kotlin, unit-tested)
  export/      CSV / PDF / anonymised dataset / research CSV import
  notify/      WorkManager reminders and notifications
  i18n/        Strings.kt (GENERATED — see below)
  ui/          theme, shared components & charts, screens
app/src/test/  ObservationEngine unit tests
```

## Building locally
Android Studio (Koala or newer) → *Open* this folder → Run. Or on the command line with Gradle 8.9 and JDK 17:
```
gradle assembleRelease      # APK in app/build/outputs/apk/release/
gradle testDebugUnitTest    # unit tests
```

## Editing translations
`i18n/Strings.kt` is generated. The source files live in `i18n_src/*.txt`, one string per line:
```
key|English|Hindi|Sanskrit
```
Use `{0}`, `{1}` for placeholders and `\n` for line breaks. A blank Sanskrit column falls back to Hindi. Regenerate with:
```
python3 tools/gen_strings.py
```
The Sanskrit interface text is a best-effort rendering and should be reviewed by a Sanskrit scholar.

## Maintenance notes
- **Changing the database**: bump `version` in `AharaDatabase` and add a `Migration`; the schema JSON is exported to `app/schemas/`.
- **Yama benchmark**: the default lives in `Settings.yamaHours` (`data/Prefs.kt`); interpretive rules are documented at the top of `ObservationEngine.kt`.
- **New screen**: add a composable in `ui/screens`, a route in `Routes` and a `composable(...)` entry in `AharaNavHost`.

## Publishing on Google Play
1. Create an upload keystore (`keytool -genkeypair -v -keystore upload.jks -alias ahara -keyalg RSA -keysize 2048 -validity 10000`). Keep it safe — never commit it.
2. Replace the debug `signingConfig` in `app/build.gradle.kts` with a release config that reads the keystore from environment variables or GitHub Secrets.
3. Raise `versionCode` / `versionName` for each release; consider `isMinifyEnabled = true`.
4. Build an App Bundle: `gradle bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`.
5. In Play Console: create the app, complete the Data safety form (no data collected or shared off-device), add the privacy policy URL, content rating and health-app declarations, then upload the AAB to internal testing first.
6. Target the latest API level Google Play requires at the time of submission.

## Disclaimer
AHARA records self-reported observations within a traditional Ayurvedic framework. It does not diagnose, treat or prescribe, and its observations are not validated medical assessments. Research use of participant data requires informed consent and appropriate ethics approval.
