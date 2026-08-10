# Android App Skeleton

This is a starter Android project for the collaborative household app.

## Stack

- `Kotlin`
- `Jetpack Compose`
- `Material 3`
- `Navigation Compose`
- `Lifecycle ViewModel`
- `Coroutines`
- `Retrofit`
- `Moshi`

## Included Structure

- `MainActivity.kt`: Android entry point
- `HouseholdApp.kt`: root Compose app container
- `auth/AuthScreen.kt`: device registration entry screen
- `auth/AuthViewModel.kt`: bootstrap flow state holder
- `navigation/AppNavGraph.kt`: app navigation
- `dashboard`: dashboard screen and ViewModel
- `tasks`: tasks screen and ViewModel
- `workouts`: workouts screen and ViewModel
- `rewards`: rewards screen and ViewModel
- `profile`: profile screen and ViewModel
- `core/model`: domain models
- `core/network`: DTOs and Retrofit interface
- `core/repository`: starter repositories
- `core/ui/theme`: theme files

## What This Skeleton Does

- sets up a Compose Android app
- provides screen placeholders for the main product areas
- defines domain models based on the technical spec
- defines API request and response models matching the Apps Script starter
- includes repository stubs ready for real endpoint wiring
- includes ViewModels with example loading states
- includes device bootstrap scaffolding for `auth/bootstrap_device`
- includes live task loading and task completion wiring against the Apps Script API
- includes live workout logging and dashboard aggregation wiring against the Apps Script API

## What You Need To Do Next

### 1. Open in Android Studio

Open the `android-app-skeleton` folder as a Gradle project.

### 2. Set the backend URL

Update these `build.gradle.kts` values in the `app` module:

- `APPS_SCRIPT_BASE_URL`
- `APPS_SCRIPT_DEPLOYMENT_PATH`

The starter is already prefilled with the current Apps Script deployment URL and can be overridden if you redeploy.

### 3. Register the first two devices

This skeleton now includes:

- device ID bootstrap
- automatic first-device and second-device registration
- bootstrap call to `auth/bootstrap_device`

Before running it, make sure:

- your Apps Script web app is deployed and accessible
- the `Users` sheet either has zero users or at most two active user slots
- if you pre-created user rows, leave `device_id` blank so the first two devices can claim them

### 4. Connected endpoints in this starter

This starter now includes wiring for:

- `auth/bootstrap_device`
- `dashboard`
- `tasks/list`
- `tasks/create`
- `tasks/complete`
- `rewards`
- `rewards/redeem`
- `workouts/log`

Next endpoints to add:

- household log
- task edit UI flow

### 5. Add persistence

For MVP:

- add `Room` for local cache
- add `DataStore` for lightweight preferences
- add `WorkManager` for sync refresh jobs

## Current Screens

- Dashboard
- Tasks
- Workouts
- Rewards
- Profile

These are starter screens with placeholder actions and data flow.

## Recommended Next Build Order

1. verify `auth/bootstrap_device`
2. verify `dashboard`
3. verify `tasks/list`
4. verify `tasks/create`
5. verify `tasks/complete`
6. verify `workouts/log`
7. verify `rewards`

## Notes

- This is a skeleton, not a finished production app
- It is intentionally small so the team can evolve it safely
- The published Android application ID is `com.homemanager.app`
- The current bootstrap flow uses device registration and only accepts the first two devices
- `TasksScreen.kt` now calls the real backend and updates the signed-in user session after completion
- `TasksScreen.kt` now supports basic task creation
- `WorkoutsScreen.kt` now posts real workout logs and updates the signed-in user session
- `DashboardScreen.kt` now loads aggregated dashboard data from the backend
