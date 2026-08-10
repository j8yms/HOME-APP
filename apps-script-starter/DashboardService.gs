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

  var couplesStreak = readCouplesStreak();

  return {
    currentUser: mapUserSummary(user),
    partner: partner ? mapUserSummary(partner) : null,
    tasksDueToday: tasksDueToday,
    overdueTasks: overdueTasks,
    couplesStreak: couplesStreak
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
