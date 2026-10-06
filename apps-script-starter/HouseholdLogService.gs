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

function getUserHouseholdId(userId) {
  var user = findOneBy(SHEET_NAMES.USERS, function(row) {
    return row.user_id === userId;
  });
  return user ? user.household_id : '';
}

function createHouseholdLog(payload) {
  var userId = normalizeString(payload.userId);
  var type = normalizeString(payload.type);
  var title = normalizeString(payload.title);
  var details = normalizeString(payload.details);
  var relatedTaskId = normalizeString(payload.relatedTaskId);
  var requestId = payload.requestId || generateId('hl', SHEET_NAMES.HOUSEHOLD_LOG);

  if (!userId || !type || !title) {
    throw new Error('userId, type, and title are required.');
  }

  var user = getUserById(userId);
  if (!user) {
    throw new Error('User not found.');
  }

  var householdId = getUserHouseholdId(userId);
  if (!householdId) {
    throw new Error('User not associated with a household.');
  }

  // Verify related_task_id belongs to the same household if provided
  if (relatedTaskId) {
    var task = findOneBy(SHEET_NAMES.TASKS, function(row) {
      return row.task_id === relatedTaskId && row.household_id === householdId;
    });
    if (!task) {
      throw new Error('Related task not found or does not belong to your household.');
    }
  }

  // Use LockService for atomic operation
  var lock = LockService.getScriptLock();
  lock.waitLock(30);  // Wait up to 30 seconds for lock

  try {
    // Check idempotency - if a log with this requestId already exists, skip
    var existingLogs = findManyBy(SHEET_NAMES.HOUSEHOLD_LOG, function(row) {
      return row.request_id === requestId;
    });
    if (existingLogs.length > 0) {
      // Return existing log data
      var existing = existingLogs[0];
      var updatedUser = getUserById(userId);
      return {
        log: mapHouseholdLog({
          log_id: existing.log_id,
          type: existing.type,
          title: existing.title,
          details: existing.details || '',
          user_id: existing.user_id,
          related_task_id: existing.related_task_id || '',
          xp_reward: existing.xp_reward,
          coin_reward: existing.coin_reward,
          logged_at: existing.logged_at || '',
          created_at: existing.created_at || ''
        }),
        user: Object.assign({}, mapUserSummary(updatedUser), {
          xpTotal: updatedUser.xp_total,
          coinsTotal: updatedUser.coins_total,
          currentStreak: updatedUser.currentStreak,
          longestStreak: updatedUser.longestStreak
        })
      };
    }

    var reward = resolveHouseholdLogReward(payload.xpReward, payload.coinReward);
    var logId = generateId('h', SHEET_NAMES.HOUSEHOLD_LOG);

    appendRow(SHEET_NAMES.HOUSEHOLD_LOG, {
      log_id: logId,
      request_id: requestId,
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
  } finally {
    lock.release();
  }
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
