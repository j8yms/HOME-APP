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

  var couplesStreak = updateCouplesStreak(currentDate);

  return {
    currentStreak: newStreak,
    longestStreak: Math.max(longestStreak, newStreak),
    streakBonusXp: bonusXp,
    couplesStreak: couplesStreak
  };
}

function updateCouplesStreak(activityDate) {
  var currentDate = activityDate || todayDateString();
  var activeUsers = findManyBy(SHEET_NAMES.USERS, function(row) {
    return toBoolean(row.is_active);
  });

  if (activeUsers.length < 2) {
    return { currentStreak: 0, longestStreak: 0 };
  }

  var teamDay = activeUsers.every(function(row) {
    return normalizeString(row.last_qualifying_date) === currentDate;
  });

  if (!teamDay) {
    return readCouplesStreak();
  }

  var config = getConfigMap();
  var lastTeamDate = normalizeString(config.couples_last_team_date);
  var currentStreak = toInt(config.couples_current_streak);
  var longestStreak = toInt(config.couples_longest_streak);
  var newStreak = currentStreak;

  if (!lastTeamDate) {
    newStreak = 1;
  } else if (lastTeamDate === currentDate) {
    newStreak = Math.max(1, currentStreak);
  } else if (dateDiffInDays(lastTeamDate, currentDate) === 1) {
    newStreak = Math.max(1, currentStreak + 1);
  } else {
    newStreak = 1;
  }

  longestStreak = Math.max(longestStreak, newStreak);

  upsertConfig('couples_current_streak', newStreak);
  upsertConfig('couples_longest_streak', longestStreak);
  upsertConfig('couples_last_team_date', currentDate);

  return {
    currentStreak: newStreak,
    longestStreak: longestStreak
  };
}

function readCouplesStreak() {
  var config = getConfigMap();
  return {
    currentStreak: toInt(config.couples_current_streak),
    longestStreak: toInt(config.couples_longest_streak)
  };
}

function upsertConfig(key, value) {
  var existing = findOneBy(SHEET_NAMES.CONFIG, function(row) {
    return row.config_key === key;
  });

  if (existing) {
    existing.config_value = String(value);
    existing.updated_at = nowIso();
    updateRowByIndex(SHEET_NAMES.CONFIG, existing.__rowIndex, existing);
  } else {
    appendRow(SHEET_NAMES.CONFIG, {
      config_key: key,
      config_value: String(value),
      updated_at: nowIso()
    });
  }
}

function dateDiffInDays(fromDateString, toDateString) {
  var from = new Date(fromDateString + 'T00:00:00Z');
  var to = new Date(toDateString + 'T00:00:00Z');
  var msPerDay = 24 * 60 * 60 * 1000;
  return Math.round((to.getTime() - from.getTime()) / msPerDay);
}
