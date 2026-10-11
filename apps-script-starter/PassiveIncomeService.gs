// Request 8: daily passive-income logging.
//
// Passive_Income rows remain the reusable income SOURCES (no new source is
// required per entry). Each daily receipt is a Passive_Income_Logs row with:
// amount, income_date, source reference (income_id and/or free-text source
// name), income type/category, optional notes, and entry kind
// ('actual' received vs 'projected').
//
// A log can optionally create ONE linked Money_Transactions income row
// (category 'passive_income'); the link id is stored on the log so edits and
// deletes update the linked row too — never double-counted.

var PASSIVE_INCOME_KINDS = ['actual', 'projected'];

function resolvePassiveSourceName(incomeId, fallbackName, householdId) {
  if (incomeId) {
    var source = findOneBy(SHEET_NAMES.PASSIVE_INCOME, function(row) {
      return row.income_id === incomeId && rowMatchesHousehold(row, householdId);
    });
    if (!source) {
      throw new Error('Income source not found.');
    }
    return normalizeString(source.name) || fallbackName;
  }
  return fallbackName;
}

function mapPassiveIncomeLog(row, householdId) {
  var incomeId = normalizeString(row.income_id);
  var storedName = normalizeString(row.source_name);
  var sourceName = storedName;
  if (incomeId && !sourceName) {
    var source = findOneBy(SHEET_NAMES.PASSIVE_INCOME, function(candidate) {
      return candidate.income_id === incomeId;
    });
    if (source) sourceName = normalizeString(source.name);
  }
  return {
    logId: row.log_id,
    incomeId: incomeId,
    sourceName: sourceName,
    amount: Number(row.amount) || 0,
    incomeDate: normalizeString(row.income_date),
    incomeType: normalizeString(row.income_type),
    notes: normalizeString(row.notes),
    entryKind: normalizeString(row.entry_kind) || 'actual',
    linkedTransactionId: normalizeString(row.linked_transaction_id),
    userId: normalizeString(row.user_id),
    createdAt: normalizeString(row.created_at),
    updatedAt: normalizeString(row.updated_at)
  };
}

function normalizePassiveKind(value) {
  var kind = normalizeString(value).toLowerCase() || 'actual';
  if (PASSIVE_INCOME_KINDS.indexOf(kind) < 0) {
    throw new Error('entryKind must be one of: actual, projected.');
  }
  return kind;
}

function normalizeIncomeDate(value, householdId) {
  var raw = normalizeString(value);
  if (!raw) {
    return todayDateStringByHousehold(householdId);
  }
  var match = raw.match(/^(\d{4})-(\d{2})-(\d{2})/);
  if (!match) {
    throw new Error('incomeDate must be yyyy-MM-dd.');
  }
  return match[1] + '-' + match[2] + '-' + match[3];
}

function listPassiveIncomeLogs(payload) {
  var userId = normalizeString(payload.userId);
  var householdId = getUserHouseholdId(userId);
  var month = normalizeString(payload.month);
  var incomeId = normalizeString(payload.incomeId);
  var kind = normalizeString(payload.kind).toLowerCase();

  var logs = findManyBy(SHEET_NAMES.PASSIVE_INCOME_LOGS, function(row) {
    if (!rowMatchesHousehold(row, householdId)) return false;
    if (month && normalizeString(row.income_date).substring(0, 7) !== month) return false;
    if (incomeId && normalizeString(row.income_id) !== incomeId) return false;
    if (kind && normalizeString(row.entry_kind).toLowerCase() !== kind) return false;
    return true;
  }).map(function(row) {
    return mapPassiveIncomeLog(row, householdId);
  });

  logs.sort(function(a, b) {
    var dateCompare = String(b.incomeDate).localeCompare(String(a.incomeDate));
    if (dateCompare !== 0) return dateCompare;
    return String(b.createdAt).localeCompare(String(a.createdAt));
  });

  return logs.slice(0, 500);
}

function upsertLinkedPassiveTransaction(log, householdId, userId, sourceName) {
  // Creates or refreshes the single linked finance row for an 'actual' log.
  // Projected entries never touch confirmed balances.
  var linkedId = normalizeString(log.linked_transaction_id);
  if (linkedId) {
    var linked = findOneBy(SHEET_NAMES.MONEY_TRANSACTIONS, function(row) {
      return row.transaction_id === linkedId;
    });
    if (linked) {
      linked.type = 'income';
      linked.description = 'Passive income: ' + sourceName;
      linked.category = normalizeString(log.income_type) || 'passive_income';
      linked.amount = Number(log.amount) || 0;
      linked.wallet = 'joint';
      linked.updated_at = nowIso();
      updateRowByIndex(SHEET_NAMES.MONEY_TRANSACTIONS, linked.__rowIndex, linked);
      return linkedId;
    }
  }
  var transaction = {
    transaction_id: generateId('m', SHEET_NAMES.MONEY_TRANSACTIONS),
    type: 'income',
    description: 'Passive income: ' + sourceName,
    category: normalizeString(log.income_type) || 'passive_income',
    amount: Number(log.amount) || 0,
    wallet: 'joint',
    user_id: userId,
    household_id: householdId,
    created_at: nowIso()
  };
  appendRow(SHEET_NAMES.MONEY_TRANSACTIONS, transaction);
  return transaction.transaction_id;
}

function savePassiveIncomeLog(payload) {
  var userId = normalizeString(payload.userId);
  var amount = Number(payload.amount);
  var incomeId = normalizeString(payload.incomeId);
  var sourceName = normalizeString(payload.sourceName);
  var linkTransaction = payload.linkTransaction === true || payload.linkTransaction === 'true';

  if (!userId) {
    throw new Error('userId is required.');
  }
  if (!(amount > 0)) {
    throw new Error('amount must be a positive number.');
  }
  if (!incomeId && !sourceName) {
    throw new Error('Provide an income source or a source name.');
  }

  var householdId = getUserHouseholdId(userId);
  var kind = normalizePassiveKind(payload.entryKind);
  var incomeDate = normalizeIncomeDate(payload.incomeDate, householdId);
  var resolvedName = resolvePassiveSourceName(incomeId, sourceName, householdId) || 'Passive income';
  var incomeType = normalizeString(payload.incomeType);
  var notes = normalizeString(payload.notes);

  var lock = LockService.getScriptLock();
  lock.waitLock(30000);

  try {
    var logId = normalizeString(payload.logId);
    var log = logId ? findOneBy(SHEET_NAMES.PASSIVE_INCOME_LOGS, function(row) {
      return row.log_id === logId;
    }) : null;
    if (logId && !log) {
      throw new Error('Passive income log not found.');
    }
    if (log && householdId && log.household_id && log.household_id !== householdId) {
      throw new Error('Passive income log does not belong to your household.');
    }

    var shouldLink = linkTransaction && kind === 'actual';

    if (log) {
      log.income_id = incomeId;
      log.source_name = resolvedName;
      log.amount = amount;
      log.income_date = incomeDate;
      log.income_type = incomeType;
      log.notes = notes;
      log.entry_kind = kind;
      log.updated_at = nowIso();
      if (shouldLink) {
        log.linked_transaction_id = upsertLinkedPassiveTransaction(log, householdId, userId, resolvedName);
      } else if (normalizeString(log.linked_transaction_id)) {
        // Unlinked (or now projected): remove the previously linked finance
        // row so reports never double-count it.
        removeMoneyTransactionRows(normalizeString(log.linked_transaction_id), householdId);
        log.linked_transaction_id = '';
      }
      updateRowByIndex(SHEET_NAMES.PASSIVE_INCOME_LOGS, log.__rowIndex, log);
    } else {
      log = {
        log_id: generateId('pil', SHEET_NAMES.PASSIVE_INCOME_LOGS),
        household_id: householdId,
        user_id: userId,
        income_id: incomeId,
        source_name: resolvedName,
        amount: amount,
        income_date: incomeDate,
        income_type: incomeType,
        notes: notes,
        entry_kind: kind,
        linked_transaction_id: '',
        created_at: nowIso(),
        updated_at: nowIso()
      };
      if (shouldLink) {
        log.linked_transaction_id = upsertLinkedPassiveTransaction(log, householdId, userId, resolvedName);
      }
      appendRow(SHEET_NAMES.PASSIVE_INCOME_LOGS, log);
    }

    return mapPassiveIncomeLog(log, householdId);
  } finally {
    lock.releaseLock();
  }
}

function deletePassiveIncomeLog(payload) {
  var logId = normalizeString(payload.logId);
  var userId = normalizeString(payload.userId);

  if (!logId) {
    throw new Error('logId is required.');
  }
  if (!userId) {
    throw new Error('userId is required.');
  }

  var householdId = getUserHouseholdId(userId);

  var lock = LockService.getScriptLock();
  lock.waitLock(30000);

  try {
    var log = findOneBy(SHEET_NAMES.PASSIVE_INCOME_LOGS, function(row) {
      return row.log_id === logId;
    });
    if (!log) {
      throw new Error('Passive income log not found.');
    }
    if (householdId && log.household_id && log.household_id !== householdId) {
      throw new Error('Passive income log does not belong to your household.');
    }

    // Deleting a log removes its linked finance row too, keeping reports
    // and balances consistent.
    if (normalizeString(log.linked_transaction_id)) {
      removeMoneyTransactionRows(normalizeString(log.linked_transaction_id), householdId);
    }
    deleteRowByIndex(SHEET_NAMES.PASSIVE_INCOME_LOGS, log.__rowIndex);

    return { logId: logId };
  } finally {
    lock.releaseLock();
  }
}
