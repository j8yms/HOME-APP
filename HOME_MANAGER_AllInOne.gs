// HOME MANAGER - All-in-one Apps Script backend
// Integrated multi-user authentication, analytics, ledger, budgets, subscriptions, and transfers

function doGet(e) {
  try {
    ensureInitialized();
    var payload = Object.assign({}, (e && e.parameter) || {});
    var route = resolveRoute(e, payload);
    return dispatchRoute(route, payload);
  } catch (error) {
    return handleRouteError(error);
  }
}

function doPost(e) {
  try {
    ensureInitialized();
    var payload = parsePostBody(e);
    var route = resolveRoute(e, payload);
    return dispatchRoute(route, payload);
  } catch (error) {
    return handleRouteError(error);
  }
}

function ensureInitialized() {
  try {
    var spreadsheet = getSpreadsheet();
    var configSheet = spreadsheet.getSheetByName(SHEET_NAMES.CONFIG);
    var billsSheet = spreadsheet.getSheetByName(SHEET_NAMES.BILLS);
    var shoppingSheet = spreadsheet.getSheetByName(SHEET_NAMES.SHOPPING_ITEMS);
    if (!configSheet || configSheet.getLastRow() <= 1 || !billsSheet || !shoppingSheet) {
      initializeHouseholdSpreadsheet();
      if (!configSheet || configSheet.getLastRow() <= 1) {
        seedDemoConfig();
      }
    }
  } catch (error) {
    // Initialization is best-effort; individual routes surface their own errors.
  }
}

function resolveRoute(e, payload) {
  var route = normalizeString((payload && payload.route) || '');
  if (route) {
    return route;
  }
  if (e && e.pathInfo) {
    return normalizeString(e.pathInfo);
  }
  throw new Error('Route is required.');
}

function parsePostBody(e) {
  if (!e || !e.postData || !e.postData.contents) {
    return {};
  }
  return JSON.parse(e.postData.contents);
}

function dispatchRoute(route, payload) {
  switch (route) {
    case 'health':
      return jsonSuccess({ status: 'ok' });
    case 'auth/bootstrap':
      return jsonSuccess(bootstrapUser(payload));
    case 'auth/bootstrap_device':
      return jsonSuccess(bootstrapDevice(payload));
    case 'dashboard':
      return jsonSuccess(getDashboard(payload));
    case 'tasks/list':
      return jsonSuccess({ tasks: listTasks(payload) });
    case 'tasks/create':
      return jsonSuccess({ task: createTask(payload) });
    case 'tasks/update':
      return jsonSuccess({ task: updateTask(payload) });
    case 'tasks/complete':
      return jsonSuccess(completeTask(payload));
    case 'workouts/log':
      return jsonSuccess(logWorkout(payload));
    case 'rewards':
      return jsonSuccess({ rewards: getRewards(payload) });
    case 'rewards/redeem':
      return jsonSuccess(redeemReward(payload));
    case 'household/log/list':
      return jsonSuccess({ logs: listHouseholdLogs(payload) });
    case 'household/log/create':
      return jsonSuccess(createHouseholdLog(payload));
    case 'transfers/list':
      return jsonSuccess({ transfers: listTransfers(payload) });
    case 'transfers/execute':
      return jsonSuccess(executeTransfer(payload));
    case 'analytics/chart':
      return jsonSuccess(getAnalytics(payload));
    case 'analytics/category':
      return jsonSuccess(getCategoryInsights(payload));
    case 'ledger/list':
      return jsonSuccess({ ledger: listLedger(payload) });
    case 'finance/summary':
      return jsonSuccess(getFinanceSummary(payload));
    case 'finance/add_transaction':
      return jsonSuccess(addTransaction(payload));
    case 'finance/goals':
      return jsonSuccess(setFinanceGoals(payload));
    case 'finance/currency':
      return jsonSuccess(setFinanceCurrency(payload));
    case 'bills/list':
      return jsonSuccess({ bills: listBills(payload) });
    case 'bills/create':
      return jsonSuccess({ bill: createBill(payload) });
    case 'bills/update':
      return jsonSuccess({ bill: updateBill(payload) });
    case 'shopping/list':
      return jsonSuccess({ items: listShoppingItems(payload) });
    case 'shopping/create':
      return jsonSuccess({ item: createShoppingItem(payload) });
    case 'shopping/update':
      return jsonSuccess({ item: updateShoppingItem(payload) });
    case 'budgets/list':
      return jsonSuccess({ budgets: listBudgets(payload) });
    case 'budgets/create':
      return jsonSuccess({ budget: createBudget(payload) });
    case 'subscriptions/list':
      return jsonSuccess({ subscriptions: listSubscriptions(payload) });
    case 'wealth/investment_accounts':
      return jsonSuccess({ accounts: listInvestmentAccounts(payload) });
    case 'wealth/investment_accounts/create':
      return jsonSuccess({ account: createInvestmentAccount(payload) });
    case 'wealth/investment_accounts/update':
      return jsonSuccess({ account: updateInvestmentAccount(payload) });
    case 'wealth/investment_accounts/delete':
      return jsonSuccess({ success: true });
    case 'wealth/mmf':
      return jsonSuccess({ accounts: listMMFAccounts(payload) });
    case 'wealth/mmf/create':
      return jsonSuccess({ account: createMMFAccount(payload) });
    case 'wealth/mmf/update':
      return jsonSuccess({ account: updateMMFAccount(payload) });
    case 'wealth/mmf/valuation':
      return jsonSuccess({ valuation: updateMMFValuation(payload) });
    case 'wealth/sacco':
      return jsonSuccess({ accounts: listSACCOAccounts(payload) });
    case 'wealth/sacco/create':
      return jsonSuccess({ account: createSACCOAccount(payload) });
    case 'wealth/sacco/deposit':
      return jsonSuccess({ transaction: recordSACCODeposit(payload) });
    case 'wealth/sacco/withdrawal':
      return jsonSuccess({ transaction: recordSACCOWithdrawal(payload) });
    case 'wealth/trading_account':
      return jsonSuccess({ account: getTradingAccount(payload) });
    case 'wealth/trading_account/create':
      return jsonSuccess({ account: createTradingAccount(payload) });
    case 'wealth/trade_journal':
      return jsonSuccess({ trades: listTradeJournal(payload) });
    case 'wealth/trade_journal/create':
      return jsonSuccess({ trade: createTradeJournalEntry(payload) });
    case 'wealth/milestones':
      return jsonSuccess({ milestones: listWealthMilestones(payload) });
    case 'wealth/milestones/create':
      return jsonSuccess({ milestone: createWealthMilestone(payload) });
    case 'wealth/financial_freedom':
      return jsonSuccess({ progress: calculateFinancialFreedom(payload) });
    case 'wealth/asset_register':
      return jsonSuccess({ assets: listAssetRegister(payload) });
    case 'wealth/dashboard':
      return jsonSuccess({ dashboard: getWealthDashboard(payload) });
    case 'wealth/timeline':
      return jsonSuccess({ timeline: getWealthTimeline(payload) });
    case 'wealth/alerts':
      return jsonSuccess({ alerts: listWealthAlerts(payload) });
    case 'wealth/financial_habits':
      return jsonSuccess({ habits: getFinancialHabits(payload) });
    case 'wealth/report':
      return jsonSuccess({ report: generateWealthReport(payload) });
    case 'library/list':
      return jsonSuccess({ subscriptions: listSubscriptions(payload) });
    case 'subscriptions/create':
      return jsonSuccess(createSubscription(payload));
    case 'subscriptions/update':
      return jsonSuccess({ subscription: updateSubscription(payload) });
    default:
      return jsonError('ROUTE_NOT_FOUND', 'Unsupported route: ' + route);
  }
}

function handleRouteError(error) {
  var code = normalizeErrorCode(error);
  return jsonError(code, error.message || 'Unexpected server error.');
}

function ping() {
  return { status: 'ok' };
}

function normalizeErrorCode(error) {
  var message = (error && error.message) || 'SERVER_ERROR';
  if (message === 'TASK_NOT_FOUND') return 'TASK_NOT_FOUND';
  if (message === 'TASK_ALREADY_COMPLETED') return 'TASK_ALREADY_COMPLETED';
  if (message === 'STALE_TASK_VERSION') return 'STALE_TASK_VERSION';
  if (message.indexOf('required') >= 0) return 'INVALID_REQUEST';
  return 'SERVER_ERROR';
}

function jsonSuccess(data, meta) {
  return jsonOutput({
    success: true,
    data: data || {},
    meta: Object.assign({ serverTime: nowIso() }, meta || {})
  });
}

function jsonError(code, message, details) {
  return jsonOutput({
    success: false,
    error: {
      code: code,
      message: message,
      details: details || null
    }
  });
}

function jsonOutput(payload) {
  return ContentService
    .createTextOutput(JSON.stringify(payload))
    .setMimeType(ContentService.MimeType.JSON);
}

function nowIso() {
  return new Date().toISOString();
}

var SHEET_NAMES = {
  USERS: 'Users',
  TASKS: 'Tasks',
  WORKOUTS: 'Workouts',
  HOUSEHOLD_LOG: 'Household_Log',
  REWARDS_STORE: 'Rewards_Store',
  REWARD_REDEMPTIONS: 'Reward_Redemptions',
  XP_LEDGER: 'XP_Ledger',
  STREAK_HISTORY: 'Streak_History',
  CONFIG: 'Config',
  ANALYTICS: 'Analytics',
  LEDGER: 'Ledger',
  BUDGETS: 'Budgets',
  SUBSCRIPTIONS: 'Subscriptions',
  BILLS: 'Bills',
  SHOPPING_ITEMS: 'Shopping_Items'
};

function getSpreadsheet() {
  var spreadsheetId = PropertiesService.getScriptProperties().getProperty('SPREADSHEET_ID');
  if (spreadsheetId) {
    return SpreadsheetApp.openById(spreadsheetId);
  }
  var active = SpreadsheetApp.getActiveSpreadsheet();
  if (!active) {
    throw new Error('No spreadsheet available. Set script property SPREADSHEET_ID or use a bound script.');
  }
  return active;
}

function getSheet(sheetName) {
  var sheet = getSpreadsheet().getSheetByName(sheetName);
  if (!sheet) {
    throw new Error('Missing required sheet: ' + sheetName);
  }
  return sheet;
}

function getSheetData(sheetName) {
  var sheet = getSheet(sheetName);
  var values = sheet.getDataRange().getValues();
  if (!values || values.length < 2) {
    return [];
  }
  var headers = values[0];
  return values.slice(1).filter(function(row) {
    return row.some(function(cell) { return cell !== ''; });
  }).map(function(row, index) {
    var obj = {};
    headers.forEach(function(header, colIndex) {
      obj[header] = row[colIndex];
    });
    obj.__rowIndex = index + 2;
    return obj;
  });
}

function appendRow(sheetName, object, headers) {
  var sheet = getSheet(sheetName);
  var sheetHeaders = headers || getHeaders(sheetName);
  var row = sheetHeaders.map(function(header) {
    return toSheetValue(object[header]);
  });
  sheet.appendRow(row);
}

function updateRowByIndex(sheetName, rowIndex, object, headers) {
  var sheet = getSheet(sheetName);
  var sheetHeaders = headers || getHeaders(sheetName);
  var row = sheetHeaders.map(function(header) {
    return toSheetValue(object[header]);
  });
  sheet.getRange(rowIndex, 1, 1, row.length).setValues([row]);
}

function getHeaders(sheetName) {
  var sheet = getSheet(sheetName);
  return sheet.getRange(1, 1, 1, sheet.getLastColumn()).getValues()[0];
}

function findOneBy(sheetName, predicate) {
  var rows = getSheetData(sheetName);
  for (var i = 0; i < rows.length; i += 1) {
    if (predicate(rows[i])) {
      return rows[i];
    }
  }
  return null;
}

function findManyBy(sheetName, predicate) {
  return getSheetData(sheetName).filter(predicate);
}

function getConfigMap() {
  var rows = getSheetData(SHEET_NAMES.CONFIG);
  var config = {};
  rows.forEach(function(row) {
    config[String(row.config_key)] = row.config_value;
  });
  return config;
}

function getConfigNumber(key, defaultValue) {
  var map = getConfigMap();
  var raw = map[key];
  if (raw === undefined || raw === null || raw === '') {
    return defaultValue;
  }
  return Number(raw);
}

function getConfigString(key, defaultValue) {
  var map = getConfigMap();
  var raw = map[key];
  if (raw === undefined || raw === null || raw === '') {
    return defaultValue;
  }
  return String(raw);
}

function upsertConfig(key, value) {
  var configSheetName = SHEET_NAMES.CONFIG;
  var rows = getSheetData(configSheetName);
  var row = null;
  for (var i = 0; i < rows.length; i += 1) {
    if (rows[i].config_key === key) {
      row = rows[i];
      break;
    }
  }
  if (row) {
    row.config_value = String(value);
    row.updated_at = nowIso();
    updateRowByIndex(configSheetName, row.__rowIndex, row);
  } else {
    appendRow(configSheetName, {
      config_key: key,
      config_value: String(value),
      updated_at: nowIso()
    });
  }
  return value;
}

function generateId(prefix, sheetName) {
  return prefix + '_' + Utilities.getUuid().replace(/-/g, '').slice(0, 10);
}

function todayDateString() {
  return Utilities.formatDate(new Date(), 'UTC', 'yyyy-MM-dd');
}

function toSheetValue(value) {
  if (value === undefined || value === null) {
    return '';
  }
  if (typeof value === 'boolean') {
    return value ? 'true' : 'false';
  }
  return value;
}

function toBoolean(value) {
  return String(value).toLowerCase() === 'true';
}

function toInt(value, fallback) {
  var parsed = parseInt(value, 10);
  return isNaN(parsed) ? (fallback || 0) : parsed;
}

function normalizeString(value) {
  return value === undefined || value === null ? '' : String(value).trim();
}

// ============================================================
// Multi-User Account Infrastructure
// ============================================================

function initializeHouseholdSpreadsheet() {
  var spreadsheet = getSpreadsheet();

  var definitions = [
    {
      name: SHEET_NAMES.USERS,
      headers: ['user_id', 'household_id', 'device_id', 'email', 'display_name', 'photo_url', 'role', 'xp_total', 'level', 'coins_total', 'current_streak', 'longest_streak', 'last_qualifying_date', 'created_at', 'updated_at', 'is_active', 'personal_identity']
    },
    {
      name: SHEET_NAMES.TASKS,
      headers: ['task_id', 'title', 'description', 'category', 'assigned_to_user_id', 'created_by_user_id', 'status', 'priority', 'due_date', 'repeat_rule', 'xp_reward', 'coin_reward', 'streak_eligible', 'completed_at', 'completed_by_user_id', 'created_at', 'updated_at', 'version']
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
      headers: ['reward_id', 'title', 'description', 'cost_coins', 'cost_xp', 'category', 'is_active', 'created_at', 'updated_at']
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
      name: SHEET_NAMES.ANALYTICS,
      headers: ['category', 'expenditure_pct', 'total_spent', 'category_type', 'color']
    },
    {
      name: SHEET_NAMES.LEDGER,
      headers: ['ledger_id', 'user_id', 'source_type', 'source_id', 'action_type', 'amount', 'direction', 'description', 'timestamp', 'category']
    },
    {
      name: SHEET_NAMES.BUDGETS,
      headers: ['budget_id', 'user_id', 'category', 'month', 'current_spent', 'budget_limit', 'progress_pct', 'status']
    },
    {
      name: SHEET_NAMES.SUBSCRIPTIONS,
      headers: ['subscription_id', 'user_id', 'service_name', 'next_renewal_date', 'billing_interval', 'termination_rule', 'is_active', 'created_at', 'updated_at']
    },
    {
      name: SHEET_NAMES.BILLS,
      headers: ['bill_id', 'title', 'category', 'amount', 'due_date', 'status', 'paid_by_user_id', 'paid_at', 'created_by_user_id', 'created_at', 'updated_at']
    },
    {
      name: SHEET_NAMES.SHOPPING_ITEMS,
      headers: ['item_id', 'title', 'category', 'estimated_cost', 'status', 'purchased_by_user_id', 'purchased_at', 'added_by_user_id', 'created_at', 'updated_at']
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

function bootstrapUser(payload) {
  payload = payload || {};
  var email = normalizeString(payload.googleEmail).toLowerCase();
  if (!email) {
    throw new Error('googleEmail is required.');
  }

  var user = getActiveUserByEmail(email);
  if (!user) {
    return {
      authorized: false,
      reason: 'Email is not authorized for this household.'
    };
  }

  var partner = findManyBy(SHEET_NAMES.USERS, function(row) {
    return toBoolean(row.is_active) && row.user_id !== user.user_id;
  })[0] || null;

  return {
    authorized: true,
    user: mapUserSummary(user, partner),
    partner: partner ? mapUserSummary(partner) : null
  };
}

function bootstrapDevice(payload) {
  payload = payload || {};
  var deviceId = normalizeString(payload.deviceId);
  if (!deviceId) {
    throw new Error('deviceId is required.');
  }

  var user = getUserByDeviceId(deviceId);
  if (!user) {
    var activeUsers = findManyBy(SHEET_NAMES.USERS, function(row) {
      return toBoolean(row.is_active);
    });
    var registeredDevices = activeUsers.filter(function(row) {
      return normalizeString(row.device_id) !== '';
    });
    var openSlot = activeUsers.find(function(row) {
      return normalizeString(row.device_id) === '';
    }) || null;

    if (openSlot) {
      openSlot.device_id = deviceId;
      openSlot.updated_at = nowIso();
      if (!normalizeString(openSlot.display_name)) {
        openSlot.display_name = 'Home Member ' + String(Math.min(activeUsers.indexOf(openSlot) + 1, 2));
      }
      updateRowByIndex(SHEET_NAMES.USERS, openSlot.__rowIndex, openSlot);
      user = getUserById(openSlot.user_id);
    } else if (registeredDevices.length < 2 && activeUsers.length < 2) {
      var slotNumber = activeUsers.length + 1;
      var newUserId = generateId('u', SHEET_NAMES.USERS);
      appendRow(SHEET_NAMES.USERS, {
        user_id: newUserId,
        household_id: '',
        device_id: deviceId,
        email: '',
        display_name: 'Home Member ' + slotNumber,
        photo_url: '',
        role: 'member',
        xp_total: 0,
        level: 1,
        coins_total: 0,
        current_streak: 0,
        longest_streak: 0,
        last_qualifying_date: '',
        created_at: nowIso(),
        updated_at: nowIso(),
        is_active: 'true',
        personal_identity: 'User ' + slotNumber
      });
      user = getUserById(newUserId);
    } else {
      return {
        authorized: false,
        reason: 'This household already has two registered devices.'
      };
    }
  }

  var partner = findManyBy(SHEET_NAMES.USERS, function(row) {
    return toBoolean(row.is_active) && row.user_id !== user.user_id;
  })[0] || null;

  return {
    authorized: true,
    user: mapUserSummary(user, partner),
    partner: partner ? mapUserSummary(partner) : null
  };
}

function getActiveUserByEmail(email) {
  return findOneBy(SHEET_NAMES.USERS, function(row) {
    return normalizeString(row.email).toLowerCase() === email && toBoolean(row.is_active);
  });
}

function getUserById(userId) {
  return findOneBy(SHEET_NAMES.USERS, function(row) {
    return row.user_id === userId && toBoolean(row.is_active);
  });
}

function getUserByDeviceId(deviceId) {
  return findOneBy(SHEET_NAMES.USERS, function(row) {
    return normalizeString(row.device_id) === deviceId && toBoolean(row.is_active);
  });
}

function mapUserSummary(row, partner) {
  return {
    userId: row.user_id,
    householdId: row.household_id || '',
    email: row.email,
    displayName: row.display_name,
    photoUrl: row.photo_url || '',
    personalIdentity: row.personal_identity || '',
    role: row.role,
    xpTotal: toInt(row.xp_total),
    level: toInt(row.level, 1),
    coinsTotal: toInt(row.coins_total),
    currentStreak: toInt(row.current_streak),
    longestStreak: toInt(row.longest_streak),
    lastQualifyingDate: row.last_qualifying_date || '',
    partner: partner ? mapUserSummary(partner) : null,
    isActive: toBoolean(row.is_active)
  };
}

function appendXpLedgerEntry(entry) {
  appendRow(SHEET_NAMES.XP_LEDGER, {
    ledger_id: generateId('x', SHEET_NAMES.XP_LEDGER),
    user_id: entry.userId,
    source_type: entry.sourceType,
    source_id: entry.sourceId,
    action_type: entry.actionType,
    xp_delta: entry.xpDelta,
    coin_delta: entry.coinDelta,
    reason: entry.reason,
    created_at: nowIso()
  });
}

function updateUserTotals(userId, xpDelta, coinDelta) {
  var user = getUserById(userId);
  if (!user) {
    throw new Error('User not found: ' + userId);
  }

  var newXpTotal = toInt(user.xp_total) + toInt(xpDelta);
  var newCoinsTotal = toInt(user.coins_total) + toInt(coinDelta);
  if (newXpTotal < 0 || newCoinsTotal < 0) {
    throw new Error('Balance cannot become negative.');
  }

  var level = calculateLevel(newXpTotal);

  user.xp_total = newXpTotal;
  user.coins_total = newCoinsTotal;
  user.level = level;
  user.updated_at = nowIso();

  updateRowByIndex(SHEET_NAMES.USERS, user.__rowIndex, user);

  return {
    xpTotal: newXpTotal,
    coinsTotal: newCoinsTotal,
    level: level
  };
}

function calculateLevel(xpTotal) {
  var raw = getConfigMap().level_table_json;
  if (!raw) {
    return 1;
  }
  var table = JSON.parse(raw);
  var level = 1;
  Object.keys(table).forEach(function(key) {
    if (xpTotal >= Number(table[key])) {
      level = Number(key);
    }
  });
  return level;
}

function applyQualifyingAction(userId, activityDate, actionType, sourceId) {
  var user = getUserById(userId);
  if (!user) {
    throw new Error('User not found: ' + userId);
  }

  var currentDate = activityDate || todayDateString();
  var lastDate = normalizeString(user.last_qualifying_date);
  var currentStreak = toInt(user.current_streak);
  var longestStreak = toInt(user.longest_streak);
  var newStreak = currentStreak;
  var changed = false;

  if (!lastDate) {
    newStreak = 1;
    changed = true;
  } else if (lastDate === currentDate) {
    newStreak = currentStreak || 1;
  } else {
    var diff = dateDiffInDays(lastDate, currentDate);
    if (diff === 1) {
      newStreak = Math.max(1, currentStreak + 1);
      changed = true;
    } else if (diff > 1) {
      newStreak = 1;
      changed = true;
    }
  }

  if (changed || lastDate !== currentDate) {
    user.current_streak = newStreak;
    user.longest_streak = Math.max(longestStreak, newStreak);
    user.last_qualifying_date = currentDate;
    user.updated_at = nowIso();
    updateRowByIndex(SHEET_NAMES.USERS, user.__rowIndex, user);

    appendRow(SHEET_NAMES.STREAK_HISTORY, {
      entry_id: generateId('s', SHEET_NAMES.STREAK_HISTORY),
      user_id: userId,
      activity_date: currentDate,
      qualifying_action_type: actionType,
      source_id: sourceId,
      streak_count_after: newStreak,
      created_at: nowIso()
    });
  }

  var bonusXp = 0;
  if (newStreak > 0 && newStreak % 7 === 0 && changed) {
    bonusXp = getConfigNumber('weekly_streak_bonus_xp', 50);
    appendXpLedgerEntry({
      userId: userId,
      sourceType: 'streak',
      sourceId: sourceId,
      actionType: 'streak_bonus',
      xpDelta: bonusXp,
      coinDelta: 0,
      reason: 'Weekly streak bonus'
    });
    updateUserTotals(userId, bonusXp, 0);
  }

  return {
    currentStreak: newStreak,
    longestStreak: Math.max(longestStreak, newStreak),
    streakBonusXp: bonusXp
  };
}

function dateDiffInDays(fromDateString, toDateString) {
  var from = new Date(fromDateString + 'T00:00:00Z');
  var to = new Date(toDateString + 'T00:00:00Z');
  var msPerDay = 24 * 60 * 60 * 1000;
  return Math.round((to.getTime() - from.getTime()) / msPerDay);
}

// ============================================================
// Multi-User Account Infrastructure
// ============================================================

function getHouseholdId() {
  var authUser = getActiveAuthUser();
  if (!authUser) {
    throw new Error('Not authenticated.');
  }
  return authUser.householdId || '';
}

function getActiveAuthUser() {
  var authToken = PropertiesService.getScriptProperties().getProperty('AUTH_TOKEN');
  if (!authToken) {
    return null;
  }
  var user = findOneBy(SHEET_NAMES.USERS, function(row) {
    return row.user_id === authToken;
  });
  return user || null;
}

function getUsersByHousehold(householdId) {
  return findManyBy(SHEET_NAMES.USERS, function(row) {
    return normalizeString(row.household_id) === householdId && toBoolean(row.is_active);
  });
}

function getHouseholdUsers(householdId) {
  var users = getUsersByHousehold(householdId);
  return users.filter(function(row) {
    return toBoolean(row.is_active);
  });
}

function findOrCreateUserByEmail(email, householdId) {
  var existing = findOneBy(SHEET_NAMES.USERS, function(row) {
    return normalizeString(row.email).toLowerCase() === email && toBoolean(row.is_active);
  });

  if (existing) {
    return existing;
  }

  var newUserId = generateId('u', SHEET_NAMES.USERS);
  appendRow(SHEET_NAMES.USERS, {
    user_id: newUserId,
    household_id: householdId,
    device_id: '',
    email: email,
    display_name: email.split('@')[0],
    photo_url: '',
    role: 'member',
    xp_total: 0,
    level: 1,
    coins_total: 0,
    current_streak: 0,
    longest_streak: 0,
    last_qualifying_date: '',
    created_at: nowIso(),
    updated_at: nowIso(),
    is_active: 'true',
    personal_identity: 'User ' + newUserId
  });

  return getUserById(newUserId);
}

function validateHouseholdMembership(userId, householdId) {
  var user = getUserById(userId);
  if (!user) {
    return false;
  }
  return normalizeString(user.household_id) === householdId && toBoolean(user.is_active);
}

// ============================================================
// Chart Analytics & Category Insights
// ============================================================

function getAnalytics(payload) {
  var userId = normalizeString(payload.userId);
  if (!userId) {
    throw new Error('userId is required.');
  }

  var user = getUserById(userId);
  if (!user) {
    throw new Error('User not found.');
  }

  var householdId = user.household_id || '';
  var users = getHouseholdUsers(householdId);
  var memberIds = users.map(function(u) { return u.user_id; });

  var ledgerRows = getSheetData(SHEET_NAMES.LEDGER);
  var householdRows = ledgerRows.filter(function(row) {
    return memberIds.indexOf(normalizeString(row.user_id)) >= 0 && toInt(row.amount) > 0;
  });

  var categories = ['Housing', 'Groceries', 'Dining Out', 'Fitness/Health', 'Education', 'Holidays', 'Subscriptions'];
  var categoryData = [];

  categories.forEach(function(category) {
    var catRows = householdRows.filter(function(row) {
      return normalizeString(row.category) === category;
    });

    var total = catRows.reduce(function(sum, row) { return sum + toInt(row.amount); }, 0);

    categoryData.push({
      category: category,
      expenditure_pct: total,
      total_spent: total,
      category_type: category,
      color: getCategoryColor(category)
    });
  });

  return {
    userId: userId,
    householdId: householdId,
    categories: categoryData,
    overview: {
      combinedTotal: categoryData.reduce(function(sum, cat) { return sum + toInt(cat.total_spent); }, 0),
      individualBreakdowns: users.map(function(u) {
        var spent = householdRows.filter(function(row) {
          return row.user_id === u.user_id;
        }).reduce(function(sum, row) { return sum + toInt(row.amount); }, 0);
        return {
          userId: u.user_id,
          displayName: u.display_name,
          totalSpent: spent
        };
      })
    }
  };
}

function getCategoryInsights(payload) {
  var userId = normalizeString(payload.userId);
  var category = normalizeString(payload.category);

  var user = getUserById(userId);
  if (!user) {
    throw new Error('User not found.');
  }

  var users = getHouseholdUsers(user.household_id || '');
  var memberIds = users.map(function(u) { return u.user_id; });

  var ledgerRows = getSheetData(SHEET_NAMES.LEDGER);
  var categoryTransactions = ledgerRows.filter(function(row) {
    return normalizeString(row.category) === category && memberIds.indexOf(normalizeString(row.user_id)) >= 0;
  });

  var totalSpent = categoryTransactions.reduce(function(sum, row) {
    return sum + toInt(row.amount);
  }, 0);

  var categoryData = {
    category: category,
    totalSpent: totalSpent,
    transactionCount: categoryTransactions.length,
    color: getCategoryColor(category)
  };

  return categoryData;
}

// ============================================================
// Shared Ledger with Chronological Timeline
// ============================================================

function listLedger(payload) {
  var userId = normalizeString(payload.userId);
  var sortBy = normalizeString(payload.sortBy) || 'date';

  var ledgerRows = getSheetData(SHEET_NAMES.LEDGER);

  var filtered = [];
  if (userId) {
    filtered = ledgerRows.filter(function(row) {
      return row.user_id === userId;
    });
  }

  filtered.sort(function(a, b) {
    if (sortBy === 'date') {
      return a.timestamp.localeCompare(b.timestamp) || b.__rowIndex - a.__rowIndex;
    } else if (sortBy === 'category') {
      return a.category.localeCompare(b.category) || b.__rowIndex - a.__rowIndex;
    } else if (sortBy === 'amount') {
      return toInt(a.amount) - toInt(b.amount);
    }
    return b.__rowIndex - a.__rowIndex;
  });

  return {
    ledger: filtered,
    totalEntries: filtered.length
  };
}

function appendLedgerEntry(entry) {
  appendRow(SHEET_NAMES.LEDGER, {
    ledger_id: generateId('l', SHEET_NAMES.LEDGER),
    user_id: entry.userId,
    source_type: entry.sourceType,
    source_id: entry.sourceId,
    action_type: entry.actionType,
    amount: entry.amount,
    direction: entry.direction,
    description: entry.description,
    timestamp: nowIso(),
    category: entry.category
  });
}

// ============================================================
// Finance Service (summary, transactions, bills, shopping)
// ============================================================

var FINANCE_TYPES = ['income', 'expense', 'savings_deposit', 'savings_withdraw'];
var FINANCE_WALLETS = ['joint', 'his', 'hers'];
var SUPPORTED_CURRENCIES = ['USD', 'EUR', 'GBP', 'KES', 'NGN', 'CAD', 'AUD', 'JPY'];

function getFinanceSummary(payload) {
  var ledgerRows = getSheetData(SHEET_NAMES.LEDGER);

  var wallets = { joint: 0, his: 0, hers: 0 };
  var savings = 0;
  ledgerRows.forEach(function(row) {
    var source = normalizeString(row.source_type) || normalizeString(row.source_id);
    var amount = toInt(row.amount);
    var sign = normalizeString(row.direction) === 'out' ? -1 : 1;
    if (wallets[source] !== undefined) {
      wallets[source] += sign * amount;
    }
    var actionType = normalizeString(row.action_type);
    if (actionType === 'savings_deposit') {
      savings += amount;
    } else if (actionType === 'savings_withdraw') {
      savings -= amount;
    }
  });

  var transactions = ledgerRows
    .filter(function(row) { return FINANCE_TYPES.indexOf(normalizeString(row.action_type)) >= 0; })
    .map(mapMoneyTransaction);
  transactions.sort(function(a, b) {
    return String(b.createdAt).localeCompare(String(a.createdAt));
  });

  var bills = getSheetData(SHEET_NAMES.BILLS).map(mapBill);
  var shoppingItems = getSheetData(SHEET_NAMES.SHOPPING_ITEMS).map(mapShoppingItem);

  var today = todayDateString();
  var openBills = bills.filter(function(bill) {
    return bill.status === 'pending' && bill.dueDate >= today;
  }).sort(compareBillsByDueDate);
  var overdueBills = bills.filter(function(bill) {
    return bill.status === 'pending' && bill.dueDate && bill.dueDate < today;
  }).sort(compareBillsByDueDate);
  var openShoppingItems = shoppingItems.filter(function(item) {
    return item.status === 'open';
  }).sort(function(a, b) {
    return String(b.createdAt).localeCompare(String(a.createdAt));
  });

  return {
    balance: Math.round((wallets.joint + wallets.his + wallets.hers) * 100) / 100,
    savings: Math.round(savings * 100) / 100,
    wallets: {
      joint: Math.round(wallets.joint * 100) / 100,
      his: Math.round(wallets.his * 100) / 100,
      hers: Math.round(wallets.hers * 100) / 100
    },
    goals: {
      vacationGoal: getConfigNumber('vacation_goal', 2000),
      dreamGoal: getConfigNumber('dream_goal', 10000)
    },
    currency: getConfigString('currency', 'USD'),
    recentTransactions: transactions.slice(0, 15),
    openBills: openBills,
    overdueBills: overdueBills,
    openShoppingItems: openShoppingItems
  };
}

function addTransaction(payload) {
  var type = normalizeString(payload.type);
  var description = normalizeString(payload.description);
  var category = normalizeString(payload.category);
  var amount = Number(payload.amount);
  var userId = normalizeString(payload.userId);
  var wallet = normalizeWallet(payload.wallet);

  if (FINANCE_TYPES.indexOf(type) < 0) {
    throw new Error('type must be one of: income, expense, savings_deposit, savings_withdraw.');
  }
  if (!description) {
    throw new Error('description is required.');
  }
  if (!(amount > 0)) {
    throw new Error('amount must be a positive number.');
  }

  var direction = (type === 'income' || type === 'savings_withdraw') ? 'in' : 'out';

  appendLedgerEntry({
    userId: userId,
    sourceType: wallet,
    sourceId: wallet,
    actionType: type,
    amount: amount,
    direction: direction,
    description: description,
    category: category
  });

  if (type === 'expense' && category) {
    try {
      var currentMonth = todayDateString().slice(0, 7);
      updateBudgetProgress(category, currentMonth, amount);
    } catch (e) {
      // Budget tracking is best-effort.
    }
  }

  return {
    transaction: mapMoneyTransaction({
      ledger_id: '',
      user_id: userId,
      source_type: wallet,
      action_type: type,
      amount: amount,
      direction: direction,
      description: description,
      timestamp: nowIso(),
      category: category
    }),
    summary: getFinanceSummary({})
  };
}

function setFinanceGoals(payload) {
  var vacationGoal = Number(payload.vacationGoal);
  var dreamGoal = Number(payload.dreamGoal);

  if (isNaN(vacationGoal) || vacationGoal < 0) {
    throw new Error('vacationGoal must be a non-negative number.');
  }
  if (isNaN(dreamGoal) || dreamGoal < 0) {
    throw new Error('dreamGoal must be a non-negative number.');
  }

  upsertConfig('vacation_goal', vacationGoal);
  upsertConfig('dream_goal', dreamGoal);

  return {
    vacationGoal: getConfigNumber('vacation_goal', 2000),
    dreamGoal: getConfigNumber('dream_goal', 10000)
  };
}

function setFinanceCurrency(payload) {
  var currency = normalizeCurrency(payload.currency);
  upsertConfig('currency', currency);
  return {
    currency: getConfigString('currency', 'USD')
  };
}

function listBills(payload) {
  return getSheetData(SHEET_NAMES.BILLS).map(mapBill).sort(compareBillsByDueDate);
}

function createBill(payload) {
  var title = normalizeString(payload.title);
  var category = normalizeString(payload.category);
  var amount = Number(payload.amount);
  var dueDate = normalizeString(payload.dueDate);
  var createdByUserId = normalizeString(payload.createdByUserId);

  if (!title) {
    throw new Error('title is required.');
  }
  if (!(amount > 0)) {
    throw new Error('amount must be a positive number.');
  }
  if (!dueDate) {
    throw new Error('dueDate is required.');
  }

  var bill = {
    bill_id: generateId('b', SHEET_NAMES.BILLS),
    title: title,
    category: category,
    amount: amount,
    due_date: dueDate,
    status: 'pending',
    paid_by_user_id: '',
    paid_at: '',
    created_by_user_id: createdByUserId,
    created_at: nowIso(),
    updated_at: nowIso()
  };
  appendRow(SHEET_NAMES.BILLS, bill);
  return mapBill(bill);
}

function updateBill(payload) {
  var billId = normalizeString(payload.billId);
  var status = normalizeString(payload.status);
  var paidByUserId = normalizeString(payload.paidByUserId);

  if (!billId) {
    throw new Error('billId is required.');
  }

  var bill = findOneBy(SHEET_NAMES.BILLS, function(row) {
    return row.bill_id === billId;
  });
  if (!bill) {
    throw new Error('Bill not found.');
  }

  if (status === 'paid') {
    bill.status = 'paid';
    bill.paid_by_user_id = paidByUserId;
    bill.paid_at = nowIso();
  } else if (status === 'pending') {
    bill.status = 'pending';
    bill.paid_by_user_id = '';
    bill.paid_at = '';
  }
  bill.updated_at = nowIso();
  updateRowByIndex(SHEET_NAMES.BILLS, bill.__rowIndex, bill);
  return mapBill(bill);
}

function listShoppingItems(payload) {
  return getSheetData(SHEET_NAMES.SHOPPING_ITEMS).map(mapShoppingItem).sort(function(a, b) {
    if (a.status === b.status) {
      return String(b.createdAt).localeCompare(String(a.createdAt));
    }
    return a.status === 'open' ? -1 : 1;
  });
}

function createShoppingItem(payload) {
  var title = normalizeString(payload.title);
  var category = normalizeString(payload.category);
  var estimatedCost = Number(payload.estimatedCost);
  var addedByUserId = normalizeString(payload.addedByUserId);

  if (!title) {
    throw new Error('title is required.');
  }

  var item = {
    item_id: generateId('i', SHEET_NAMES.SHOPPING_ITEMS),
    title: title,
    category: category,
    estimated_cost: estimatedCost > 0 ? estimatedCost : 0,
    status: 'open',
    purchased_by_user_id: '',
    purchased_at: '',
    added_by_user_id: addedByUserId,
    created_at: nowIso(),
    updated_at: nowIso()
  };
  appendRow(SHEET_NAMES.SHOPPING_ITEMS, item);
  return mapShoppingItem(item);
}

function updateShoppingItem(payload) {
  var itemId = normalizeString(payload.itemId);
  var status = normalizeString(payload.status);
  var purchasedByUserId = normalizeString(payload.purchasedByUserId);

  if (!itemId) {
    throw new Error('itemId is required.');
  }

  var item = findOneBy(SHEET_NAMES.SHOPPING_ITEMS, function(row) {
    return row.item_id === itemId;
  });
  if (!item) {
    throw new Error('Shopping item not found.');
  }

  if (status === 'purchased') {
    item.status = 'purchased';
    item.purchased_by_user_id = purchasedByUserId;
    item.purchased_at = nowIso();
  } else if (status === 'open') {
    item.status = 'open';
    item.purchased_by_user_id = '';
    item.purchased_at = '';
  }
  item.updated_at = nowIso();
  updateRowByIndex(SHEET_NAMES.SHOPPING_ITEMS, item.__rowIndex, item);
  return mapShoppingItem(item);
}

function compareBillsByDueDate(a, b) {
  return String(a.dueDate).localeCompare(String(b.dueDate));
}

function normalizeWallet(value) {
  var wallet = normalizeString(value).toLowerCase();
  return FINANCE_WALLETS.indexOf(wallet) < 0 ? 'joint' : wallet;
}

function normalizeCurrency(value) {
  var currency = normalizeString(value).toUpperCase();
  if (SUPPORTED_CURRENCIES.indexOf(currency) < 0) {
    throw new Error('currency must be one of: ' + SUPPORTED_CURRENCIES.join(', ') + '.');
  }
  return currency;
}

function mapMoneyTransaction(row) {
  return {
    transactionId: row.ledger_id,
    type: row.action_type || '',
    description: row.description || '',
    category: row.category || '',
    amount: Number(row.amount) || 0,
    wallet: normalizeWallet(row.source_type),
    userId: row.user_id || '',
    createdAt: row.timestamp || ''
  };
}

function mapBill(row) {
  return {
    billId: row.bill_id,
    title: row.title || '',
    category: row.category || '',
    amount: Number(row.amount) || 0,
    dueDate: row.due_date || '',
    status: row.status || 'pending',
    paidByUserId: row.paid_by_user_id || '',
    paidAt: row.paid_at || ''
  };
}

function mapShoppingItem(row) {
  return {
    itemId: row.item_id,
    title: row.title || '',
    category: row.category || '',
    estimatedCost: Number(row.estimated_cost) || 0,
    status: row.status || 'open',
    addedByUserId: row.added_by_user_id || '',
    purchasedAt: row.purchased_at || ''
  };
}

// ============================================================
// Budgets & Envelope Budgeting Tracking
// ============================================================

function listBudgets(payload) {
  var userId = normalizeString(payload.userId);
  var category = normalizeString(payload.category);
  var month = normalizeString(payload.month);

  var budgetRows = getSheetData(SHEET_NAMES.BUDGETS);

  var filtered = [];
  if (userId) {
    filtered = budgetRows.filter(function(row) {
      return row.user_id === userId;
    });
  }

  if (category) {
    filtered = filtered.filter(function(row) {
      return normalizeString(row.category) === category;
    });
  }

  if (month) {
    filtered = filtered.filter(function(row) {
      return normalizeString(row.month) === month;
    });
  }

  return {
    budgets: filtered.map(mapBudget),
    totalBudgets: filtered.length
  };
}

function mapBudget(row) {
  return {
    budgetId: row.budget_id,
    userId: row.user_id || '',
    category: row.category,
    month: row.month,
    currentSpent: toInt(row.current_spent),
    budgetLimit: toInt(row.budget_limit),
    progressPct: toInt(row.progress_pct),
    status: row.status
  };
}

function createBudget(payload) {
  var userId = normalizeString(payload.userId);
  var category = normalizeString(payload.category);
  var month = normalizeString(payload.month);
  var budgetLimit = toInt(payload.budgetLimit);

  if (!category || !month || !budgetLimit) {
    throw new Error('category, month, and budgetLimit are required.');
  }

  var budgetId = generateId('b', SHEET_NAMES.BUDGETS);
  appendRow(SHEET_NAMES.BUDGETS, {
    budget_id: budgetId,
    user_id: userId,
    category: category,
    month: month,
    current_spent: 0,
    budget_limit: budgetLimit,
    progress_pct: 0,
    status: 'active'
  });

  return {
    budget: {
      budgetId: budgetId,
      userId: userId,
      category: category,
      month: month,
      currentSpent: 0,
      budgetLimit: budgetLimit,
      progressPct: 0,
      status: 'active'
    }
  };
}

function updateBudgetProgress(category, month, amount) {
  var budgetRows = getSheetData(SHEET_NAMES.BUDGETS);
  var budget = budgetRows.find(function(row) {
    return normalizeString(row.category) === category && normalizeString(row.month) === month;
  });

  if (!budget) {
    throw new Error('Budget not found for category: ' + category + ' month: ' + month);
  }

  var currentSpent = toInt(budget.current_spent) + toInt(amount);
  var budgetLimit = toInt(budget.budget_limit);
  var progress = budgetLimit > 0 ? Math.min(100, Math.round((currentSpent / budgetLimit) * 100)) : 0;

  budget.current_spent = currentSpent;
  budget.progress_pct = progress;
  budget.status = progress >= 100 ? 'completed' : 'active';

  updateRowByIndex(SHEET_NAMES.BUDGETS, budget.__rowIndex, budget);

  return budget;
}

// ============================================================
// Recurring Subscriptions Control Matrix
// ============================================================

function listSubscriptions(payload) {
  var userId = normalizeString(payload.userId);

  var subscriptionRows = getSheetData(SHEET_NAMES.SUBSCRIPTIONS);

  var filtered = subscriptionRows.filter(function(row) {
    return toBoolean(row.is_active);
  });

  if (userId) {
    filtered = filtered.filter(function(row) {
      return row.user_id === userId;
    });
  }

  return {
    subscriptions: filtered.map(mapSubscription),
    totalSubscriptions: filtered.length
  };
}

function createSubscription(payload) {
  var userId = normalizeString(payload.userId);
  var serviceName = normalizeString(payload.serviceName);
  var nextRenewalDate = normalizeString(payload.nextRenewalDate);
  var billingInterval = normalizeString(payload.billingInterval);
  var terminationRule = normalizeString(payload.terminationRule);

  if (!serviceName || !nextRenewalDate) {
    throw new Error('serviceName and nextRenewalDate are required.');
  }

  var subscriptionId = generateId('s', SHEET_NAMES.SUBSCRIPTIONS);
  appendRow(SHEET_NAMES.SUBSCRIPTIONS, {
    subscription_id: subscriptionId,
    user_id: userId,
    service_name: serviceName,
    next_renewal_date: nextRenewalDate,
    billing_interval: billingInterval,
    termination_rule: terminationRule,
    is_active: 'true',
    created_at: nowIso(),
    updated_at: nowIso()
  });

  return {
    subscription: mapSubscription({
      subscription_id: subscriptionId,
      user_id: userId,
      service_name: serviceName,
      next_renewal_date: nextRenewalDate,
      billing_interval: billingInterval,
      termination_rule: terminationRule,
      is_active: 'true',
      created_at: nowIso(),
      updated_at: nowIso()
    })
  };
}

function mapSubscription(row) {
  return {
    subscriptionId: row.subscription_id,
    userId: row.user_id || '',
    serviceName: row.service_name,
    nextRenewalDate: row.next_renewal_date,
    billingInterval: row.billing_interval || '',
    terminationRule: row.termination_rule || '',
    isActive: toBoolean(row.is_active),
    createdAt: row.created_at || '',
    updatedAt: row.updated_at || ''
  };
}

function updateSubscription(payload) {
  var subscriptionId = normalizeString(payload.subscriptionId);
  var subscriptionRows = getSheetData(SHEET_NAMES.SUBSCRIPTIONS);
  var subscription = subscriptionRows.find(function(row) {
    return row.subscription_id === subscriptionId;
  });

  if (!subscription) {
    throw new Error('Subscription not found.');
  }

  if (payload.nextRenewalDate !== undefined) subscription.next_renewal_date = normalizeString(payload.nextRenewalDate);
  if (payload.isActive !== undefined) subscription.is_active = String(Boolean(payload.isActive));
  subscription.updated_at = nowIso();

  updateRowByIndex(SHEET_NAMES.SUBSCRIPTIONS, subscription.__rowIndex, subscription);

  return { subscription: mapSubscription(subscription) };
}

// ============================================================
// Explicit Fund Transfers
// ============================================================

function listTransfers(payload) {
  var userId = normalizeString(payload.userId);
  var sourceAccount = normalizeString(payload.sourceAccount);
  var destinationTarget = normalizeString(payload.destinationTarget);

  var ledgerRows = getSheetData(SHEET_NAMES.LEDGER);

  var rows = [];
  if (userId) {
    rows = ledgerRows.filter(function(row) {
      return row.user_id === userId && normalizeString(row.action_type) === 'transfer';
    });
  } else {
    rows = ledgerRows.filter(function(row) {
      return normalizeString(row.action_type) === 'transfer';
    });
  }

  if (sourceAccount) {
    rows = rows.filter(function(row) {
      return normalizeString(row.source_type) === sourceAccount || normalizeString(row.source_id) === sourceAccount;
    });
  }

  if (destinationTarget) {
    rows = rows.filter(function(row) {
      return normalizeString(row.source_type) === destinationTarget || normalizeString(row.source_id) === destinationTarget;
    });
  }

  var grouped = {};
  rows.forEach(function(row) {
    var key = String(row.user_id) + '|' + String(row.amount) + '|' + String(row.description);
    if (!grouped[key]) {
      grouped[key] = [];
    }
    grouped[key].push(row);
  });

  var transfers = [];
  Object.keys(grouped).forEach(function(key) {
    var pair = grouped[key].sort(function(a, b) { return a.__rowIndex - b.__rowIndex; });
    var outRow = null;
    var inRow = null;
    for (var i = 0; i < pair.length; i += 1) {
      if (normalizeString(pair[i].direction) === 'out' && !outRow) {
        outRow = pair[i];
      } else if (normalizeString(pair[i].direction) === 'in' && !inRow) {
        inRow = pair[i];
      }
    }
    if (!outRow) outRow = pair[0];
    if (!inRow) inRow = outRow;
    transfers.push({
      transferId: outRow.ledger_id,
      sourceAccount: outRow.source_id,
      destinationTarget: inRow.source_id,
      amount: toInt(outRow.amount),
      direction: outRow.direction,
      timestamp: outRow.timestamp
    });
  });

  transfers.sort(function(a, b) {
    return String(b.timestamp).localeCompare(String(a.timestamp));
  });

  return {
    transfers: transfers,
    totalTransfers: transfers.length
  };
}

function executeTransfer(payload) {
  var sourceAccount = normalizeString(payload.sourceAccount);
  var destinationTarget = normalizeString(payload.destinationTarget);
  var amount = toInt(payload.amount);
  var direction = normalizeString(payload.direction) || 'out';

  if (!sourceAccount || !destinationTarget || !amount) {
    throw new Error('sourceAccount, destinationTarget, and amount are required.');
  }

  if (amount <= 0) {
    throw new Error('Amount must be a positive number.');
  }

  var user = getUserById(payload.userId);
  if (!user) {
    throw new Error('User not found.');
  }

  var sourceAccountData = getAccountBalance(sourceAccount, user.userId);
  var destinationAccountData = getAccountBalance(destinationTarget, user.userId);

  if (!sourceAccountData || sourceAccountData.balance < amount) {
    throw new Error('Insufficient liquidity in source account: ' + sourceAccount + '. Available balance: ' + sourceAccountData.balance);
  }

  var sourceDelta = (direction === 'out') ? -amount : amount;
  var destDelta = (direction === 'out') ? amount : -amount;

  var newSourceBalance = sourceAccountData.balance + sourceDelta;
  var newDestBalance = destinationAccountData.balance + destDelta;

  appendLedgerEntry({
    userId: user.userId,
    sourceType: 'transfer',
    sourceId: sourceAccount,
    actionType: 'transfer',
    amount: amount,
    direction: direction,
    description: 'Transfer from ' + sourceAccount + ' to ' + destinationTarget,
    category: 'Transfer'
  });

  appendLedgerEntry({
    userId: user.userId,
    sourceType: 'transfer',
    sourceId: destinationTarget,
    actionType: 'transfer',
    amount: amount,
    direction: (direction === 'out') ? 'in' : 'out',
    description: 'Transfer from ' + sourceAccount + ' to ' + destinationTarget,
    category: 'Transfer'
  });

  return {
    transfer: {
      sourceAccount: sourceAccount,
      destinationTarget: destinationTarget,
      amount: amount,
      direction: direction,
      sourceBalance: newSourceBalance,
      destinationBalance: newDestBalance,
      timestamp: nowIso()
    }
  };
}

function getAccountBalance(accountId, userId) {
  var ledgerRows = getSheetData(SHEET_NAMES.LEDGER);
  var accountRows = ledgerRows.filter(function(row) {
    return normalizeString(row.user_id) === userId && (normalizeString(row.source_type) === accountId || normalizeString(row.source_id) === accountId);
  });

  var balance = 0;
  accountRows.forEach(function(row) {
    balance += (normalizeString(row.direction) === 'out') ? -toInt(row.amount) : toInt(row.amount);
  });

  return {
    balance: balance,
    accountId: accountId,
    userId: userId
  };
}

// ============================================================
// Helper: Get household summary for dashboard
// ============================================================

function getDashboard(payload) {
  var userId = normalizeString(payload.userId);
  if (!userId) {
    throw new Error('userId is required.');
  }

  var user = getUserById(userId);
  if (!user) {
    throw new Error('User not found.');
  }

  var householdId = user.household_id || '';
  var users = getHouseholdUsers(householdId);
  var partner = users.filter(function(u) { return u.user_id !== user.user_id; })[0] || null;

  var today = todayDateString();
  var openTasks = findManyBy(SHEET_NAMES.TASKS, function(row) {
    return row.status !== 'completed' && row.status !== 'archived';
  }).map(mapTask);

  var tasksDueToday = openTasks.filter(function(task) {
    return task.dueDate === today;
  }).sort(compareTasksForDashboard);

  var overdueTasks = openTasks.filter(function(task) {
    return task.dueDate && task.dueDate < today;
  }).sort(compareTasksForDashboard);

  return {
    currentUser: mapUserSummary(user),
    partner: partner ? mapUserSummary(partner) : null,
    tasksDueToday: tasksDueToday,
    overdueTasks: overdueTasks
  };
}

function compareTasksForDashboard(a, b) {
  if (a.priority === b.priority) {
    return String(a.title).localeCompare(String(b.title));
  }

  var weight = {
    high: 0,
    medium: 1,
    low: 2
  };

  return (weight[a.priority] || 9) - (weight[b.priority] || 9);
}

function listTasks(payload) {
  var status = normalizeString(payload.status);
  var assignedToUserId = normalizeString(payload.assignedToUserId);

  return findManyBy(SHEET_NAMES.TASKS, function(row) {
    var statusMatch = !status || row.status === status;
    var assigneeMatch = !assignedToUserId || row.assigned_to_user_id === assignedToUserId;
    return statusMatch && assigneeMatch;
  }).map(mapTask);
}

function createTask(payload) {
  validateTaskPayload(payload, true);

  var priority = normalizeString(payload.priority) || 'medium';
  var reward = resolveTaskReward(priority, payload.xpReward, payload.coinReward);
  var task = {
    task_id: generateId('t', SHEET_NAMES.TASKS),
    title: normalizeString(payload.title),
    description: normalizeString(payload.description),
    category: normalizeString(payload.category) || 'General',
    assigned_to_user_id: normalizeString(payload.assignedToUserId),
    created_by_user_id: normalizeString(payload.createdByUserId),
    status: 'open',
    priority: priority,
    due_date: normalizeString(payload.dueDate),
    repeat_rule: normalizeString(payload.repeatRule),
    xp_reward: reward.xpReward,
    coin_reward: reward.coinReward,
    streak_eligible: String(Boolean(payload.streakEligible)),
    completed_at: '',
    completed_by_user_id: '',
    created_at: nowIso(),
    updated_at: nowIso(),
    version: 1
  };

  appendRow(SHEET_NAMES.TASKS, task);
  return mapTask(task);
}

function updateTask(payload) {
  var taskId = normalizeString(payload.taskId);
  var incomingVersion = toInt(payload.version);

  if (!taskId) {
    throw new Error('taskId is required.');
  }

  var task = findOneBy(SHEET_NAMES.TASKS, function(row) {
    return row.task_id === taskId;
  });

  if (!task) {
    throw new Error('Task not found.');
  }

  if (toInt(task.version) !== incomingVersion) {
    throw new Error('STALE_TASK_VERSION');
  }

  if (normalizeString(payload.title)) task.title = normalizeString(payload.title);
  if (payload.description !== undefined) task.description = normalizeString(payload.description);
  if (payload.category !== undefined) task.category = normalizeString(payload.category);
  if (payload.assignedToUserId !== undefined) task.assigned_to_user_id = normalizeString(payload.assignedToUserId);
  if (payload.priority !== undefined) task.priority = normalizeString(payload.priority);
  if (payload.dueDate !== undefined) task.due_date = normalizeString(payload.dueDate);
  if (payload.repeatRule !== undefined) task.repeat_rule = normalizeString(payload.repeatRule);
  if (payload.streakEligible !== undefined) task.streak_eligible = String(Boolean(payload.streakEligible));

  task.version = toInt(task.version) + 1;
  task.updated_at = nowIso();

  updateRowByIndex(SHEET_NAMES.TASKS, task.__rowIndex, task);
  return mapTask(task);
}

function completeTask(payload) {
  var taskId = normalizeString(payload.taskId);
  var completedByUserId = normalizeString(payload.completedByUserId);

  if (!taskId || !completedByUserId) {
    throw new Error('taskId and completedByUserId are required.');
  }

  var lock = LockService.getScriptLock();
  lock.waitLock(30000);

  try {
    var task = findOneBy(SHEET_NAMES.TASKS, function(row) {
      return row.task_id === taskId;
    });

    if (!task) {
      throw new Error('TASK_NOT_FOUND');
    }

    if (task.status === 'completed') {
      throw new Error('TASK_ALREADY_COMPLETED');
    }

    task.status = 'completed';
    task.completed_at = nowIso();
    task.completed_by_user_id = completedByUserId;
    task.updated_at = nowIso();
    task.version = toInt(task.version) + 1;
    updateRowByIndex(SHEET_NAMES.TASKS, task.__rowIndex, task);

    var xpReward = toInt(task.xp_reward, getConfigNumber('default_task_xp', 20));
    var coinReward = toInt(task.coin_reward, getConfigNumber('default_task_coins', 5));

    appendXpLedgerEntry({
      userId: completedByUserId,
      sourceType: 'task',
      sourceId: taskId,
      actionType: 'task_completion',
      xpDelta: xpReward,
      coinDelta: coinReward,
      reason: 'Task completed'
    });

    var totals = updateUserTotals(completedByUserId, xpReward, coinReward);
    var streak = toBoolean(task.streak_eligible)
      ? applyQualifyingAction(completedByUserId, todayDateString(), 'task', taskId)
      : {
          currentStreak: toInt(getUserById(completedByUserId).current_streak),
          longestStreak: toInt(getUserById(completedByUserId).longest_streak),
          streakBonusXp: 0
        };

    return {
      task: mapTask(task),
      rewards: {
        xpGained: xpReward + (streak.streakBonusXp || 0),
        coinsGained: coinReward
      },
      user: Object.assign({}, mapUserSummary(getUserById(completedByUserId)), totals, {
        currentStreak: streak.currentStreak,
        longestStreak: streak.longestStreak
      })
    };
  } finally {
    lock.releaseLock();
  }
}

function resolveTaskReward(priority, xpReward, coinReward) {
  if (xpReward !== undefined || coinReward !== undefined) {
    return {
      xpReward: toInt(xpReward, getConfigNumber('default_task_xp', 20)),
      coinReward: toInt(coinReward, getConfigNumber('default_task_coins', 5))
    };
  }

  if (priority === 'high') {
    return {
      xpReward: getConfigNumber('high_priority_task_xp', 35),
      coinReward: getConfigNumber('high_priority_task_coins', 8)
    };
  }

  return {
    xpReward: getConfigNumber('default_task_xp', 20),
    coinReward: getConfigNumber('default_task_coins', 5)
  };
}

function validateTaskPayload(payload, requireCreator) {
  if (!normalizeString(payload.title)) {
    throw new Error('title is required.');
  }

  if (requireCreator && !normalizeString(payload.createdByUserId)) {
    throw new Error('createdByUserId is required.');
  }
}

function mapTask(row) {
  return {
    taskId: row.task_id,
    title: row.title,
    description: row.description || '',
    category: row.category || '',
    assignedToUserId: row.assigned_to_user_id || '',
    createdByUserId: row.created_by_user_id || '',
    status: row.status,
    priority: row.priority,
    dueDate: row.due_date || '',
    repeatRule: row.repeat_rule || '',
    xpReward: toInt(row.xp_reward),
    coinReward: toInt(row.coin_reward),
    streakEligible: toBoolean(row.streak_eligible),
    completedAt: row.completed_at || '',
    completedByUserId: row.completed_by_user_id || '',
    createdAt: row.created_at || '',
    updatedAt: row.updated_at || '',
    version: toInt(row.version, 1)
  };
}

function logWorkout(payload) {
  var userId = normalizeString(payload.userId);
  var workoutType = normalizeString(payload.workoutType);
  var durationMinutes = toInt(payload.durationMinutes);
  var intensity = normalizeString(payload.intensity) || 'medium';
  var notes = normalizeString(payload.notes);

  if (!userId || !workoutType || durationMinutes <= 0) {
    throw new Error('userId, workoutType, and durationMinutes are required.');
  }

  var lock = LockService.getScriptLock();
  lock.waitLock(30000);

  try {
    var xpReward = Math.max(1, Math.floor(durationMinutes / 10)) * getConfigNumber('workout_xp_per_10_minutes', 10);
    var coinReward = getConfigNumber('workout_coins_flat', 3);
    var workoutId = generateId('w', SHEET_NAMES.WORKOUTS);

    appendRow(SHEET_NAMES.WORKOUTS, {
      workout_id: workoutId,
      user_id: userId,
      workout_type: workoutType,
      duration_minutes: durationMinutes,
      intensity: intensity,
      notes: notes,
      xp_reward: xpReward,
      coin_reward: coinReward,
      logged_at: nowIso(),
      created_at: nowIso()
    });

    appendXpLedgerEntry({
      userId: userId,
      sourceType: 'workout',
      sourceId: workoutId,
      actionType: 'workout_logged',
      xpDelta: xpReward,
      coinDelta: coinReward,
      reason: 'Workout logged'
    });

    var totals = updateUserTotals(userId, xpReward, coinReward);
    var streak = applyQualifyingAction(userId, todayDateString(), 'workout', workoutId);

    return {
      workoutId: workoutId,
      xpReward: xpReward + (streak.streakBonusXp || 0),
      coinReward: coinReward,
      user: Object.assign({}, mapUserSummary(getUserById(userId)), totals, {
        currentStreak: streak.currentStreak,
        longestStreak: streak.longestStreak
      })
    };
  } finally {
    lock.releaseLock();
  }
}

function getRewards(payload) {
  return findManyBy(SHEET_NAMES.REWARDS_STORE, function(row) {
    return toBoolean(row.is_active);
  }).map(mapRewardItem);
}

function redeemReward(payload) {
  var userId = normalizeString(payload.userId);
  var rewardId = normalizeString(payload.rewardId);
  var notes = normalizeString(payload.notes);

  if (!userId || !rewardId) {
    throw new Error('userId and rewardId are required.');
  }

  var user = getUserById(userId);
  if (!user) {
    throw new Error('User not found.');
  }

  var reward = getRewardById(rewardId);
  if (!reward || !toBoolean(reward.is_active)) {
    throw new Error('Reward not found or inactive.');
  }

  var costCoins = toInt(reward.cost_coins);
  var costXp = toInt(reward.cost_xp);

  if (toInt(user.coins_total) < costCoins) {
    throw new Error('Insufficient coins to redeem this reward.');
  }

  if (toInt(user.xp_total) < costXp) {
    throw new Error('Insufficient XP to redeem this reward.');
  }

  var redemptionId = generateId('r', SHEET_NAMES.REWARD_REDEMPTIONS);
  appendRow(SHEET_NAMES.REWARD_REDEMPTIONS, {
    redemption_id: redemptionId,
    reward_id: rewardId,
    user_id: userId,
    cost_coins: costCoins,
    cost_xp: costXp,
    status: 'redeemed',
    redeemed_at: nowIso(),
    resolved_at: '',
    notes: notes
  });

  appendXpLedgerEntry({
    userId: userId,
    sourceType: 'reward',
    sourceId: rewardId,
    actionType: 'reward_redeem',
    xpDelta: -costXp,
    coinDelta: -costCoins,
    reason: 'Reward redeemed: ' + reward.title
  });

  var totals = updateUserTotals(userId, -costXp, -costCoins);
  var updatedUser = getUserById(userId);

  return {
    reward: mapRewardItem(reward),
    redemptionId: redemptionId,
    user: Object.assign({}, mapUserSummary(updatedUser), totals)
  };
}

function getRewardById(rewardId) {
  return findOneBy(SHEET_NAMES.REWARDS_STORE, function(row) {
    return row.reward_id === rewardId;
  });
}

function mapRewardItem(row) {
  return {
    rewardId: row.reward_id,
    title: row.title,
    description: row.description || '',
    costCoins: toInt(row.cost_coins),
    costXp: toInt(row.cost_xp),
    category: row.category || '',
    isActive: toBoolean(row.is_active)
  };
}

function listHouseholdLogs(payload) {
  var userId = normalizeString(payload.userId);
  var logs = findManyBy(SHEET_NAMES.HOUSEHOLD_LOG, function() {
    return true;
  }).map(mapHouseholdLog);

  if (userId) {
    logs = logs.filter(function(log) {
      return log.userId === userId;
    });
  }

  return logs;
}

function createHouseholdLog(payload) {
  var userId = normalizeString(payload.userId);
  var type = normalizeString(payload.type);
  var title = normalizeString(payload.title);
  var details = normalizeString(payload.details);
  var relatedTaskId = normalizeString(payload.relatedTaskId);

  if (!userId || !type || !title) {
    throw new Error('userId, type, and title are required.');
  }

  var user = getUserById(userId);
  if (!user) {
    throw new Error('User not found.');
  }

  var reward = resolveHouseholdLogReward(payload.xpReward, payload.coinReward);
  var logId = generateId('h', SHEET_NAMES.HOUSEHOLD_LOG);

  appendRow(SHEET_NAMES.HOUSEHOLD_LOG, {
    log_id: logId,
    type: type,
    title: title,
    details: details,
    user_id: userId,
    related_task_id: relatedTaskId,
    xp_reward: reward.xpReward,
    coin_reward: reward.coinReward,
    logged_at: nowIso(),
    created_at: nowIso()
  });

  appendXpLedgerEntry({
    userId: userId,
    sourceType: 'household_log',
    sourceId: logId,
    actionType: 'household_action',
    xpDelta: reward.xpReward,
    coinDelta: reward.coinReward,
    reason: 'Household log entry: ' + title
  });

  var totals = updateUserTotals(userId, reward.xpReward, reward.coinReward);
  var streak = applyQualifyingAction(userId, todayDateString(), 'household_log', logId);
  var updatedUser = getUserById(userId);

  return {
    log: mapHouseholdLog({
      log_id: logId,
      type: type,
      title: title,
      details: details,
      user_id: userId,
      related_task_id: relatedTaskId,
      xp_reward: reward.xpReward,
      coin_reward: reward.coinReward,
      logged_at: nowIso(),
      created_at: nowIso()
    }),
    user: Object.assign({}, mapUserSummary(updatedUser), totals, {
      currentStreak: streak.currentStreak,
      longestStreak: streak.longestStreak
    })
  };
}

function resolveHouseholdLogReward(xpReward, coinReward) {
  if (xpReward !== undefined || coinReward !== undefined) {
    return {
      xpReward: toInt(xpReward, getConfigNumber('household_log_xp', 10)),
      coinReward: toInt(coinReward, getConfigNumber('household_log_coins', 3))
    };
  }

  return {
    xpReward: getConfigNumber('household_log_xp', 10),
    coinReward: getConfigNumber('household_log_coins', 3)
  };
}

function mapHouseholdLog(row) {
  return {
    logId: row.log_id,
    type: row.type,
    title: row.title,
    details: row.details || '',
    userId: row.user_id,
    relatedTaskId: row.related_task_id || '',
    xpReward: toInt(row.xp_reward),
    coinReward: toInt(row.coin_reward),
    loggedAt: row.logged_at || '',
    createdAt: row.created_at || ''
  };
}

// ============================================================
// Helper Functions
// ============================================================

function getCategoryColor(category) {
  var colors = {
    'Housing': '#4A90D9',
    'Groceries': '#E8A838',
    'Dining Out': '#E74C3C',
    'Fitness/Health': '#52BE80',
    'Education': '#9B59B6',
    'Holidays': '#F39C12',
    'Subscriptions': '#1ABC9C'
  };
  return colors[category] || '#95A5A6';
}

function initialize() {
  initializeHouseholdSpreadsheet();
  seedDemoConfig();
  return { success: true, message: 'Spreadsheet initialized and seeded.' };
}