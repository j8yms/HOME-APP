function initializeHouseholdSpreadsheet() {
  var spreadsheet = getSpreadsheet();

  var definitions = [
    {
      name: SHEET_NAMES.USERS,
      headers: ['user_id', 'device_id', 'email', 'display_name', 'photo_url', 'role', 'household_side', 'xp_total', 'level', 'coins_total', 'current_streak', 'longest_streak', 'last_qualifying_date', 'created_at', 'updated_at', 'is_active']
    },
    {
      name: SHEET_NAMES.TASKS,
      headers: ['task_id', 'title', 'description', 'category', 'assigned_to_user_id', 'assignee_label', 'created_by_user_id', 'status', 'priority', 'due_date', 'repeat_rule', 'xp_reward', 'coin_reward', 'streak_eligible', 'completed_at', 'completed_by_user_id', 'created_at', 'updated_at', 'version']
    },
    {
      name: SHEET_NAMES.WORKOUTS,
      headers: ['workout_id', 'user_id', 'workout_type', 'duration_minutes', 'intensity', 'notes', 'xp_reward', 'coin_reward', 'logged_at', 'created_at']
    },
    {
      name: SHEET_NAMES.HOUSEHOLD_LOG,
      headers: ['log_id', 'type', 'title', 'details', 'user_id', 'related_task_id', 'xp_reward', 'coin_reward', 'logged_at', 'created_at']
    },
    {
      name: SHEET_NAMES.REWARDS_STORE,
      headers: ['reward_id', 'title', 'description', 'cost_coins', 'cost_xp', 'category', 'is_active', 'hide_from_partner', 'created_by_user_id', 'created_at', 'updated_at']
    },
    {
      name: SHEET_NAMES.REWARD_REDEMPTIONS,
      headers: ['redemption_id', 'reward_id', 'user_id', 'cost_coins', 'cost_xp', 'status', 'redeemed_at', 'resolved_at', 'notes']
    },
    {
      name: SHEET_NAMES.XP_LEDGER,
      headers: ['ledger_id', 'user_id', 'source_type', 'source_id', 'action_type', 'xp_delta', 'coin_delta', 'reason', 'created_at']
    },
    {
      name: SHEET_NAMES.STREAK_HISTORY,
      headers: ['entry_id', 'user_id', 'activity_date', 'qualifying_action_type', 'source_id', 'streak_count_after', 'created_at']
    },
    {
      name: SHEET_NAMES.CONFIG,
      headers: ['config_key', 'config_value', 'updated_at']
    },
    {
      name: SHEET_NAMES.MONEY_TRANSACTIONS,
      headers: ['transaction_id', 'type', 'description', 'category', 'amount', 'wallet', 'user_id', 'created_at']
    },
    {
      name: SHEET_NAMES.BILLS,
      headers: ['bill_id', 'title', 'category', 'amount', 'due_date', 'status', 'paid_by_user_id', 'paid_at', 'created_by_user_id', 'created_at', 'updated_at']
    },
    {
      name: SHEET_NAMES.SHOPPING_ITEMS,
      headers: ['item_id', 'title', 'category', 'estimated_cost', 'status', 'purchased_by_user_id', 'purchased_at', 'added_by_user_id', 'created_at', 'updated_at']
    },
    {
      name: SHEET_NAMES.REFERENCE_LIBRARY,
      headers: ['entry_id', 'category', 'title', 'content', 'created_by_user_id', 'updated_at', 'created_at']
    }
  ];

  definitions.forEach(function(definition) {
    var sheet = spreadsheet.getSheetByName(definition.name);
    if (!sheet) {
      sheet = spreadsheet.insertSheet(definition.name);
    }

    if (sheet.getLastRow() === 0) {
      sheet.getRange(1, 1, 1, definition.headers.length).setValues([definition.headers]);
    } else if (sheet.getLastColumn() === 0) {
      sheet.getRange(1, 1, 1, definition.headers.length).setValues([definition.headers]);
    }
  });

  return { success: true, message: 'Spreadsheet initialized.' };
}

function migrateHouseholdSpreadsheet() {
  ensureColumn(SHEET_NAMES.TASKS, 'assignee_label');
  ensureColumn(SHEET_NAMES.USERS, 'household_side');
  ensureColumn(SHEET_NAMES.REWARDS_STORE, 'hide_from_partner');
  ensureColumn(SHEET_NAMES.REWARDS_STORE, 'created_by_user_id');
  ensureColumn(SHEET_NAMES.MONEY_TRANSACTIONS, 'wallet');
  ensureSheet(
    SHEET_NAMES.REFERENCE_LIBRARY,
    ['entry_id', 'category', 'title', 'content', 'created_by_user_id', 'updated_at', 'created_at']
  );
  upsertConfig('vacation_goal', 2000);
  upsertConfig('dream_goal', 10000);
  upsertConfig('currency', 'USD');

  return { success: true, message: 'Migration applied.' };
}

function ensureSheet(sheetName, headers) {
  var sheet = spreadsheetSafeGet(sheetName);
  if (!sheet) {
    sheet = getSpreadsheet().insertSheet(sheetName);
  }
  if (sheet.getLastRow() === 0) {
    sheet.getRange(1, 1, 1, headers.length).setValues([headers]);
  }
}

function ensureColumn(sheetName, header) {
  var sheet = spreadsheetSafeGet(sheetName);
  if (!sheet) {
    return;
  }

  var headers = sheet.getRange(1, 1, 1, sheet.getLastColumn()).getValues()[0];
  if (headers.indexOf(header) === -1) {
    sheet.getRange(1, headers.length + 1).setValue(header);
  }
}

function spreadsheetSafeGet(sheetName) {
  try {
    return getSpreadsheet().getSheetByName(sheetName);
  } catch (e) {
    return null;
  }
}

function seedDemoConfig() {
  var configSheetName = SHEET_NAMES.CONFIG;
  var existing = getConfigMap();
  var defaults = [
    ['default_task_xp', '20'],
    ['default_task_coins', '5'],
    ['high_priority_task_xp', '35'],
    ['high_priority_task_coins', '8'],
    ['workout_xp_per_10_minutes', '10'],
    ['workout_coins_flat', '3'],
    ['household_log_xp', '10'],
    ['household_log_coins', '3'],
    ['daily_streak_bonus_xp', '25'],
    ['weekly_streak_bonus_xp', '50'],
    ['vacation_goal', '2000'],
    ['dream_goal', '10000'],
    ['currency', 'USD'],
    ['level_table_json', '{"1":0,"2":100,"3":250,"4":450,"5":700,"6":1000}']
  ];

  defaults.forEach(function(pair) {
    if (existing[pair[0]] === undefined) {
      appendRow(configSheetName, {
        config_key: pair[0],
        config_value: pair[1],
        updated_at: nowIso()
      });
    }
  });

  return { success: true, message: 'Default config seeded.' };
}
