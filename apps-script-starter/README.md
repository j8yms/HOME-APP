# Apps Script Starter Package

This is a starter backend for the collaborative household Android app that uses **Google Sheets as the only persistent database**.

## Included Files

- `appsscript.json`: Apps Script manifest
- `Main.gs`: web app entry points and request routing
- `Setup.gs`: spreadsheet setup and seed helpers
- `SheetRepository.gs`: sheet access helpers and shared utilities
- `AuthService.gs`: user bootstrap and membership validation
- `TaskService.gs`: task listing, creation, update, and completion
- `WorkoutService.gs`: workout logging
- `DashboardService.gs`: dashboard aggregation
- `LedgerService.gs`: XP and coin ledger writes
- `StreakService.gs`: streak evaluation
- `ResponseUtil.gs`: API response helpers

## What This Starter Supports

- Resolve spreadsheet from script properties or bound spreadsheet
- Initialize required tabs and headers
- Seed default config values
- Register and validate the first two household devices
- Return bootstrap data for signed-in devices
- Create and list tasks
- Complete tasks with XP, coins, and streak updates
- Log workouts with rewards
- Return dashboard summary data
- Write immutable ledger entries

## Required Spreadsheet Tabs

The starter creates these tabs:

- `Users`
- `Tasks`
- `Workouts`
- `Household_Log`
- `Rewards_Store`
- `Reward_Redemptions`
- `XP_Ledger`
- `Streak_History`
- `Config`

## Setup Steps

### 1. Create the spreadsheet

Create one Google Spreadsheet for the household.

### 2. Create an Apps Script project

You can use:

- a standalone Apps Script project, or
- a script bound to the spreadsheet

### 3. Add these files

Copy each file into the Apps Script project.

### 4. Set script property if using a standalone project

In Apps Script:

- open `Project Settings`
- add script property `SPREADSHEET_ID`
- set it to the Google Spreadsheet ID

If the script is spreadsheet-bound, the starter can use the active spreadsheet instead.

### 5. Run setup

Run:

- `initializeHouseholdSpreadsheet()`

This creates the tabs and header rows if they do not already exist.

Then optionally run:

- `seedDemoConfig()`

### 6. Add users or let devices claim the two slots

You can either:

- let the first two devices create the two household members automatically, or
- insert up to two rows into `Users` and leave `device_id` blank so the devices can claim them

If you insert rows manually, use at least:

- `user_id`
- `device_id`
- `email`
- `display_name`
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

Example:

```text
u_001 |  |  | Alex |  | member | 0 | 1 | 0 | 0 | 0 |  | 2026-06-08T00:00:00Z | 2026-06-08T00:00:00Z | true
u_002 |  |  | Sam  |  | member | 0 | 1 | 0 | 0 | 0 |  | 2026-06-08T00:00:00Z | 2026-06-08T00:00:00Z | true
```

## API Usage

Publish the Apps Script as a web app.

This starter accepts:

- `GET` with query params
- `POST` with JSON body

### Route Style

Use either:

- `route` query/body field, or
- Apps Script `pathInfo`

Example routes:

- `auth/bootstrap_device`
- `auth/bootstrap`
- `tasks/list`
- `tasks/create`
- `tasks/update`
- `tasks/complete`
- `workouts/log`
- `dashboard`

### Example Device Bootstrap Request

```json
{
  "route": "auth/bootstrap_device",
  "deviceId": "d_1234567890ab"
}
```

### Example Task Create Request

```json
{
  "route": "tasks/create",
  "title": "Take out trash",
  "description": "Before 8 PM",
  "category": "Cleaning",
  "assignedToUserId": "u_002",
  "createdByUserId": "u_001",
  "priority": "medium",
  "dueDate": "2026-06-08",
  "repeatRule": "",
  "streakEligible": true
}
```

## Important Notes

- All write actions use `LockService`
- Google Sheets remains the only persistent datastore
- `Room` in Android should be used only as a local cache, not as a second database
- This starter is designed for one household with two users and only two registered devices

## Recommended Next Backend Additions

- reward store read and redeem endpoints
- household log endpoint
- dashboard aggregation endpoint
- task recurrence generation
- stricter role-based permissions
