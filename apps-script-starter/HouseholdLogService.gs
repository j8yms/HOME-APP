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
