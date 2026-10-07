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
  MONEY_TRANSACTIONS: 'Money_Transactions',
  BILLS: 'Bills',
  SHOPPING_ITEMS: 'Shopping_Items',
  REFERENCE_LIBRARY: 'Reference_Library',
  EVENTS: 'Events',
  MAINTENANCE: 'Maintenance',
  SUBSCRIPTIONS: 'Subscriptions',
  SAVINGS_GOALS: 'Savings_Goals',
  BUDGETS: 'Budgets',
  NET_WORTH_SNAPSHOTS: 'Net_Worth_Snapshots',
  TRANSFERS: 'Transfers',
  ASSETS: 'Assets',
  LIABILITIES: 'Liabilities',
  INVESTMENTS: 'Investments',
  INVESTMENT_ACCOUNTS: 'Investment_Accounts',
  TRADE_JOURNAL: 'Trade_Journal',
  WEALTH_MILESTONES: 'Wealth_Milestones',
  INVESTMENT_TRANSACTIONS: 'Investment_Transactions',
  PASSIVE_INCOME: 'Passive_Income',
  PROP_ACCOUNTS: 'Prop_Accounts',
  PROP_PAYOUTS: 'Prop_Payouts',
  REQUEST_LOG: 'Request_Log'
};

// Per-execution caches. Every Apps Script execution is a fresh process,
// so these never go stale across requests - only within one request,
// which is why every write invalidates the affected sheet.
var SHEET_CACHE = {
  spreadsheet: null,
  sheets: {},
  data: {},
  headers: {}
};

function invalidateSheetCache(sheetName) {
  if (sheetName) {
    delete SHEET_CACHE.data[sheetName];
    delete SHEET_CACHE.headers[sheetName];
    delete SHEET_CACHE.sheets[sheetName];
    return;
  }
  SHEET_CACHE.data = {};
  SHEET_CACHE.headers = {};
  SHEET_CACHE.sheets = {};
}

function getSpreadsheet() {
  if (SHEET_CACHE.spreadsheet) {
    return SHEET_CACHE.spreadsheet;
  }

  var spreadsheetId = PropertiesService.getScriptProperties().getProperty('SPREADSHEET_ID');

  if (spreadsheetId) {
    SHEET_CACHE.spreadsheet = SpreadsheetApp.openById(spreadsheetId);
    return SHEET_CACHE.spreadsheet;
  }

  var active = SpreadsheetApp.getActiveSpreadsheet();
  if (!active) {
    throw new Error('No spreadsheet available. Set script property SPREADSHEET_ID or use a bound script.');
  }

  SHEET_CACHE.spreadsheet = active;
  return active;
}

function getSheet(sheetName) {
  if (!sheetName || typeof sheetName !== 'string' || sheetName.trim() === '') {
    throw new Error('Invalid sheet access: sheetName parameter is missing or undefined. Check SHEET_NAMES constant definitions.');
  }

  if (SHEET_CACHE.sheets[sheetName]) {
    return SHEET_CACHE.sheets[sheetName];
  }

  var spreadsheet = getSpreadsheet();
  var sheet = spreadsheet.getSheetByName(sheetName);
  if (!sheet) {
    try {
      sheet = spreadsheet.insertSheet(sheetName);
    } catch (e) {
      throw new Error('Missing required sheet: "' + sheetName + '" and unable to auto-create: ' + e.message);
    }
  }

  SHEET_CACHE.sheets[sheetName] = sheet;
  return sheet;
}

function getSheetData(sheetName) {
  if (SHEET_CACHE.data[sheetName]) {
    return SHEET_CACHE.data[sheetName];
  }

  var sheet = getSheet(sheetName);
  var values = sheet.getDataRange().getValues();

  var rows = [];
  if (values && values.length >= 2) {
    var headers = values[0];
    rows = values.slice(1).filter(function(row) {
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

  SHEET_CACHE.data[sheetName] = rows;
  return rows;
}

function appendRow(sheetName, object, headers) {
  var sheet = getSheet(sheetName);
  var sheetHeaders = headers || getHeaders(sheetName);
  var row = sheetHeaders.map(function(header) {
    return toSheetValue(object[header]);
  });
  sheet.appendRow(row);
  invalidateSheetCache(sheetName);
}

function updateRowByIndex(sheetName, rowIndex, object, headers) {
  var sheet = getSheet(sheetName);
  var sheetHeaders = headers || getHeaders(sheetName);
  var row = sheetHeaders.map(function(header) {
    return toSheetValue(object[header]);
  });
  sheet.getRange(rowIndex, 1, 1, row.length).setValues([row]);
  invalidateSheetCache(sheetName);
}

function getHeaders(sheetName) {
  if (SHEET_CACHE.headers[sheetName]) {
    return SHEET_CACHE.headers[sheetName];
  }

  var sheet = getSheet(sheetName);
  var lastColumn = Math.max(sheet.getLastColumn(), 1);
  var headers = sheet.getRange(1, 1, 1, lastColumn).getValues()[0];
  SHEET_CACHE.headers[sheetName] = headers;
  return headers;
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

function generateId(prefix, sheetName) {
  // No sheet read required: UUIDs are unique without counting existing rows.
  return prefix + '_' + Utilities.getUuid().replace(/-/g, '').slice(0, 10);
}

function todayDateString() {
  return Utilities.formatDate(new Date(), 'UTC', 'yyyy-MM-dd');
}

function getHouseholdTimezone(householdId) {
  // Default to Africa/Nairobi for household operations
  return getConfigString('household_timezone', 'Africa/Nairobi');
}

function todayDateStringByHousehold(householdId) {
  var timezone = getHouseholdTimezone(householdId);
  return Utilities.formatDate(new Date(), timezone, 'yyyy-MM-dd');
}

function addDays(dateString, days) {
  if (!dateString) return '';
  var date = new Date(Date.parse(dateString));
  date.setUTCDate(date.getUTCDate() + days);
  return Utilities.formatDate(date, 'UTC', 'yyyy-MM-dd');
}

// Household scoping helper.
// Empty household ids (legacy rows, single-household deployments) match any
// household, so data written before household scoping stays visible.
function rowMatchesHousehold(row, householdId) {
  if (!householdId) {
    return true;
  }
  var rowHousehold = normalizeString(row && row.household_id);
  if (!rowHousehold) {
    return true;
  }
  return rowHousehold === householdId;
}

function toSheetValue(value) {
  if (value === undefined || value === null) {
    return '';
  }
  if (typeof value === 'boolean') {
    return value ? 'true' : 'false';
  }
  if (value instanceof Date) {
    return value;
  }
  return value;
}

function toBoolean(value) {
  if (typeof value === 'boolean') {
    return value;
  }
  return String(value).toLowerCase() === 'true';
}

function toDouble(value, fallback) {
  var parsed = Number(value);
  return isNaN(parsed) ? (fallback || 0) : parsed;
}

function toInt(value, fallback) {
  var parsed = parseInt(value, 10);
  return isNaN(parsed) ? (fallback || 0) : parsed;
}

function normalizeString(value) {
  return value === undefined || value === null ? '' : String(value).trim();
}
