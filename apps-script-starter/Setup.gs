function initializeHouseholdSpreadsheet() {
  var spreadsheet = getSpreadsheet();

  var definitions = [
    {
      name: SHEET_NAMES.USERS,
      headers: ['user_id', 'device_id', 'email', 'display_name', 'photo_url', 'role', 'household_side', 'household_id', 'xp_total', 'level', 'coins_total', 'current_streak', 'longest_streak', 'last_qualifying_date', 'created_at', 'updated_at', 'is_active']
    },
    {
      name: SHEET_NAMES.TASKS,
      headers: ['task_id', 'title', 'description', 'category', 'assigned_to_user_id', 'assignee_label', 'created_by_user_id', 'household_id', 'status', 'priority', 'due_date', 'repeat_rule', 'xp_reward', 'coin_reward', 'streak_eligible', 'completed_at', 'completed_by_user_id', 'created_at', 'updated_at', 'version']
    },
    {
      name: SHEET_NAMES.WORKOUTS,
      headers: ['workout_id', 'user_id', 'workout_type', 'duration_minutes', 'intensity', 'notes', 'xp_reward', 'coin_reward', 'logged_at', 'created_at']
    },
    {
      name: SHEET_NAMES.HOUSEHOLD_LOG,
      headers: ['log_id', 'type', 'title', 'details', 'user_id', 'related_task_id', 'household_id', 'xp_reward', 'coin_reward', 'logged_at', 'created_at']
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
      headers: ['transaction_id', 'type', 'description', 'category', 'amount', 'wallet', 'user_id', 'household_id', 'request_id', 'created_at']
    },
    {
      name: SHEET_NAMES.BILLS,
      headers: ['bill_id', 'title', 'category', 'amount', 'due_date', 'status', 'frequency', 'auto_pay', 'paid_by_user_id', 'paid_at', 'created_by_user_id', 'household_id', 'created_at', 'updated_at']
    },
    {
      name: SHEET_NAMES.SHOPPING_ITEMS,
      headers: ['item_id', 'title', 'category', 'estimated_cost', 'status', 'purchased_by_user_id', 'purchased_at', 'added_by_user_id', 'household_id', 'created_at', 'updated_at']
    },
    {
      name: SHEET_NAMES.REFERENCE_LIBRARY,
      headers: ['entry_id', 'category', 'title', 'content', 'created_by_user_id', 'updated_at', 'created_at']
    },
    {
      name: SHEET_NAMES.EVENTS,
      headers: ['event_id', 'title', 'category', 'start', 'end', 'description', 'household_id', 'created_by_user_id', 'created_at']
    },
    {
      name: SHEET_NAMES.MAINTENANCE,
      headers: ['maintenance_id', 'title', 'category', 'due_date', 'priority', 'status', 'assigned_to_user_id', 'household_id', 'created_at']
    },
    {
      name: SHEET_NAMES.SUBSCRIPTIONS,
      headers: ['subscription_id', 'service_name', 'amount', 'next_renewal_date', 'billing_interval', 'termination_rule', 'is_active', 'user_id', 'household_id', 'created_at', 'updated_at']
    },
    {
      name: SHEET_NAMES.SAVINGS_GOALS,
      headers: ['goal_id', 'name', 'target_amount', 'current_amount', 'currency', 'deadline', 'priority', 'status', 'household_id', 'created_by_user_id', 'created_at', 'updated_at']
    },
    {
      name: SHEET_NAMES.BUDGETS,
      headers: ['budget_id', 'household_id', 'user_id', 'category', 'month', 'budget_limit', 'created_by_user_id', 'created_at', 'updated_at']
    },
    {
      name: SHEET_NAMES.NET_WORTH_SNAPSHOTS,
      headers: ['snapshot_id', 'household_id', 'total_assets', 'total_liabilities', 'net_worth', 'snapshot_date', 'created_at']
    },
    {
      name: SHEET_NAMES.TRANSFERS,
      headers: ['transfer_id', 'source_account', 'destination_target', 'amount', 'direction', 'source_balance', 'destination_balance', 'user_id', 'household_id', 'created_at']
    },
    {
      name: SHEET_NAMES.ASSETS,
      headers: ['asset_id', 'household_id', 'user_id', 'name', 'category', 'value', 'notes', 'created_at', 'updated_at']
    },
    {
      name: SHEET_NAMES.LIABILITIES,
      headers: ['liability_id', 'household_id', 'user_id', 'name', 'category', 'amount_owed', 'interest_rate', 'minimum_payment', 'notes', 'created_at', 'updated_at']
    },
    {
      name: SHEET_NAMES.INVESTMENTS,
      headers: ['investment_id', 'household_id', 'user_id', 'name', 'type', 'provider', 'reference', 'starting_balance', 'amount_invested', 'current_value', 'yield_pct', 'notes', 'created_at', 'updated_at']
    },
    {
      name: SHEET_NAMES.INVESTMENT_ACCOUNTS,
      headers: ['account_id', 'household_id', 'account_name', 'account_type', 'currency', 'balance', 'notes', 'created_at', 'updated_at']
    },
    {
      name: SHEET_NAMES.TRADE_JOURNAL,
      headers: ['trade_id', 'household_id', 'user_id', 'symbol', 'action', 'quantity', 'price', 'entry_price', 'stop_loss', 'take_profit', 'exit_price', 'pnl', 'fees', 'strategy', 'status', 'closed_at', 'notes', 'created_at', 'updated_at']
    },
    {
      name: SHEET_NAMES.WEALTH_MILESTONES,
      headers: ['milestone_id', 'household_id', 'user_id', 'title', 'target_amount', 'current_amount', 'monthly_contribution', 'target_date', 'achieved', 'achieved_at', 'notes', 'created_at', 'updated_at']
    },
    {
      name: SHEET_NAMES.INVESTMENT_TRANSACTIONS,
      headers: ['transaction_id', 'household_id', 'user_id', 'investment_id', 'type', 'amount', 'notes', 'occurred_at', 'created_at']
    },
    {
      name: SHEET_NAMES.PASSIVE_INCOME,
      headers: ['income_id', 'household_id', 'user_id', 'name', 'source_type', 'amount_monthly', 'status', 'notes', 'created_at', 'updated_at']
    },
    {
      name: SHEET_NAMES.PROP_ACCOUNTS,
      headers: ['account_id', 'household_id', 'user_id', 'name', 'provider', 'currency', 'balance', 'status', 'notes', 'created_at', 'updated_at']
    },
    {
      name: SHEET_NAMES.PROP_PAYOUTS,
      headers: ['payout_id', 'household_id', 'user_id', 'account_id', 'amount', 'currency', 'status', 'paid_at', 'notes', 'created_at']
    },
    {
      name: SHEET_NAMES.REQUEST_LOG,
      headers: ['request_id', 'route', 'response', 'created_at']
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

  invalidateSheetCache();

  return { success: true, message: 'Spreadsheet initialized.' };
}

function migrateHouseholdSpreadsheet() {
  initializeHouseholdSpreadsheet();

  // Columns added after the original sheet definitions. ensureColumn is
  // idempotent, so this is safe to run on every deployment.
  var ensureColumns = [
    [SHEET_NAMES.USERS, 'household_id'],
    [SHEET_NAMES.TASKS, 'household_id'],
    [SHEET_NAMES.TASKS, 'assignee_label'],
    [SHEET_NAMES.USERS, 'household_side'],
    [SHEET_NAMES.REWARDS_STORE, 'hide_from_partner'],
    [SHEET_NAMES.REWARDS_STORE, 'created_by_user_id'],
    [SHEET_NAMES.MONEY_TRANSACTIONS, 'wallet'],
    [SHEET_NAMES.MONEY_TRANSACTIONS, 'household_id'],
    [SHEET_NAMES.MONEY_TRANSACTIONS, 'request_id'],
    [SHEET_NAMES.BILLS, 'household_id'],
    [SHEET_NAMES.BILLS, 'frequency'],
    [SHEET_NAMES.BILLS, 'auto_pay'],
    [SHEET_NAMES.SHOPPING_ITEMS, 'household_id'],
    [SHEET_NAMES.HOUSEHOLD_LOG, 'household_id'],
    [SHEET_NAMES.EVENTS, 'household_id'],
    [SHEET_NAMES.MAINTENANCE, 'household_id'],
    [SHEET_NAMES.SUBSCRIPTIONS, 'household_id'],
    [SHEET_NAMES.SUBSCRIPTIONS, 'amount'],
    [SHEET_NAMES.SAVINGS_GOALS, 'household_id'],
    [SHEET_NAMES.SAVINGS_GOALS, 'created_by_user_id'],
    [SHEET_NAMES.SAVINGS_GOALS, 'updated_at'],
    [SHEET_NAMES.BUDGETS, 'household_id'],
    [SHEET_NAMES.BUDGETS, 'user_id'],
    [SHEET_NAMES.BUDGETS, 'created_by_user_id'],
    [SHEET_NAMES.BUDGETS, 'updated_at'],
    [SHEET_NAMES.NET_WORTH_SNAPSHOTS, 'household_id'],
    [SHEET_NAMES.NET_WORTH_SNAPSHOTS, 'total_assets'],
    [SHEET_NAMES.NET_WORTH_SNAPSHOTS, 'total_liabilities'],
    [SHEET_NAMES.TRANSFERS, 'household_id'],
    [SHEET_NAMES.TRANSFERS, 'source_balance'],
    [SHEET_NAMES.TRANSFERS, 'destination_balance'],
    [SHEET_NAMES.ASSETS, 'household_id'],
    [SHEET_NAMES.ASSETS, 'user_id'],
    [SHEET_NAMES.ASSETS, 'created_at'],
    [SHEET_NAMES.LIABILITIES, 'household_id'],
    [SHEET_NAMES.LIABILITIES, 'user_id'],
    [SHEET_NAMES.LIABILITIES, 'interest_rate'],
    [SHEET_NAMES.LIABILITIES, 'minimum_payment'],
    [SHEET_NAMES.LIABILITIES, 'created_at'],
    [SHEET_NAMES.INVESTMENTS, 'household_id'],
    [SHEET_NAMES.INVESTMENTS, 'user_id'],
    [SHEET_NAMES.INVESTMENTS, 'provider'],
    [SHEET_NAMES.INVESTMENTS, 'reference'],
    [SHEET_NAMES.INVESTMENTS, 'starting_balance'],
    [SHEET_NAMES.INVESTMENTS, 'amount_invested'],
    [SHEET_NAMES.INVESTMENTS, 'notes'],
    [SHEET_NAMES.INVESTMENTS, 'created_at'],
    [SHEET_NAMES.INVESTMENT_ACCOUNTS, 'household_id'],
    [SHEET_NAMES.INVESTMENT_ACCOUNTS, 'currency'],
    [SHEET_NAMES.INVESTMENT_ACCOUNTS, 'notes'],
    [SHEET_NAMES.INVESTMENT_ACCOUNTS, 'created_at'],
    [SHEET_NAMES.TRADE_JOURNAL, 'household_id'],
    [SHEET_NAMES.TRADE_JOURNAL, 'user_id'],
    [SHEET_NAMES.TRADE_JOURNAL, 'entry_price'],
    [SHEET_NAMES.TRADE_JOURNAL, 'stop_loss'],
    [SHEET_NAMES.TRADE_JOURNAL, 'take_profit'],
    [SHEET_NAMES.TRADE_JOURNAL, 'exit_price'],
    [SHEET_NAMES.TRADE_JOURNAL, 'pnl'],
    [SHEET_NAMES.TRADE_JOURNAL, 'fees'],
    [SHEET_NAMES.TRADE_JOURNAL, 'strategy'],
    [SHEET_NAMES.TRADE_JOURNAL, 'status'],
    [SHEET_NAMES.TRADE_JOURNAL, 'closed_at'],
    [SHEET_NAMES.TRADE_JOURNAL, 'updated_at'],
    [SHEET_NAMES.WEALTH_MILESTONES, 'household_id'],
    [SHEET_NAMES.WEALTH_MILESTONES, 'user_id'],
    [SHEET_NAMES.WEALTH_MILESTONES, 'current_amount'],
    [SHEET_NAMES.WEALTH_MILESTONES, 'monthly_contribution'],
    [SHEET_NAMES.WEALTH_MILESTONES, 'target_date'],
    [SHEET_NAMES.WEALTH_MILESTONES, 'notes'],
    [SHEET_NAMES.WEALTH_MILESTONES, 'updated_at']
  ];

  ensureColumns.forEach(function(pair) {
    ensureColumn(pair[0], pair[1]);
  });

  invalidateSheetCache();

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
  invalidateSheetCache(sheetName);
}

function ensureColumn(sheetName, header) {
  var sheet = spreadsheetSafeGet(sheetName);
  if (!sheet) {
    return;
  }

  var lastColumn = Math.max(sheet.getLastColumn(), 1);
  var headers = sheet.getRange(1, 1, 1, lastColumn).getValues()[0];
  if (headers.indexOf(header) === -1) {
    sheet.getRange(1, headers.length + 1).setValue(header);
    invalidateSheetCache(sheetName);
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
    ['household_timezone', 'Africa/Nairobi'],
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
