# Collaborative Household App Technical Spec

## 1. Overview

### Product Goal
Build a collaborative Android app for exactly two household members to manage chores, workouts, and household logistics using **Google Sheets as the only persistent database**. The app must include a built-in gamification system with XP, streaks, levels, coins, and a reward store.

### Core Constraint
- No Firebase
- No SQL database
- No alternate cloud datastore
- Google Sheets is the source of truth for all persistent app data

### Recommended Stack
- Android client: `Kotlin` + `Jetpack Compose`
- Local cache only: `Room`
- Background work: `WorkManager`
- API client: `Retrofit`
- Auth: `Google Sign-In`
- Backend/API layer: `Google Apps Script`
- Persistent datastore: `Google Sheets`

### Architecture Principle
The Android app must **never write directly to Google Sheets**. All reads and writes go through Google Apps Script endpoints that enforce validation, business rules, and conflict handling.

## 2. Scope

### MVP Features
- Google Sign-In
- Household membership validation
- Shared dashboard
- Task creation, editing, assignment, completion
- Workout logging
- Household event logging
- XP, coins, levels, and streaks
- Reward store and redemption flow
- Shared activity feed
- Read-only offline cache

### Post-MVP Features
- Recurring task auto-generation
- Push reminders
- Weekly challenges
- Shared goals
- Badges and achievements
- Streak freeze rewards
- Charts and insights

## 3. Users And Roles

### Household Members
The application supports two active users per household.

Both users can:
- Create tasks
- Assign tasks
- Complete tasks
- Log workouts
- Log household events
- Redeem rewards
- View shared household activity

### Optional Role Extension
Support a future `admin` role for changing config and managing rewards, but do not depend on it for MVP.

## 4. High-Level Architecture

### Components
1. Android app
2. Google Apps Script web app
3. Google Spreadsheet with multiple tabs

### Request Flow
1. User signs into Android app with Google account.
2. Android app calls Apps Script endpoint.
3. Apps Script validates that the user email exists in the `Users` tab.
4. Apps Script performs reads/writes to spreadsheet tabs.
5. Apps Script calculates rewards and updates ledgers.
6. Android app refreshes local state and cached views.

### Why This Is Required
Direct client-to-sheet access would expose credentials, create inconsistent reward logic, and make concurrent edits unsafe.

## 5. Spreadsheet Design

Use a single Google Spreadsheet per household.

### 5.1 `Users`
Purpose: store the two household members and their game progression.

Columns:
- `user_id`
- `email`
- `display_name`
- `photo_url`
- `role`
- `xp_total`
- `level`
- `coins_total`
- `current_streak`
- `longest_streak`
- `last_qualifying_date`
- `created_at`
- `updated_at`
- `is_active`

Notes:
- `email` is used for membership validation
- `xp_total`, `coins_total`, and streak fields are denormalized summary values

### 5.2 `Tasks`
Purpose: track all household chores and recurring responsibilities.

Columns:
- `task_id`
- `title`
- `description`
- `category`
- `assigned_to_user_id`
- `created_by_user_id`
- `status`
- `priority`
- `due_date`
- `repeat_rule`
- `xp_reward`
- `coin_reward`
- `streak_eligible`
- `completed_at`
- `completed_by_user_id`
- `created_at`
- `updated_at`
- `version`

Allowed `status` values:
- `open`
- `completed`
- `archived`

Allowed `priority` values:
- `low`
- `medium`
- `high`

### 5.3 `Workouts`
Purpose: log fitness activity for habit tracking and rewards.

Columns:
- `workout_id`
- `user_id`
- `workout_type`
- `duration_minutes`
- `intensity`
- `notes`
- `xp_reward`
- `coin_reward`
- `logged_at`
- `created_at`

### 5.4 `Household_Log`
Purpose: record non-task household actions like bills, groceries, or maintenance.

Columns:
- `log_id`
- `type`
- `title`
- `details`
- `user_id`
- `related_task_id`
- `xp_reward`
- `coin_reward`
- `logged_at`
- `created_at`

Suggested `type` values:
- `bill`
- `groceries`
- `delivery`
- `maintenance`
- `cleaning`
- `other`

### 5.5 `Rewards_Store`
Purpose: define redeemable household rewards.

Columns:
- `reward_id`
- `title`
- `description`
- `cost_coins`
- `cost_xp`
- `category`
- `is_active`
- `created_at`
- `updated_at`

### 5.6 `Reward_Redemptions`
Purpose: track reward purchases and status.

Columns:
- `redemption_id`
- `reward_id`
- `user_id`
- `cost_coins`
- `cost_xp`
- `status`
- `redeemed_at`
- `resolved_at`
- `notes`

Suggested `status` values:
- `redeemed`
- `approved`
- `rejected`
- `fulfilled`

### 5.7 `XP_Ledger`
Purpose: immutable audit trail for every XP and coin change.

Columns:
- `ledger_id`
- `user_id`
- `source_type`
- `source_id`
- `action_type`
- `xp_delta`
- `coin_delta`
- `reason`
- `created_at`

Examples:
- task completion reward
- workout reward
- streak bonus
- reward redemption deduction

### 5.8 `Streak_History`
Purpose: audit streak changes by date.

Columns:
- `entry_id`
- `user_id`
- `activity_date`
- `qualifying_action_type`
- `source_id`
- `streak_count_after`
- `created_at`

### 5.9 `Config`
Purpose: central configuration for reward and game rules.

Columns:
- `config_key`
- `config_value`
- `updated_at`

Suggested keys:
- `default_task_xp`
- `default_task_coins`
- `high_priority_task_xp`
- `high_priority_task_coins`
- `workout_xp_per_10_minutes`
- `workout_coins_flat`
- `daily_streak_bonus_xp`
- `weekly_streak_bonus_xp`
- `level_table_json`

## 6. Data Rules

### ID Conventions
- user: `u_001`
- task: `t_001`
- workout: `w_001`
- household log: `h_001`
- reward: `r_001`
- redemption: `rd_001`
- ledger: `x_001`

### Timestamp Standard
Use ISO 8601 UTC strings for all timestamps, for example:
- `2026-06-08T09:15:00Z`

### Dates
Use `YYYY-MM-DD` for date-only values like `due_date` and streak evaluation.

## 7. Core Business Logic

### 7.1 XP Rules
Recommended MVP defaults:
- Normal task completion: `20 XP`
- High priority task completion: `35 XP`
- Workout reward: `10 XP` per 10 minutes
- Household log entry: `10 XP`
- Daily all-tasks-completed bonus: `25 XP`
- 7-day streak bonus: `50 XP`

### 7.2 Coin Rules
Recommended MVP defaults:
- Task completion: `5 coins`
- Workout logged: `3 coins`
- Household log entry: `3 coins`
- Weekly consistency bonus: `10 coins`

### 7.3 Level Rules
Recommended implementation: milestone table stored in `Config`.

Example milestones:
- Level 1: `0 XP`
- Level 2: `100 XP`
- Level 3: `250 XP`
- Level 4: `450 XP`
- Level 5: `700 XP`
- Level 6: `1000 XP`

Reason:
- Easier to explain
- Easy to adjust without changing app code
- Safer than spreadsheet formulas spread across tabs

### 7.4 Streak Rules
A day counts if a user completes at least one qualifying action:
- completes a streak-eligible task
- logs a workout
- logs a qualifying household event

Rules:
- multiple qualifying actions on the same day count once for streak preservation
- if current action date is one day after `last_qualifying_date`, increment streak
- if current action date equals `last_qualifying_date`, keep current streak unchanged
- if one or more days were missed, reset streak to `1`
- update `longest_streak` when new streak exceeds it

### 7.5 Reward Redemption Rules
- User cannot redeem if `coins_total` or `xp_total` would become negative
- Redemption inserts a row into `Reward_Redemptions`
- Redemption creates a negative ledger entry in `XP_Ledger`
- Summary totals in `Users` must be updated in the same transaction flow

## 8. API Design

Apps Script should expose a JSON API through a published web app.

### 8.1 Common Response Envelope
Successful response:

```json
{
  "success": true,
  "data": {},
  "meta": {
    "serverTime": "2026-06-08T09:15:00Z"
  }
}
```

Error response:

```json
{
  "success": false,
  "error": {
    "code": "TASK_ALREADY_COMPLETED",
    "message": "Task has already been completed."
  }
}
```

### 8.2 Endpoints

#### `POST /auth/bootstrap`
Purpose:
- Validate signed-in user
- Return household context and initial profile data

Request:

```json
{
  "googleEmail": "alex@example.com"
}
```

Response data:
- current user profile
- partner profile summary
- household name if used
- config snapshot if needed

#### `GET /dashboard`
Purpose:
- Return main dashboard payload in one call

Response data:
- tasks due today
- overdue tasks
- recent activity
- current user summary
- partner summary
- rewards preview
- streak summary

#### `GET /tasks`
Query options:
- `status`
- `assignedToUserId`
- `dueDate`
- `category`

#### `POST /tasks/create`
Request:

```json
{
  "title": "Take out trash",
  "description": "Before 8 PM",
  "category": "Cleaning",
  "assignedToUserId": "u_002",
  "priority": "medium",
  "dueDate": "2026-06-08",
  "repeatRule": "weekly",
  "streakEligible": true
}
```

Server actions:
- validate fields
- generate `task_id`
- resolve default reward values
- insert row

#### `POST /tasks/update`
Request:

```json
{
  "taskId": "t_001",
  "title": "Take out trash",
  "priority": "high",
  "version": 2
}
```

Server actions:
- fetch current task
- validate version
- reject stale update if versions differ
- update row
- increment version

#### `POST /tasks/complete`
Request:

```json
{
  "taskId": "t_001",
  "completedByUserId": "u_002"
}
```

Server actions:
- verify task exists
- verify not already completed
- mark task completed
- create ledger reward entry
- update user totals
- evaluate streak
- evaluate daily bonus eligibility
- return updated balances and task state

#### `POST /workouts/log`
Request:

```json
{
  "userId": "u_001",
  "workoutType": "Running",
  "durationMinutes": 30,
  "intensity": "medium",
  "notes": "Evening run"
}
```

Server actions:
- calculate XP based on duration
- create workout row
- append ledger
- update totals
- evaluate streak

#### `POST /household/log`
Request:

```json
{
  "userId": "u_001",
  "type": "bill",
  "title": "Electricity bill paid",
  "details": "June payment complete"
}
```

Server actions:
- create log row
- create ledger entry if reward-eligible
- update totals
- evaluate streak if qualifying

#### `GET /rewards`
Purpose:
- return active rewards store

#### `POST /rewards/redeem`
Request:

```json
{
  "rewardId": "r_005",
  "userId": "u_001"
}
```

Server actions:
- verify reward exists and active
- verify balances
- create redemption row
- append negative ledger entry
- update summary balances

#### `GET /profile`
Purpose:
- return full profile, level progress, streak history, recent rewards

#### `GET /history`
Purpose:
- return merged activity timeline from tasks, workouts, logs, and redemptions

## 9. Concurrency And Consistency

### Key Rule
All state-changing actions must execute on the server side in Apps Script.

### Consistency Requirements
- No duplicate task completion rewards
- No negative balances
- No streak increment more than once per user per day
- No stale task overwrite without version check

### Conflict Strategy
- Each mutable task has a `version`
- Updates require latest known `version`
- Completion endpoint checks task `status` before awarding anything
- If two users try to complete the same task, only first valid request gets reward

### Recommended Apps Script Safeguard
Use `LockService` during write operations that touch:
- `Tasks`
- `Users`
- `XP_Ledger`
- `Reward_Redemptions`
- `Streak_History`

This reduces race conditions between overlapping requests.

## 10. Android Client Architecture

### 10.1 Modules
- `auth`
- `dashboard`
- `tasks`
- `workouts`
- `household`
- `rewards`
- `profile`
- `history`
- `core.network`
- `core.database`
- `core.model`
- `core.ui`

### 10.2 Layers

#### Presentation
- Compose screens
- ViewModels
- UI state classes

#### Domain
- Use cases
- business mapping
- validation helpers

#### Data
- Retrofit services
- Room DAOs
- repositories
- DTO to domain mappers

### 10.3 Recommended Package Structure

```text
com.example.householdapp
├── auth
├── dashboard
├── tasks
├── workouts
├── household
├── rewards
├── profile
├── history
├── core
│   ├── model
│   ├── network
│   ├── database
│   ├── repository
│   ├── ui
│   └── util
```

### 10.4 Suggested ViewModels
- `AuthViewModel`
- `DashboardViewModel`
- `TasksViewModel`
- `TaskEditorViewModel`
- `WorkoutViewModel`
- `HouseholdLogViewModel`
- `RewardsViewModel`
- `ProfileViewModel`
- `HistoryViewModel`

## 11. Android Data Models

### Domain Models
- `UserProfile`
- `PartnerSummary`
- `Task`
- `WorkoutLog`
- `HouseholdEvent`
- `RewardItem`
- `RewardRedemption`
- `LedgerEntry`
- `DashboardData`
- `ActivityFeedItem`

### Example `Task`

```kotlin
data class Task(
    val taskId: String,
    val title: String,
    val description: String?,
    val category: String,
    val assignedToUserId: String?,
    val createdByUserId: String,
    val status: TaskStatus,
    val priority: TaskPriority,
    val dueDate: String?,
    val repeatRule: String?,
    val xpReward: Int,
    val coinReward: Int,
    val streakEligible: Boolean,
    val completedAt: String?,
    val completedByUserId: String?,
    val createdAt: String,
    val updatedAt: String,
    val version: Int
)
```

## 12. Offline Strategy

### MVP Strategy
- Cache server responses locally with Room
- Allow offline viewing of last-known dashboard and task lists
- Require connectivity for all writes

### Reason
This keeps Sheets and game state consistent while still making the app feel responsive.

### Local Cache Entities
- `UserEntity`
- `TaskEntity`
- `RewardEntity`
- `ActivityFeedEntity`
- `DashboardSnapshotEntity`

### Sync Behavior
- refresh dashboard on app open
- refresh after every successful write
- periodic background sync with `WorkManager`

## 13. UI Specification

### 13.1 Sign In Screen
Elements:
- app title
- Google Sign-In button
- loading state
- access denied state

### 13.2 Dashboard Screen
Sections:
- greeting and profile summary
- today’s tasks
- overdue tasks
- streak cards
- XP and level progress
- partner progress
- recent activity
- rewards preview
- quick actions

### 13.3 Tasks Screen
Sections:
- filter chips
- task list
- floating action button for create
- complete action
- edit action

### 13.4 Task Editor Screen
Fields:
- title
- description
- category
- assignee
- due date
- priority
- repeat rule
- streak eligible toggle

### 13.5 Workout Screen
Fields:
- workout type
- duration
- intensity
- notes
- reward preview

### 13.6 Household Log Screen
Fields:
- event type
- title
- details
- optional related task

### 13.7 Rewards Store Screen
Sections:
- balance header
- reward cards
- redeem confirmation dialog
- redemption result state

### 13.8 Profile Screen
Sections:
- avatar and display name
- XP total
- coins total
- level progress
- streak summary
- recent earned activity

### 13.9 History Screen
Sections:
- reverse chronological feed
- source badges like task, workout, reward, household

## 14. Validation Rules

### Tasks
- title required
- title max 100 chars
- due date optional
- priority must be valid enum

### Workouts
- duration must be greater than 0
- duration max recommended 300 minutes for MVP

### Rewards
- cost must be non-negative
- inactive rewards cannot be redeemed

### Membership
- only emails present in `Users` and marked active may use the app

## 15. Security Requirements

### Required
- Authenticate via Google Sign-In
- Validate household membership server-side
- Reject unauthorized emails
- Centralize reward logic on server

### Avoid
- Direct spreadsheet credentials in Android app
- Trusting client-provided XP values
- Direct sheet editing from device

## 16. Performance Constraints

### Expected Safe Usage
This design is suitable for:
- 2 users
- low to moderate daily writes
- a few thousand rows per tab

### Performance Risks
- scanning entire sheets on every request
- storing too many formulas inside spreadsheet tabs
- unbounded ledger/history growth

### Mitigations
- minimize per-request full-tab scans
- keep formulas limited
- compute business rules in Apps Script
- archive old ledger rows periodically

## 17. Error Handling

### Suggested Error Codes
- `UNAUTHORIZED_USER`
- `TASK_NOT_FOUND`
- `TASK_ALREADY_COMPLETED`
- `STALE_TASK_VERSION`
- `INSUFFICIENT_BALANCE`
- `REWARD_NOT_FOUND`
- `INVALID_REQUEST`
- `SERVER_ERROR`

### Client Handling
- show readable message
- keep previous cached data visible when possible
- refresh stale task on version conflict

## 18. Suggested Apps Script Structure

### Files
- `Main.gs`
- `AuthService.gs`
- `TaskService.gs`
- `WorkoutService.gs`
- `RewardService.gs`
- `StreakService.gs`
- `LedgerService.gs`
- `SheetRepository.gs`
- `ConfigService.gs`
- `ResponseUtil.gs`

### Responsibilities
- `AuthService.gs`: validate membership
- `TaskService.gs`: CRUD and completion rules
- `WorkoutService.gs`: workout reward logic
- `RewardService.gs`: store reads and redemption
- `StreakService.gs`: streak evaluation
- `LedgerService.gs`: create immutable reward entries
- `SheetRepository.gs`: tab read/write helpers

## 19. Suggested Android Ticket Breakdown

### Phase 1: Project Setup
- initialize Android project
- add Compose navigation
- add Retrofit, Room, DataStore, WorkManager
- configure Google Sign-In

### Phase 2: Spreadsheet And Backend
- create spreadsheet tabs
- define headers
- implement bootstrap endpoint
- implement tasks endpoints

### Phase 3: Task Flow
- task list UI
- task create/edit UI
- task completion flow
- dashboard task widgets

### Phase 4: Gamification
- implement XP ledger handling
- level calculation display
- streak cards
- daily bonus rules

### Phase 5: Workout And Household Logs
- workout entry UI and endpoint
- household log entry UI and endpoint

### Phase 6: Rewards
- rewards store UI
- redemption endpoint
- balance deduction handling

### Phase 7: Polish
- offline cache
- history feed
- error handling
- loading and empty states

## 20. Acceptance Criteria

### Functional
- Two approved users can sign in and access the same household data
- Users can create, edit, assign, and complete tasks
- Completing a task updates rewards exactly once
- Users can log workouts and receive rewards
- Users can redeem store rewards if balance is sufficient
- Dashboard shows both users’ progress and current streaks

### Data Integrity
- Google Sheets remains the only persistent database
- Every XP or coin change is represented in `XP_Ledger`
- No duplicate streak increment occurs on same day
- No balance can go below zero

### UX
- Dashboard loads with cached data if offline
- Successful actions show immediate updated reward feedback
- Common flows complete in a few taps

## 21. Final Recommendation

For MVP, keep the system intentionally simple:
- one household spreadsheet
- two authorized users
- one Apps Script web app
- one Android app
- network required for writes
- cached read-only offline access

This gives you a practical architecture that respects the Google Sheets-only database requirement while still supporting collaboration and gamification safely.
