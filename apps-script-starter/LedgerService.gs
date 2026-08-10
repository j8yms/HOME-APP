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

function listLedger(payload) {
  var userId = normalizeString(payload.userId);
  var limit = toInt(payload.limit, 100) || 100;
  limit = Math.min(limit, 500);

  var entries = findManyBy(SHEET_NAMES.XP_LEDGER, function() {
    return true;
  }).map(mapLedgerEntry);

  if (userId) {
    entries = entries.filter(function(entry) {
      return entry.userId === userId;
    });
  }

  entries.sort(function(a, b) {
    return String(b.createdAt).localeCompare(String(a.createdAt));
  });

  return entries.slice(0, limit);
}

function mapLedgerEntry(row) {
  return {
    ledgerId: row.ledger_id,
    sourceType: row.source_type || '',
    actionType: row.action_type || '',
    xpDelta: toInt(row.xp_delta),
    coinDelta: toInt(row.coin_delta),
    reason: row.reason || '',
    userId: row.user_id || '',
    createdAt: row.created_at || ''
  };
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
