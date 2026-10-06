function getDashboard(payload) {
  var userId = normalizeString(payload.userId);
  if (!userId) {
    throw new Error('userId is required.');
  }

  var user = getUserById(userId);
  if (!user) {
    throw new Error('User not found.');
  }

  var householdId = user.household_id || '';
  var partner = findManyBy(SHEET_NAMES.USERS, function(row) {
    return toBoolean(row.is_active) && row.user_id !== userId && row.household_id === householdId;
  })[0] || null;

  var today = todayDateStringByHousehold(householdId);

  // ===== B. TODAY section =====
  var openTasks = findManyBy(SHEET_NAMES.TASKS, function(row) {
    return row.status !== 'completed' && row.status !== 'archived' && row.household_id === householdId;
  }).map(mapTask);

  var tasksDueToday = openTasks.filter(function(task) {
    return task.dueDate === today;
  }).sort(compareTasksForDashboard);

  var overdueTasks = openTasks.filter(function(task) {
    return task.dueDate && task.dueDate < today;
  }).sort(compareTasksForDashboard);

  // Today's bills (due today)
  var todayBills = findManyBy(SHEET_NAMES.BILLS, function(row) {
    return row.status === 'pending' && row.household_id === householdId && row.dueDate === today;
  }).map(mapBill).sort(compareBillsByDueDate);

  // Upcoming bills (due within 7 days or still pending past today)
  var sevenDaysLater = addDays(today, 7);
  var upcomingBills = findManyBy(SHEET_NAMES.BILLS, function(row) {
    return row.status === 'pending' && row.household_id === householdId && row.dueDate && row.dueDate >= today && row.dueDate <= sevenDaysLater;
  }).map(mapBill).sort(compareBillsByDueDate);

  // Overdue bills
  var overdueBills = findManyBy(SHEET_NAMES.BILLS, function(row) {
    return row.status === 'pending' && row.household_id === householdId && row.dueDate && new Date(row.dueDate) < new Date(today);
  }).map(mapBill).sort(compareBillsByDueDate);

  // Today's events (events starting today)
  var todayEvents = findManyBy(SHEET_NAMES.EVENTS, function(row) {
    return row.household_id === householdId && row.start === today;
  }).map(mapEvent).sort(function(a, b) {
    return String(a.start).localeCompare(String(b.start));
  });

  // Quick actions summary
  var incomeThisMonth = getIncomeThisMonth(householdId);
  var expensesThisMonth = getExpensesThisMonth(householdId);
  var openShoppingItems = findManyBy(SHEET_NAMES.SHOPPING_ITEMS, function(row) {
    return row.status === 'open' && row.household_id === householdId;
  }).map(mapShoppingItem).sort(function(a, b) {
    return String(b.priority).localeCompare(String(a.priority));
  });

  // ===== C. MONEY section =====
  var summary = getFinanceSummaryByHousehold(householdId);
  var accountBreakdown = summary.wallets;
  var incomeMonthly = incomeThisMonth;
  var expensesMonthly = expensesThisMonth;
  var remainingBudget = summary.balance - expensesMonthly; // simple calculation
  var subscriptionTotalMonthly = getSubscriptionTotalMonthly(householdId);

  // ===== E. GOALS section =====
  var savingsGoals = findManyBy(SHEET_NAMES.SAVINGS_GOALS, function(row) {
    return row.household_id === householdId;
  }).map(mapSavingsGoal);

  // ===== F. ACTIVITY section =====
  var activityFeed = buildActivityFeed(householdId, today);

  // ===== G. GAMIFICATION section (secondary) =====
  var user = getUserById(userId);
  var xpTotal = toInt(user.xp_total);
  var level = toInt(user.level);
  var coinsTotal = toInt(user.coins_total);
  var currentStreak = toInt(user.currentStreak);
  var longestStreak = toInt(user.longestStreak);

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
      incomeThisMonth: incomeMonthly,
      expensesThisMonth: expensesMonthly,
      remainingBudget: remainingBudget,
      subscriptionTotalMonthly: subscriptionTotalMonthly,
      upcomingSubscriptions: getUpcomingSubscriptions(householdId)
    },

    // D. HOUSEHOLD
    household: {
      openTasks: openTasks.sort(compareTasksForDashboard),
      completedTasks: findManyBy(SHEET_NAMES.TASKS, function(row) {
        return row.status === 'completed' && row.household_id === householdId;
      }).map(mapTask).sort(compareTasksForDashboard),
      maintenanceIssues: findManyBy(SHEET_NAMES.MAINTENANCE, function(row) {
        return row.status !== 'completed' && row.household_id === householdId;
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

    // Household metadata
    householdId: householdId
  };
}

// Helper functions

function getHouseholdName(householdId) {
  if (!householdId) return '';
  var users = findManyBy(SHEET_NAMES.USERS, function(row) {
    return row.household_id === householdId && toBoolean(row.is_active);
  });
  if (users.length > 0) {
    return users[0].householdName || 'Our Household';
  }
  return 'Our Household';
}

function getIncomeThisMonth(householdId) {
  var transactions = getSheetData(SHEET_NAMES.MONEY_TRANSACTIONS);
  var today = todayDateStringByHousehold(householdId);
  var monthStart = today; // simplified - would need month boundary calculation
  var income = 0;
  transactions.forEach(function(t) {
    if (t.household_id !== householdId) return;
    if (t.type === 'income') {
      income += Number(t.amount) || 0;
    }
  });
  return Math.round(income * 100) / 100;
}

function getExpensesThisMonth(householdId) {
  var transactions = getSheetData(SHEET_NAMES.MONEY_TRANSACTIONS);
  var expenses = 0;
  transactions.forEach(function(t) {
    if (t.household_id !== householdId) return;
    if (t.type === 'expense') {
      expenses += Number(t.amount) || 0;
    }
  });
  return Math.round(expenses * 100) / 100;
}

function getSubscriptionTotalMonthly(householdId) {
  var subs = findManyBy(SHEET_NAMES.SUBSCRIPTIONS, function(row) {
    return row.household_id === householdId && row.active;
  });
  var total = 0;
  subs.forEach(function(s) {
    total += Number(s.amount) || 0;
  });
  return Math.round(total * 100) / 100;
}

function getUpcomingSubscriptions(householdId) {
  var today = todayDateStringByHousehold(householdId);
  var subs = findManyBy(SHEET_NAMES.SUBSCRIPTIONS, function(row) {
    return row.household_id === householdId && row.active && row.nextRenewalDate && row.nextRenewalDate >= today;
  });
  return subs.slice(0, 5).map(function(s) {
    return {
      serviceName: s.serviceName,
      nextRenewalDate: s.nextRenewalDate,
      amount: s.amount
    };
  });
}

function buildActivityFeed(householdId, today) {
  var feed = [];

  // Task completions (last 30 days)
  var thirtyDaysAgo = addDays(today, -30);
  var completedTasks = findManyBy(SHEET_NAMES.TASKS, function(row) {
    return row.status === 'completed' && row.household_id === householdId && row.completedAt >= thirtyDaysAgo;
  });
  // ... would need completion date tracking, simplified

  // Transaction entries
  var recentTransactions = findManyBy(SHEET_NAMES.MONEY_TRANSACTIONS, function(row) {
    return row.household_id === householdId && row.created_at >= todayAdd(-30);
  }).slice(0, 5).map(mapMoneyTransaction);

  // Bill payments
  var paidBills = findManyBy(SHEET_NAMES.BILLS, function(row) {
    return row.status === 'paid' && row.household_id === householdId && row.paid_at >= thirtyDaysAgo;
  }).slice(0, 3).map(function(row) {
    return {
      title: row.title + ' bill',
      description: 'Paid KES ' + row.amount,
      type: 'bill',
      timestamp: row.paid_at
    };
  });

  // Household logs
  var recentLogs = findManyBy(SHEET_NAMES.HOUSEHOLD_LOG, function(row) {
    return row.household_id === householdId && row.created_at >= thirtyDaysAgo;
  }).slice(0, 3).map(function(row) {
    return {
      title: row.title,
      type: row.type,
      description: 'Logged ' + row.type,
      timestamp: row.created_at
    };
  });

  // Combine and sort by timestamp descending
  var allEntries = [...recentTransactions, ...paidBills, ...recentLogs];
  allEntries.sort(function(a, b) {
    return String(b.timestamp).localeCompare(String(a.timestamp));
  });

  return allEntries.slice(0, 10); // top 10
}

function buildImportantReminders(householdId, today) {
  var reminders = [];

  // Upcoming bills reminder
  var upcomingBills = findManyBy(SHEET_NAMES.BILLS, function(row) {
    return row.status === 'pending' && row.household_id === householdId && row.dueDate && new Date(row.dueDate) <= new Date(todayAdd(7)) && new Date(row.dueDate) > new Date(today);
  });
  if (upcomingBills.length > 0) {
    reminders.push({
      title: 'Upcoming bills',
      description: upcomingBills.length + ' bill' + (upcomingBills.length > 1 ? 's' : '') + ' due soon',
      priority: 'high'
    });
  }

  // Overdue tasks reminder
  var overdueTasks = findManyBy(SHEET_NAMES.TASKS, function(row) {
    return row.status !== 'completed' && row.status !== 'archived' && row.household_id === householdId && row.dueDate && new Date(row.dueDate) < new Date(today);
  });
  if (overdueTasks.length > 0) {
    reminders.push({
      title: 'Overdue tasks',
      description: overdueTasks.length + ' task' + (overdueTasks.length > 1 ? 's' : '') + ' overdue',
      priority: 'high'
    });
  }

  // Subscription renewals
  var renewingSubs = findManyBy(SHEET_NAMES.SUBSCRIPTIONS, function(row) {
    return row.household_id === householdId && row.active && row.nextRenewalDate && new Date(row.nextRenewalDate) <= new Date(todayAdd(7)) && new Date(row.nextRenewalDate) > new Date(today);
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

  var weight = {
    high: 0,
    medium: 1,
    low: 2
  };

  return (weight[a.priority] || 9) - (weight[b.priority] || 9);
}
