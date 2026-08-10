// HOME MANAGER - All-in-one Apps Script backend
// Paste this entire file into a single Apps Script `.gs` file.

function doGet(e) {
  try {
    var payload = Object.assign({}, (e && e.parameter) || {});
    var route = resolveRoute(e, payload);
    return dispatchRoute(route, payload);
  } catch (error) {
    return handleRouteError(error);
  }
}

function doPost(e) {
  try {
    var payload = parsePostBody(e);
    var route = resolveRoute(e, payload);
    return dispatchRoute(route, payload);
  } catch (error) {
    return handleRouteError(error);
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
  CONFIG: 'Config'
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

function initializeHouseholdSpreadsheet() {
  var spreadsheet = getSpreadsheet();

  var definitions = [
    {
      name: SHEET_NAMES.USERS,
      headers: ['user_id', 'device_id', 'email', 'display_name', 'photo_url', 'role', 'xp_total', 'level', 'coins_total', 'current_streak', 'longest_streak', 'last_qualifying_date', 'created_at', 'updated_at', 'is_active']
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
    user: mapUserSummary(user),
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
        is_active: 'true'
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
    user: mapUserSummary(user),
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

function mapUserSummary(row) {
  return {
    userId: row.user_id,
    email: row.email,
    displayName: row.display_name,
    photoUrl: row.photo_url || '',
    role: row.role,
    xpTotal: toInt(row.xp_total),
    level: toInt(row.level, 1),
    coinsTotal: toInt(row.coins_total),
    currentStreak: toInt(row.current_streak),
    longestStreak: toInt(row.longest_streak),
    lastQualifyingDate: row.last_qualifying_date || ''
  };
}

function appendLedgerEntry(entry) {
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
    appendLedgerEntry({
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

function getDashboard(payload) {
  var userId = normalizeString(payload.userId);
  if (!userId) {
    throw new Error('userId is required.');
  }

  var user = getUserById(userId);
  if (!user) {
    throw new Error('User not found.');
  }

  var partner = findManyBy(SHEET_NAMES.USERS, function(row) {
    return toBoolean(row.is_active) && row.user_id !== userId;
  })[0] || null;

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

    appendLedgerEntry({
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

    appendLedgerEntry({
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

  appendLedgerEntry({
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

  appendLedgerEntry({
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
