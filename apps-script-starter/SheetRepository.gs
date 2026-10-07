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
  WEALTH_MILESTONES: 'Wealth_Milestones'
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
  if (!sheetName || typeof sheetName !== 'string' || sheetName.trim() === '') {
    throw new Error('Invalid sheet access: sheetName parameter is missing or undefined. Check SHEET_NAMES constant definitions.');
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

function generateId(prefix, sheetName) {
  var rows = getSheetData(sheetName);
  return prefix + '_' + Utilities.getUuid().replace(/-/g, '').slice(0, 10);
}

function todayDateString() {
  return Utilities.formatDate(new Date(), 'UTC', 'yyyy-MM-dd');
}

function getHouseholdTimezone(householdId) {
  var config = getSheetData(SHEET_NAMES.CONFIG);
  var timezonePair = config.find(function(row) {
    return row.key === 'household_timezone';
  });
  if (timezonePair && timezonePair.value) {
    return timezonePair.value;
  }
  // Default to Africa/Nairobi for household operations
  return 'Africa/Nairobi';
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
