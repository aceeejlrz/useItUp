# UseItUp

UseItUp is a George Brown College group Android assignment that helps reduce food waste.
Track pantry quantities and expiry dates, find meals using ingredients you have, save full
recipes for later, and maintain a shopping list. Daily reminders use WorkManager to report
food that will expire soon.

The app uses Java, XML layouts, Material components, LiveData, Room, Retrofit/TheMealDB,
Glide and WorkManager. **Prompt 1: Room Database — Cem** adds persistent pantry items,
shopping items and complete saved recipes while preserving the existing repository API.

## Setup

1. Clone `https://github.com/aceeejlrz/useItUp.git` and open the root folder in Android Studio.
2. Install Android SDK Platform **37** (the SDK folder may be named `android-37.0`), platform
   tools and an emulator system image. The minimum supported Android version is API 26.
3. The checked-in `gradle/gradle-daemon-jvm.properties` selects **JDK 25** for the Gradle
   daemon; Gradle can provision it using the configured toolchain resolver. This branch
   was built with a JDK 21 launcher, a JDK 25 daemon, Gradle 9.6.0 and Android Gradle Plugin
   9.4.1. Java source compatibility remains 17. Use the Gradle wrapper; no global Gradle
   installation is needed.
4. Let Android Studio generate `local.properties`, or set the SDK path yourself. For example:
   `sdk.dir=C\:/Users/YOUR_USER/AppData/Local/Android/Sdk`. Do not commit this machine-specific file.
5. Sync Gradle and run the `app` configuration on an emulator or device. Internet access is
   needed for initial dependency downloads and live recipe searches. Existing TheMealDB
   integration uses its public test API; there is no new API key or backend configuration.

Windows PowerShell commands (use `./gradlew` on macOS/Linux):

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:test
.\gradlew.bat :app:build
.\gradlew.bat :app:connectedDebugAndroidTest
```

`build` also builds release and runs lint. The release APK is unsigned. Debug APKs are in
`app/build/outputs/apk/debug/`. `.idea/`, local SDK settings and build outputs are excluded
from new changes; existing IDE files inherited from master are not modified by this task.

## Project structure

```text
app/src/main/java/com/team15/useitup/
  UseItUpApp.java           Application initialization, repository and reminder setup
  data/
    AppRepository.java     Shared synchronous cache, LiveData and serialized Room writes
    PantryItem.java        Pantry Room entity and immutable UI model
    ShoppingItem.java      Shopping Room entity and immutable UI model
    Recipe.java            Immutable recipe model, including full instructions
    RecipeIngredient.java  Immutable ingredient UI/network model
    SampleData.java        Search samples and first-launch seed content
    RecipeRepository.java  Existing TheMealDB search/detail network cache
    MealDbService.java     Existing Retrofit endpoints
    local/                 Room database, DAOs, converters, saved recipe entities
  ui/                      Pantry, recipes, shopping, detail, item editor and settings
  util/                    Expiry logic, ingredient matching, preferences and notifications
  worker/                  Daily expiry worker and scheduling
app/src/main/res/           XML layouts, themes, strings and sample artwork
app/src/test/              Local JVM unit tests
app/src/androidTest/       Room, cold-start, worker and application instrumentation tests
app/schemas/               Exported Room schema history (commit future versions here)
gradle/libs.versions.toml  Dependency versions and aliases, including Room
scripts/                   Repeatable emulator force-stop persistence check
```

## Room persistence and integration

The private database is `useitup.db`, schema version 1. Room dependencies are declared in
the version catalog; Java code generation uses `annotationProcessor`, without KSP or kapt.

| Table | Stored data |
| --- | --- |
| `pantry_items` | ID, name, quantity, unit, category, expiry date, original added date |
| `shopping_items` | ID, name, quantity note, checked state, source recipe, original creation timestamp |
| `SavedRecipe` | Recipe ID, name, area, category, image URL, ordered instruction steps, saved order |
| `RecipeIngredient` | Parent recipe ID, position, name, measure; repeated names are supported |
| `seed_state` | First-launch marker, committed with all seed data |

Dates use ISO-8601 strings. Instructions use a JSON array so punctuation, Unicode and
newlines survive storage. Ingredient rows use a composite `(recipeId, position)` key and
a cascading foreign key. Recipe metadata and ingredients are saved in one transaction.
No drawable resource IDs are stored: sample artwork is resolved from `SampleData` using
the stable recipe ID. Search samples remain in `SampleData`; only saved recipes enter Room.

`UseItUpApp.onCreate()` starts repository initialization before scheduling reminders.
The database opens, seeds and loads on a dedicated executor. `AppRepository.get()` waits
for this initial load when necessary, so the existing synchronous API never returns an
empty placeholder on a cold start. This is a one-time readiness wait and can briefly delay
the first screen; Room queries themselves never run on the main thread. There is no
`allowMainThreadQueries`. The worker reads the loaded cache directly from its background
thread and does not depend on an Activity or a main-thread callback.

`currentPantry()`, `findPantryItem()`, `pantryNames()`, `isSaved()`, `savedRecipes()` and
`currentShopping()` synchronously read the cache. Mutations are synchronized, update that
cache immediately, enqueue transactional writes on the same executor and publish the
existing LiveData streams. LiveData delivery may happen later than the cache update.
Undo restores the original object, including its ID, dates, checked state and timestamps.
IDs advance past restored IDs so a later addition cannot overwrite an undone item.

For this small local app, each write replaces the affected collection in a transaction.
Always write through `AppRepository`, rather than changing DAOs behind its cache. Writes
are asynchronous: `awaitIdle()` is an off-main-thread barrier for workers/tests that need
confirmation that earlier writes have committed. It must not be called from the UI.
Failed writes are logged, exposed through `getStorageErrors()` and reported on the main
and recipe-detail screens; `awaitIdle()` throws if a write failed. A forced process kill
before a queued write commits can lose that pending change, as with other asynchronous
local writes. Already committed changes survive force-stop and reopening.

Seeding is transactional and runs once per database installation, including three fully
saved sample recipes. Emptying a list does not reseed it. Clearing application data or
uninstalling removes the database and starts a new installation. There is no destructive
migration fallback; future schema changes must increment the version and add migrations.

### Saving TheMealDB meals

Once lookup returns a complete `Recipe`, a teammate can save it directly:

```java
AppRepository repository = AppRepository.get();
repository.saveRecipe(fullRecipe);             // metadata, URL, steps and all ingredients
repository.setSaved(fullRecipe.getId(), false); // remove the recipe and its ingredient rows
```

The existing `setSaved(id, true)` method is retained for full recipes already available
through `findRecipe(id)`. Unsaved network results belong to the existing main-thread
`RecipeRepository`; a background caller should pass a full `Recipe` to `saveRecipe`.
Incomplete search summaries are rejected. Recipe Detail enables saving only once full
ingredients and instructions are available. Saved recipes are looked up locally first
and remain readable after the network cache/process is gone. Image URLs are retained;
offline photos still depend on Glide's image cache, while recipe text is fully local.

## Verification

Local JVM tests cover expiry logic, ingredient matching, reminder times and TheMealDB
mapping. `RoomPersistenceTest` uses its own on-disk test database and checks:

- First-launch seeding and no reseeding after every list is emptied.
- Pantry create/update/delete/Undo, original metadata and IDs after reopening.
- Shopping checked state, source recipe, timestamps, Undo and clear-checked persistence.
- Complete remote recipe round-trips, ingredient order, repeated ingredients, updates,
  cascading deletion, saved ordering and rejection of partial search summaries.
- Immediate cache reads, LiveData delivery, concurrent mutations, transaction rollback
  and Room's rejection of main-thread queries.

For a real force-stop check on a disposable emulator, build both debug APKs:

```powershell
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest
.\scripts\verify-room-persistence.ps1
# Optional: -Adb 'D:\Android\Sdk\platform-tools\adb.exe' -Serial emulator-5556
```

The script installs those APKs with `adb install -r` (preserving existing data), grants
notification permission, inserts identifiable fixtures, confirms the
process is stopped, reopens the launcher and checks the pantry item on screen. It then
force-stops again and starts instrumentation without an Activity, checks pantry/shopping/
full saved-recipe data, verifies that the real expiry worker's notification includes the
persisted pantry item, and opens the saved recipe detail screen. Finally it removes only
its fixtures. It does not clear application data. Use an emulator with the default alert
window of at least one day. Logs and a UI hierarchy are saved under
`app/build/reports/room-force-stop/`.

The ordinary instrumentation run checks application initialization without inserting these
fixtures; the explicit script performs the process-death phases. Gradle reports are under
`app/build/reports/tests/`, `app/build/reports/androidTests/connected/` and
`app/build/reports/lint-results-debug.html`.

Verified on 2026-11-06 with the Medium Phone emulator, Android 17 / API 37:

| Check | Result |
| --- | --- |
| `:app:build` (debug/release APKs and lint) | Passed; lint: 0 errors, 19 existing warnings |
| `:app:test` | 20 unit tests passed |
| `:app:connectedDebugAndroidTest` | 15 tests passed, none skipped |
| Force-stop script | Passed: reopened pantry UI, persisted shopping/full recipe, cold worker notification and recipe detail |

All requested checks were executed. Physical devices and API 26–36 were not tested.

## Remaining work

- Barcode scanning is still the existing placeholder in the item editor.
- Add and test migrations when the Room schema changes beyond version 1.
- Before a larger release, consider an asynchronous startup screen and incremental DAO
  writes for larger collections; the current implementation preserves the synchronous API.
- Add automated write-failure recovery/retry and optional offline image downloads.
- Broaden device testing across API 26–36, accessibility settings and real devices; review
  the existing lint warnings and configure release signing as a team.

TheMealDB search, reminder scheduling and the existing screen designs remain the respective
teammates' work. This change only connects the saved-state and cold-start paths to Room.
