function listWorkouts(payload) {
  var userId = normalizeString(payload.userId);
  var workouts = findManyBy(SHEET_NAMES.WORKOUTS, function() {
    return true;
  }).map(mapWorkout);

  if (userId) {
    workouts = workouts.filter(function(workout) {
      return workout.userId === userId;
    });
  }

  return workouts.sort(function(a, b) {
    return String(b.loggedAt).localeCompare(String(a.loggedAt));
  });
}

function mapWorkout(row) {
  return {
    workoutId: row.workout_id,
    workoutType: row.workout_type,
    durationMinutes: toInt(row.duration_minutes),
    intensity: row.intensity || 'medium',
    notes: row.notes || '',
    xpReward: toInt(row.xp_reward),
    coinReward: toInt(row.coin_reward),
    loggedAt: row.logged_at || '',
    createdAt: row.created_at || ''
  };
}

function logWorkout(payload) {
  var userId = normalizeString(payload.userId);
  var workoutType = normalizeString(payload.workoutType);
  var durationMinutes = toInt(payload.durationMinutes);
  var intensity = normalizeString(payload.intensity) || 'medium';
  var notes = normalizeString(payload.notes);
  var bothPartners = Boolean(payload.bothPartners);

  if (!userId || !workoutType || durationMinutes <= 0) {
    throw new Error('userId, workoutType, and durationMinutes are required.');
  }

  var lock = LockService.getScriptLock();
  lock.waitLock(30000);

  try {
    var primary = logWorkoutForUser(userId, workoutType, durationMinutes, intensity, notes);
    if (!bothPartners) {
      return primary;
    }

    var partner = findManyBy(SHEET_NAMES.USERS, function(row) {
      return toBoolean(row.is_active) && row.user_id !== userId;
    })[0];
    if (!partner) {
      return primary;
    }

    var secondary = logWorkoutForUser(partner.user_id, workoutType, durationMinutes, intensity, notes);

    return {
      workoutId: primary.workoutId,
      xpReward: primary.xpReward,
      coinReward: primary.coinReward,
      user: primary.user,
      loggedForBoth: true,
      partnerWorkoutId: secondary.workoutId,
      partnerXpReward: secondary.xpReward,
      partnerCoinReward: secondary.coinReward,
      partnerUser: secondary.user
    };
  } finally {
    lock.releaseLock();
  }
}

function logWorkoutForUser(userId, workoutType, durationMinutes, intensity, notes) {
  var baseXp = Math.max(1, Math.floor(durationMinutes / 10)) * getConfigNumber('workout_xp_per_10_minutes', 10);
  var xpReward = Math.round(baseXp * 1.05);
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
}
