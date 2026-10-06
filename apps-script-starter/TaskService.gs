function listTasks(payload) {
  var status = normalizeString(payload.status);
  var assignedToUserId = normalizeString(payload.assignedToUserId);
  var recurrenceId = normalizeString(payload.recurrenceId);
  var householdId = getUserHouseholdId(assignedToUserId) || '';
  var hasStatus = status !== '';

  return findManyBy(SHEET_NAMES.TASKS, function(row) {
    var statusMatch = hasStatus ? row.status === status : row.status !== 'archived';
    var householdMatch = !householdId || row.household_id === householdId;
    var recurrenceMatch = recurrenceId ? row.recurrence_id === recurrenceId : true;
    var assigneeMatch = !assignedToUserId || row.assigned_to_user_id === assignedToUserId;
    return statusMatch && householdMatch && recurrenceMatch && assigneeMatch;
  }).map(mapTask);
}

function deleteTask(payload) {
  var taskId = normalizeString(payload.taskId);
  var incomingVersion = toInt(payload.version);

  if (!taskId) {
    throw new Error('taskId is required.');
  }

  var task = findOneBy(SHEET_NAMES.TASKS, function(row) {
    return row.task_id === taskId;
  });

  if (!task) {
    throw new Error('TASK_NOT_FOUND');
  }

  if (toInt(task.version) !== incomingVersion) {
    throw new Error('STALE_TASK_VERSION');
  }

  task.status = 'archived';
  task.updated_at = nowIso();
  task.version = toInt(task.version) + 1;
  updateRowByIndex(SHEET_NAMES.TASKS, task.__rowIndex, task);
  return mapTask(task);
}

function createTask(payload) {
  validateTaskPayload(payload, true);

  var priority = normalizeString(payload.priority) || 'medium';
  var recurrenceId = normalizeString(payload.recurrenceId);
  var reward = resolveTaskReward(priority, payload.xpReward, payload.coinReward);
  var assignedToUserId = normalizeString(payload.assignedToUserId);
  var householdId = getUserHouseholdId(assignedToUserId) || '';

  var task = {
    task_id: generateId('t', SHEET_NAMES.TASKS),
    title: normalizeString(payload.title),
    description: normalizeString(payload.description),
    category: normalizeString(payload.category) || 'General',
    assigned_to_user_id: assignedToUserId,
    assignee_label: normalizeAssigneeLabel(payload.assigneeLabel, assignedToUserId),
    created_by_user_id: normalizeString(payload.createdByUserId),
    status: 'open',
    priority: priority,
    due_date: normalizeString(payload.dueDate),
    repeat_rule: normalizeString(payload.repeatRule),
    recurrence_id: recurrenceId,  // series_id for recurring task groups
    xp_reward: reward.xpReward,
    coin_reward: reward.coinReward,
    streak_eligible: String(Boolean(payload.streakEligible)),
    household_id: householdId,
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
  if (payload.assigneeLabel !== undefined) task.assignee_label = normalizeAssigneeLabel(payload.assigneeLabel, task.assigned_to_user_id);
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

    var recurringTask = createNextOccurrence(task);

    return {
      task: mapTask(task),
      recurringTask: recurringTask,
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

function claimTask(payload) {
  var taskId = normalizeString(payload.taskId);
  var claimedByUserId = normalizeString(payload.claimedByUserId);

  if (!taskId || !claimedByUserId) {
    throw new Error('taskId and claimedByUserId are required.');
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

    if (task.status !== 'open') {
      throw new Error('TASK_ALREADY_COMPLETED');
    }

    var currentLabel = normalizeAssigneeLabel(task.assignee_label, task.assigned_to_user_id);
    if (currentLabel !== 'shared') {
      throw new Error('TASK_NOT_CLAIMABLE');
    }

    var claimer = getUserById(claimedByUserId);
    if (!claimer) {
      throw new Error('USER_NOT_FOUND');
    }

    task.assigned_to_user_id = claimedByUserId;
    task.assignee_label = normalizeString(claimer.household_side) || 'shared';
    task.updated_at = nowIso();
    task.version = toInt(task.version) + 1;
    updateRowByIndex(SHEET_NAMES.TASKS, task.__rowIndex, task);

    return {
      task: mapTask(task),
      user: mapUserSummary(claimer)
    };
  } finally {
    lock.releaseLock();
  }
}

function normalizeAssigneeLabel(label, assignedToUserId) {
  var normalized = normalizeString(label).toLowerCase();
  if (normalized === 'his' || normalized === 'hers' || normalized === 'shared') {
    return normalized;
  }

  if (assignedToUserId) {
    var user = getUserById(assignedToUserId);
    var side = normalizeString(user ? user.household_side : '').toLowerCase();
    if (side === 'his' || side === 'hers') {
      return side;
    }
  }

  return 'shared';
}

function createNextOccurrence(completedTask) {
  var repeatRule = normalizeString(completedTask.repeat_rule);
  if (repeatRule !== 'daily' && repeatRule !== 'weekly' && repeatRule !== 'monthly') {
    return null;
  }

  var nextTask = {
    task_id: generateId('t', SHEET_NAMES.TASKS),
    title: completedTask.title,
    description: completedTask.description || '',
    category: completedTask.category || '',
    assigned_to_user_id: completedTask.assigned_to_user_id || '',
    assignee_label: normalizeAssigneeLabel(completedTask.assignee_label, completedTask.assigned_to_user_id),
    created_by_user_id: completedTask.created_by_user_id || '',
    status: 'open',
    priority: completedTask.priority || 'medium',
    due_date: computeNextDueDate(repeatRule, completedTask.due_date),
    repeat_rule: repeatRule,
    xp_reward: completedTask.xp_reward,
    coin_reward: completedTask.coin_reward,
    streak_eligible: String(toBoolean(completedTask.streak_eligible)),
    completed_at: '',
    completed_by_user_id: '',
    created_at: nowIso(),
    updated_at: nowIso(),
    version: 1
  };

  appendRow(SHEET_NAMES.TASKS, nextTask);
  return mapTask(nextTask);
}

function computeNextDueDate(repeatRule, fromDate) {
  var base = parseDate(fromDate);
  if (!base) {
    base = new Date();
  }
  var next = new Date(base.getTime());

  if (repeatRule === 'daily') {
    next.setUTCDate(next.getUTCDate() + 1);
  } else if (repeatRule === 'weekly') {
    next.setUTCDate(next.getUTCDate() + 7);
  } else if (repeatRule === 'monthly') {
    var year = next.getUTCFullYear();
    var month = next.getUTCMonth();
    var day = next.getUTCDate();
    var daysInNextMonth = new Date(Date.UTC(year, month + 2, 0)).getUTCDate();
    var targetDay = Math.min(day, daysInNextMonth);
    next = new Date(Date.UTC(year, month + 1, targetDay));
  }

  return Utilities.formatDate(next, 'UTC', 'yyyy-MM-dd');
}

function parseDate(dateString) {
  var normalized = String(dateString || '').trim();
  if (!normalized) {
    return null;
  }

  var plainMatch = normalized.match(/^(\d{4})-(\d{2})-(\d{2})$/);
  if (plainMatch) {
    return new Date(Date.UTC(Number(plainMatch[1]), Number(plainMatch[2]) - 1, Number(plainMatch[3])));
  }

  var parsed = new Date(normalized);
  if (isNaN(parsed.getTime())) {
    return null;
  }

  var localDate = Utilities.formatDate(parsed, Session.getScriptTimeZone(), 'yyyy-MM-dd').split('-');
  return new Date(Date.UTC(Number(localDate[0]), Number(localDate[1]) - 1, Number(localDate[2])));
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
    assigneeLabel: row.assignee_label || '',
    createdByUserId: row.created_by_user_id || '',
    status: row.status,
    priority: row.priority,
    dueDate: row.due_date || '',
    repeatRule: row.repeat_rule || '',
    recurrenceId: row.recurrence_id || '',
    xpReward: toInt(row.xp_reward),
    coinReward: toInt(row.coin_reward),
    streakEligible: toBoolean(row.streak_eligible),
    completedAt: row.completed_at || '',
    completedByUserId: row.completed_by_user_id || '',
    createdAt: row.created_at || '',
    updatedAt: row.updated_at || '',
    version: toInt(row.version, 1),
    householdId: row.household_id || ''
  };
}
