function getDashboard(payload) {
  var userId = normalizeString(payload.userId);
  if (!userId) {
    throw new Error('userId is required.');
  }

  var user = getUserById(userId);
  if (!user) {
    throw new Error('User not found.');
  }

  var householdId = normalizeString(user.household_id);
  var partner = findManyBy(SHEET_NAMES.USERS, function(row) {
    return toBoolean(row.is_active) && row.user_id !== userId && rowMatchesHousehold(row, householdId);
  })[0] || null;

  var today = todayDateStringByHousehold(householdId);

  // ===== B. TODAY section =====
  var openTasks = findManyBy(SHEET_NAMES.TASKS, function(row) {
    return row.status !== 'completed' && row.status !== 'archived' && rowMatchesHousehold(row, householdId);
  }).map(mapTask);

  var tasksDueToday = openTasks.filter(function(task) {
    return task.dueDate === today;
  }).sort(compareTasksForDashboard);

  var overdueTasks = openTasks.filter(function(task) {
    return task.dueDate && task.dueDate < today;
  }).sort(compareTasksForDashboard);

  // Today's bills (due today)
  var todayBills = findManyBy(SHEET_NAMES.BILLS, function(row) {
    return row.status === 'pending' && rowMatchesHousehold(row, householdId) && row.due_date === today;
  }).map(mapBill).sort(compareBillsByDueDate);

  // Upcoming bills (due within 7 days or still pending past today)
  var sevenDaysLater = addDays(today, 7);
  var upcomingBills = findManyBy(SHEET_NAMES.BILLS, function(row) {
    return row.status === 'pending' && rowMatchesHousehold(row, householdId) && row.due_date && row.due_date >= today && row.due_date <= sevenDaysLater;
  }).map(mapBill).sort(compareBillsByDueDate);

  // Overdue bills
  var overdueBills = findManyBy(SHEET_NAMES.BILLS, function(row) {
    return row.status === 'pending' && rowMatchesHousehold(row, householdId) && row.due_date && row.due_date < today;
  }).map(mapBill).sort(compareBillsByDueDate);

  // Today's events (events starting today)
  var todayEvents = findManyBy(SHEET_NAMES.EVENTS, function(row) {
    return rowMatchesHousehold(row, householdId) && row.start === today;
  }).map(mapEvent).sort(function(a, b) {
    return String(a.start).localeCompare(String(b.start));
  });

  // Quick actions summary
  var incomeThisMonth = getIncomeThisMonth(householdId);
  var expensesThisMonth = getExpensesThisMonth(householdId);
  var openShoppingItems = findManyBy(SHEET_NAMES.SHOPPING_ITEMS, function(row) {
    return row.status === 'open' && rowMatchesHousehold(row, householdId);
  }).map(mapShoppingItem).sort(function(a, b) {
    return String(b.createdAt).localeCompare(String(a.createdAt));
  });

  // ===== C. MONEY section =====
  var summary = getFinanceSummaryByHousehold(householdId);
  var remainingBudget = summary.balance - expensesThisMonth;
  var subscriptionTotalMonthly = getSubscriptionTotalMonthly(householdId);

  // ===== E. GOALS section =====
  var savingsGoals = findManyBy(SHEET_NAMES.SAVINGS_GOALS, function(row) {
    return rowMatchesHousehold(row, householdId);
  }).map(mapSavingsGoal);

  // ===== F. ACTIVITY section =====
  var activityFeed = buildActivityFeed(householdId, today);

  // ===== G. GAMIFICATION section (secondary) =====
  var xpTotal = toInt(user.xp_total);
  var level = toInt(user.level, 1);
  var coinsTotal = toInt(user.coins_total);
  var currentStreak = toInt(user.current_streak);
  var longestStreak = toInt(user.longest_streak);

  return {
    // A. Greeting
    greeting: {
      currentUser: mapUserSummary(user),
      date: today,
      householdName: getHouseholdName(householdId)
    },

    // B. TODAY
    today: {
      tasksDueToday: tasksDueToday,
      overdueTasks: overdueTasks,
      todayBills: todayBills,
      upcomingBills: upcomingBills,
      overdueBills: overdueBills,
      todayEvents: todayEvents,
      quickActions: {
        incomeThisMonth: incomeThisMonth,
        expensesThisMonth: expensesThisMonth,
        openShoppingItems: openShoppingItems.slice(0, 5), // top 5
        remainingBudget: Math.max(0, remainingBudget)
      }
    },

    // C. MONEY
    money: {
      totalBalance: summary.balance,
      accountBreakdown: summary.wallets,
      incomeThisMonth: incomeThisMonth,
      expensesThisMonth: expensesThisMonth,
      remainingBudget: remainingBudget,
      subscriptionTotalMonthly: subscriptionTotalMonthly,
      upcomingSubscriptions: getUpcomingSubscriptions(householdId)
    },

    // D. HOUSEHOLD
    household: {
      openTasks: openTasks.sort(compareTasksForDashboard),
      completedTasks: findManyBy(SHEET_NAMES.TASKS, function(row) {
        return row.status === 'completed' && rowMatchesHousehold(row, householdId);
      }).map(mapTask).sort(compareTasksForDashboard),
      maintenanceIssues: findManyBy(SHEET_NAMES.MAINTENANCE, function(row) {
        return row.status !== 'completed' && rowMatchesHousehold(row, householdId);
      }).map(mapMaintenance).sort(compareMaintenanceByDueDate),
      shoppingItems: openShoppingItems,
      importantReminders: buildImportantReminders(householdId, today)
    },

    // E. GOALS
    goals: {
      savingsGoals: savingsGoals,
      monthlyTargets: getMonthlyTargets(householdId)
    },

    // F. ACTIVITY
    activity: activityFeed,

    // G. GAMIFICATION (secondary)
    gamification: {
      xpTotal: xpTotal,
      level: level,
      coinsTotal: coinsTotal,
      currentStreak: currentStreak,
      longestStreak: longestStreak
    },

    // Top-level user mirrors (Android DashboardResponse expects these)
    currentUser: mapUserSummary(user),
    partner: partner ? mapUserSummary(partner) : null,

    // Household metadata
    householdId: householdId,
    currency: getConfigString('currency', 'USD')
  };
}

// Helper functions

function mapEvent(row) {
  if (!row) return null;
  return {
    eventId: row.event_id || '',
    title: row.title || '',
    category: row.category || '',
    start: row.start || '',
    end: row.end || '',
    description: row.description || '',
    createdByUserId: row.created_by_user_id || ''
  };
}

function mapMaintenance(row) {
  if (!row) return null;
  return {
    maintenanceId: row.maintenance_id || '',
    title: row.title || '',
    category: row.category || '',
    dueDate: row.due_date || '',
    priority: row.priority || 'medium',
    status: row.status || 'open',
    assignedToUserId: row.assigned_to_user_id || ''
  };
}

// Monthly targets come from active budgets for the given month - never
// from hardcoded sample values.
function getMonthlyTargets(householdId) {
  var month = todayDateStringByHousehold(householdId).substring(0, 7);
  var targets = {};

  findManyBy(SHEET_NAMES.BUDGETS, function(row) {
    return rowMatchesHousehold(row, householdId) && row.month === month;
  }).forEach(function(row) {
    var category = normalizeString(row.category);
    if (category) {
      targets[category] = (targets[category] || 0) + (Number(row.budget_limit) || 0);
    }
  });

  return targets;
}

function getHouseholdName(householdId) {
  var configured = getConfigString('household_name', '');
  if (configured) {
    return configured;
  }
  if (!householdId) {
    return 'Our Household';
  }
  var users = findManyBy(SHEET_NAMES.USERS, function(row) {
    return rowMatchesHousehold(row, householdId) && toBoolean(row.is_active);
  });
  if (users.length > 0 && normalizeString(users[0].display_name)) {
    return users[0].display_name + "'s Home";
  }
  return 'Our Household';
}

function currentMonthPrefix(householdId) {
  return todayDateStringByHousehold(householdId).substring(0, 7);
}

function getIncomeThisMonth(householdId) {
  var monthPrefix = currentMonthPrefix(householdId);
  var income = 0;
  getSheetData(SHEET_NAMES.MONEY_TRANSACTIONS).forEach(function(t) {
    if (!rowMatchesHousehold(t, householdId)) return;
    if (normalizeString(t.created_at).substring(0, 7) !== monthPrefix) return;
    if (t.type === 'income') {
      income += Number(t.amount) || 0;
    }
  });
  return Math.round(income * 100) / 100;
}

function getExpensesThisMonth(householdId) {
  var monthPrefix = currentMonthPrefix(householdId);
  var expenses = 0;
  getSheetData(SHEET_NAMES.MONEY_TRANSACTIONS).forEach(function(t) {
    if (!rowMatchesHousehold(t, householdId)) return;
    if (normalizeString(t.created_at).substring(0, 7) !== monthPrefix) return;
    if (t.type === 'expense') {
      expenses += Number(t.amount) || 0;
    }
  });
  return Math.round(expenses * 100) / 100;
}

function subscriptionMonthlyAmount(subscription) {
  var amount = Number(subscription.amount) || 0;
  var interval = normalizeString(subscription.billing_interval).toLowerCase();
  if (interval === 'yearly' || interval === 'annual' || interval === 'annually') {
    return amount / 12;
  }
  if (interval === 'quarterly') {
    return amount / 3;
  }
  if (interval === 'weekly') {
    return amount * 4.33;
  }
  return amount;
}

function getSubscriptionTotalMonthly(householdId) {
  var subs = findManyBy(SHEET_NAMES.SUBSCRIPTIONS, function(row) {
    return rowMatchesHousehold(row, householdId) && toBoolean(row.is_active);
  });
  var total = 0;
  subs.forEach(function(s) {
    total += subscriptionMonthlyAmount(s);
  });
  return Math.round(total * 100) / 100;
}

function getUpcomingSubscriptions(householdId) {
  var today = todayDateStringByHousehold(householdId);
  var subs = findManyBy(SHEET_NAMES.SUBSCRIPTIONS, function(row) {
    return rowMatchesHousehold(row, householdId) && toBoolean(row.is_active) && row.next_renewal_date && row.next_renewal_date >= today;
  }).sort(function(a, b) {
    return String(a.next_renewal_date).localeCompare(String(b.next_renewal_date));
  });
  return subs.slice(0, 5).map(mapSubscription);
}

// Canonical activity entries. Every source guarantees title, description,
// type and timestamp so Android can parse them as non-null fields.
function buildActivityFeed(householdId, today) {
  var feed = [];
  var symbol = getCurrencySymbol();

  var recentTransactions = findManyBy(SHEET_NAMES.MONEY_TRANSACTIONS, function(row) {
    return rowMatchesHousehold(row, householdId);
  }).sort(function(a, b) {
    return String(b.created_at).localeCompare(String(a.created_at));
  }).slice(0, 5).map(function(row) {
    var amount = Number(row.amount) || 0;
    var type = normalizeString(row.type);
    var title;
    if (type === 'income') {
      title = 'Income recorded';
    } else if (type === 'savings_deposit') {
      title = 'Savings deposit';
    } else if (type === 'savings_withdraw') {
      title = 'Savings withdrawal';
    } else {
      title = 'Expense recorded';
    }
    return {
      title: title,
      description: (row.description || 'Transaction') + ' - ' + symbol + ' ' + amount,
      type: 'transaction',
      timestamp: row.created_at || today
    };
  });

  var recentTasks = findManyBy(SHEET_NAMES.TASKS, function(row) {
    return row.status === 'completed' && rowMatchesHousehold(row, householdId) && row.completed_at;
  }).sort(function(a, b) {
    return String(b.completed_at).localeCompare(String(a.completed_at));
  }).slice(0, 3).map(function(row) {
    return {
      title: 'Task completed',
      description: row.title || 'Task',
      type: 'task',
      timestamp: row.completed_at || today
    };
  });

  var paidBills = findManyBy(SHEET_NAMES.BILLS, function(row) {
    return row.status === 'paid' && rowMatchesHousehold(row, householdId);
  }).sort(function(a, b) {
    return String(b.paid_at).localeCompare(String(a.paid_at));
  }).slice(0, 3).map(function(row) {
    return {
      title: (row.title || 'Bill') + ' paid',
      description: 'Paid ' + symbol + ' ' + (Number(row.amount) || 0),
      type: 'bill',
      timestamp: row.paid_at || today
    };
  });

  var recentLogs = findManyBy(SHEET_NAMES.HOUSEHOLD_LOG, function(row) {
    return rowMatchesHousehold(row, householdId);
  }).sort(function(a, b) {
    return String(b.created_at || b.logged_at).localeCompare(String(a.created_at || a.logged_at));
  }).slice(0, 3).map(function(row) {
    return {
      title: row.title || 'Household log',
      type: row.type || 'log',
      description: normalizeString(row.details) || ('Logged ' + (row.type || 'household update')),
      timestamp: row.logged_at || row.created_at || today
    };
  });

  var recentWorkouts = findManyBy(SHEET_NAMES.WORKOUTS, function(row) {
    return rowMatchesHousehold(row, householdId) && row.logged_at;
  }).sort(function(a, b) {
    return String(b.logged_at).localeCompare(String(a.logged_at));
  }).slice(0, 2).map(function(row) {
    return {
      title: 'Workout logged',
      description: (row.workout_type || 'Workout') + ' - ' + toInt(row.duration_minutes) + ' min',
      type: 'workout',
      timestamp: row.logged_at || today
    };
  });

  feed = recentTransactions.concat(recentTasks, paidBills, recentLogs, recentWorkouts);
  feed.sort(function(a, b) {
    return String(b.timestamp).localeCompare(String(a.timestamp));
  });

  return {
    entries: feed.slice(0, 10)
  };
}

function buildImportantReminders(householdId, today) {
  var reminders = [];

  var upcomingBills = findManyBy(SHEET_NAMES.BILLS, function(row) {
    return row.status === 'pending' && rowMatchesHousehold(row, householdId) && row.due_date && row.due_date <= addDays(today, 7) && row.due_date > today;
  });
  if (upcomingBills.length > 0) {
    reminders.push({
      title: 'Upcoming bills',
      description: upcomingBills.length + ' bill' + (upcomingBills.length > 1 ? 's' : '') + ' due soon',
      priority: 'high'
    });
  }

  var overdueTasks = findManyBy(SHEET_NAMES.TASKS, function(row) {
    return row.status !== 'completed' && row.status !== 'archived' && rowMatchesHousehold(row, householdId) && row.due_date && row.due_date < today;
  });
  if (overdueTasks.length > 0) {
    reminders.push({
      title: 'Overdue tasks',
      description: overdueTasks.length + ' task' + (overdueTasks.length > 1 ? 's' : '') + ' overdue',
      priority: 'high'
    });
  }

  var renewingSubs = findManyBy(SHEET_NAMES.SUBSCRIPTIONS, function(row) {
    return rowMatchesHousehold(row, householdId) && toBoolean(row.is_active) && row.next_renewal_date && row.next_renewal_date <= addDays(today, 7) && row.next_renewal_date > today;
  });
  if (renewingSubs.length > 0) {
    reminders.push({
      title: 'Subscriptions renewing',
      description: renewingSubs.length + ' subscription' + (renewingSubs.length > 1 ? 's' : '') + ' renewing soon',
      priority: 'medium'
    });
  }

  return reminders;
}

function compareMaintenanceByDueDate(a, b) {
  return String(a.dueDate).localeCompare(String(b.dueDate));
}

function compareTasksForDashboard(a, b) {
  if (a.priority === b.priority) {
    return String(a.title).localeCompare(String(b.title));
  }
  return String(a.priority).localeCompare(String(b.priority));
}
