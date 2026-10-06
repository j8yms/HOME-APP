var FINANCE_TYPES = ['income', 'expense', 'savings_deposit', 'savings_withdraw'];
var FINANCE_WALLETS = ['joint', 'his', 'hers'];
var SUPPORTED_CURRENCIES = ['USD', 'EUR', 'GBP', 'KES', 'NGN', 'CAD', 'AUD', 'JPY'];

function getFinanceSummary(payload) {
  var transactions = getSheetData(SHEET_NAMES.MONEY_TRANSACTIONS);
  var bills = findManyBy(SHEET_NAMES.BILLS, function() {
    return true;
  }).map(mapBill);
  var items = findManyBy(SHEET_NAMES.SHOPPING_ITEMS, function() {
    return true;
  }).map(mapShoppingItem);

  var balance = 0;
  var savings = 0;
  var wallets = { joint: 0, his: 0, hers: 0 };
  transactions.forEach(function(transaction) {
    var type = normalizeString(transaction.type);
    var amount = Number(transaction.amount) || 0;
    if (type === 'income') {
      balance += amount;
      wallets[normalizeWallet(transaction.wallet)] += amount;
    } else if (type === 'expense') {
      balance -= amount;
      wallets[normalizeWallet(transaction.wallet)] -= amount;
    } else if (type === 'savings_deposit') {
      balance -= amount;
      savings += amount;
    } else if (type === 'savings_withdraw') {
      balance += amount;
      savings -= amount;
    }
  });

  var mapped = transactions.map(mapMoneyTransaction);
  mapped.sort(function(a, b) {
    return String(b.createdAt).localeCompare(String(a.createdAt));
  });

  var today = todayDateString();
  var openBills = bills.filter(function(bill) {
    return bill.status === 'pending' && bill.dueDate >= today;
  }).sort(compareBillsByDueDate);
  var overdueBills = bills.filter(function(bill) {
    return bill.status === 'pending' && bill.dueDate && bill.dueDate < today;
  }).sort(compareBillsByDueDate);
  var openShoppingItems = items.filter(function(item) {
    return item.status === 'open';
  }).sort(function(a, b) {
    return String(b.createdAt).localeCompare(String(a.createdAt));
  });

  return {
    balance: Math.round(balance * 100) / 100,
    savings: Math.round(savings * 100) / 100,
    wallets: {
      joint: Math.round(wallets.joint * 100) / 100,
      his: Math.round(wallets.his * 100) / 100,
      hers: Math.round(wallets.hers * 100) / 100
    },
    goals: {
      vacationGoal: getConfigNumber('vacation_goal', 2000),
      dreamGoal: getConfigNumber('dream_goal', 10000)
    },
    currency: getConfigString('currency', 'USD'),
    recentTransactions: mapped.slice(0, 15),
    openBills: openBills,
    overdueBills: overdueBills,
    openShoppingItems: openShoppingItems
  };
}

function getFinanceSummaryByHousehold(householdId) {
  var transactions = getSheetData(SHEET_NAMES.MONEY_TRANSACTIONS);
  var bills = findManyBy(SHEET_NAMES.BILLS, function(row) {
    return row.household_id === householdId;
  }).map(mapBill);
  var items = findManyBy(SHEET_NAMES.SHOPPING_ITEMS, function(row) {
    return row.household_id === householdId;
  }).map(mapShoppingItem);

  var balance = 0;
  var savings = 0;
  var wallets = { joint: 0, his: 0, hers: 0 };
  transactions.forEach(function(transaction) {
    var type = normalizeString(transaction.type);
    var amount = Number(transaction.amount) || 0;
    if (type === 'income') {
      balance += amount;
      wallets[normalizeWallet(transaction.wallet)] += amount;
    } else if (type === 'expense') {
      balance -= amount;
      wallets[normalizeWallet(transaction.wallet)] -= amount;
    } else if (type === 'savings_deposit') {
      balance -= amount;
      savings += amount;
    } else if (type === 'savings_withdraw') {
      balance += amount;
      savings -= amount;
    }
  });

  var mapped = transactions
    .filter(function(t) { return t.household_id === householdId; })
    .map(mapMoneyTransaction);
  mapped.sort(function(a, b) {
    return String(b.createdAt).localeCompare(String(a.createdAt));
  });

  var today = todayDateString();
  var openBills = bills.filter(function(bill) {
    return bill.status === 'pending' && bill.dueDate >= today;
  }).sort(compareBillsByDueDate);
  var overdueBills = bills.filter(function(bill) {
    return bill.status === 'pending' && bill.dueDate && bill.dueDate < today;
  }).sort(compareBillsByDueDate);
  var openShoppingItems = items.filter(function(item) {
    return item.status === 'open';
  }).sort(function(a, b) {
    return String(b.createdAt).localeCompare(String(a.createdAt));
  });

  return {
    balance: Math.round(balance * 100) / 100,
    savings: Math.round(savings * 100) / 100,
    wallets: {
      joint: Math.round(wallets.joint * 100) / 100,
      his: Math.round(wallets.his * 100) / 100,
      hers: Math.round(wallets.hers * 100) / 100
    },
    goals: {
      vacationGoal: getConfigNumber('vacation_goal', 2000),
      dreamGoal: getConfigNumber('dream_goal', 10000)
    },
    currency: getConfigString('currency', 'USD'),
    recentTransactions: mapped.slice(0, 15),
    openBills: openBills,
    overdueBills: overdueBills,
    openShoppingItems: openShoppingItems
  };
}

function getConfigBoolean(key, defaultValue) {
  var value = getConfigString(key, '');
  if (value === 'true') return true;
  if (value === 'false') return false;
  return defaultValue;
}

function setFinanceGoals(payload) {
  var vacationGoal = Number(payload.vacationGoal);
  var dreamGoal = Number(payload.dreamGoal);

  if (isNaN(vacationGoal) || vacationGoal < 0) {
    throw new Error('vacationGoal must be a non-negative number.');
  }
  if (isNaN(dreamGoal) || dreamGoal < 0) {
    throw new Error('dreamGoal must be a non-negative number.');
  }

  upsertConfig('vacation_goal', vacationGoal);
  upsertConfig('dream_goal', dreamGoal);

  return {
    vacationGoal: getConfigNumber('vacation_goal', 2000),
    dreamGoal: getConfigNumber('dream_goal', 10000)
  };
}

function setFinanceCurrency(payload) {
  var currency = normalizeCurrency(payload.currency);
  upsertConfig('currency', currency);
  return {
    currency: getConfigString('currency', 'USD')
  };
}

function addTransaction(payload) {
  var requestId = payload.requestId || generateId('txn', SHEET_NAMES.MONEY_TRANSACTIONS);
  var type = normalizeString(payload.type);
  var description = normalizeString(payload.description);
  var category = normalizeString(payload.category);
  var amount = Number(payload.amount);
  var userId = normalizeString(payload.userId);
  var wallet = normalizeWallet(payload.wallet);
  var householdId = getUserHouseholdId(userId);

  if (FINANCE_TYPES.indexOf(type) < 0) {
    throw new Error('type must be one of: income, expense, savings_deposit, savings_withdraw.');
  }
  if (!description) {
    throw new Error('description is required.');
  }
  if (!(amount > 0)) {
    throw new Error('amount must be a positive number.');
  }
  if (!householdId) {
    throw new Error('User not associated with a household.');
  }

  // Validate amount against current household balance
  var currentBalance = getHouseholdBalance(householdId);
  var expenseAllowed = true;
  
  if (type === 'expense' || type === 'savings_deposit') {
    // For expenses and savings deposits, check that balance won't go negative
    // (overdraft is allowed only if explicitly configured)
    var overdraftAllowed = getConfigBoolean('overdraft_allowed', false);
    if (!overdraftAllowed && currentBalance < amount) {
      throw new Error('Insufficient balance. You have KES ' + currentBalance + ' available. ' +
          'Request amount: KES ' + amount + '.');
    }
  }

  // Use LockService for atomic operation
  var lock = LockService.getScriptLock();
  lock.waitLock(30);  // Wait up to 30 seconds for lock

  try {
    // Check idempotency - if a transaction with this requestId already exists, skip
    var existingTransactions = findManyBy(SHEET_NAMES.MONEY_TRANSACTIONS, function(row) {
      return row.request_id === requestId;
    });
    if (existingTransactions.length > 0) {
      // Return existing transaction data to avoid duplicate
      var existing = existingTransactions[0];
      return {
        transaction: mapMoneyTransaction(existing),
        summary: getFinanceSummary({})
      };
    }

    var transaction = {
      transaction_id: generateId('m', SHEET_NAMES.MONEY_TRANSACTIONS),
      request_id: requestId,
      type: type,
      description: description,
      category: category,
      amount: amount,
      wallet: wallet,
      user_id: userId,
      household_id: householdId,
      created_at: nowIso()
    };
    appendRow(SHEET_NAMES.MONEY_TRANSACTIONS, transaction);

    // Return authoritative summary recalculated from all household transactions
    return {
      transaction: mapMoneyTransaction(transaction),
      summary: getFinanceSummaryByHousehold(householdId)
    };
  } finally {
    lock.release();
  }
}

function listBills(payload) {
  var userId = normalizeString(payload.userId);
  var householdId = getUserHouseholdId(userId);
  var filterFn = householdId ? function(row) {
    return row.household_id === householdId;
  } : function() { return true; };
  return findManyBy(SHEET_NAMES.BILLS, filterFn).map(mapBill).sort(compareBillsByDueDate);
}

function listBudgets(payload) {
  var userId = normalizeString(payload.userId);
  var householdId = getUserHouseholdId(userId);
  var budgets = getHouseholdBudgets(householdId);
  return budgets;
}

function createBill(payload) {
  var title = normalizeString(payload.title);
  var category = normalizeString(payload.category);
  var amount = Number(payload.amount);
  var dueDate = normalizeString(payload.dueDate);
  var frequency = normalizeString(payload.frequency) || 'monthly'; // weekly, monthly, quarterly, yearly, custom
  var autoPay = payload.autoPay !== undefined ? Boolean(payload.autoPay) : false;
  var createdByUserId = normalizeString(payload.createdByUserId);

  if (!title) {
    throw new Error('title is required.');
  }
  if (!(amount > 0)) {
    throw new Error('amount must be a positive number.');
  }
  if (!dueDate) {
    throw new Error('dueDate is required.');
  }

  var userHouseholdId = getUserHouseholdId(createdByUserId);
  if (!userHouseholdId) {
    throw new Error('User not associated with a household.');
  }

  var bill = {
    bill_id: generateId('b', SHEET_NAMES.BILLS),
    title: title,
    category: category,
    amount: amount,
    due_date: dueDate,
    status: 'pending',
    frequency: frequency,
    auto_pay: String(Boolean(autoPay)),
    paid_by_user_id: '',
    paid_at: '',
    created_by_user_id: createdByUserId,
    household_id: userHouseholdId,
    created_at: nowIso(),
    updated_at: nowIso()
  };
  appendRow(SHEET_NAMES.BILLS, bill);
  return mapBill(bill);
}

function updateBill(payload) {
  var billId = normalizeString(payload.billId);
  var status = normalizeString(payload.status);
  var paidByUserId = normalizeString(payload.paidByUserId);
  var userId = normalizeString(payload.userId || '');

  if (!billId) {
    throw new Error('billId is required.');
  }

  var bill = findOneBy(SHEET_NAMES.BILLS, function(row) {
    return row.bill_id === billId;
  });
  if (!bill) {
    throw new Error('Bill not found.');
  }

  // Household isolation: verify bill belongs to user's household
  var userHouseholdId = getUserHouseholdId(userId);
  if (userHouseholdId && bill.household_id !== userHouseholdId) {
    throw new Error('Bill does not belong to your household.');
  }

  // If marking as paid, create transaction and handle recurrence
  if (status === 'paid') {
    bill.status = 'paid';
    bill.paid_by_user_id = paidByUserId;
    bill.paid_at = nowIso();

    // Create a transaction for this bill payment (expense)
    var transactionType = 'expense';
    var transactionDescription = 'Bill payment: ' + bill.title;
    var transactionCategory = bill.category || 'Bills';
    var transactionWallet = 'joint'; // bills typically paid from joint account

    // Use LockService for atomic operation
    var lock = LockService.getScriptLock();
    lock.waitLock(30);

    try {
      // Check idempotency - if a transaction with this bill_id already exists for this date, skip
      var existingTransactions = findManyBy(SHEET_NAMES.MONEY_TRANSACTIONS, function(row) {
        return row.household_id === bill.household_id && row.type === 'expense' && row.description === transactionDescription && row.created_at >= bill.paid_at;
      });
      if (existingTransactions.length > 0) {
        // Already processed - just update the bill and return
        bill.updated_at = nowIso();
        updateRowByIndex(SHEET_NAMES.BILLS, bill.__rowIndex, bill);
        return mapBill(bill);
      }

      var transaction = {
        transaction_id: generateId('m', SHEET_NAMES.MONEY_TRANSACTIONS),
        type: transactionType,
        description: transactionDescription,
        category: transactionCategory,
        amount: bill.amount,
        wallet: transactionWallet,
        user_id: userId,
        household_id: bill.household_id,
        created_at: nowIso()
      };
      appendRow(SHEET_NAMES.MONEY_TRANSACTIONS, transaction);
    } finally {
      lock.release();
    }
  } else if (status === 'pending') {
    bill.status = 'pending';
    bill.paid_by_user_id = '';
    bill.paid_at = '';
  }

  // Handle recurrence: if bill has frequency and is being marked paid, generate next occurrence
  if (bill.frequency && status === 'paid') {
    generateNextBillOccurrence(bill);
  }

  bill.updated_at = nowIso();
  updateRowByIndex(SHEET_NAMES.BILLS, bill.__rowIndex, bill);
  return mapBill(bill);
}

function generateNextBillOccurrence(bill) {
  var nextDueDate = bill.due_date;
  var frequency = bill.frequency;

  // Calculate next due date based on frequency
  var today = todayDateStringByHousehold(bill.household_id);

  if (frequency === 'weekly') {
    nextDueDate = addDays(today, 7);
  } else if (frequency === 'monthly') {
    // Simplified: add one month (same day next month, or last day if current month shorter)
    var dateParts = nextDueDate.split('-');
    var year = parseInt(dateParts[0]);
    var month = parseInt(dateParts[1]);
    var day = parseInt(dateParts[2]);
    // Add one month
    month = month + 1;
    if (month > 12) {
      month = 1;
      year = year + 1;
    }
    // Adjust day if needed (handle shorter months)
    var date = new Date(Date.UTC(year, month - 1, Math.min(day, 28)));
    nextDueDate = Utilities.formatDate(date, 'UTC', 'yyyy-MM-dd');
  } else if (frequency === 'quarterly') {
    nextDueDate = addDays(today, 90);
  } else if (frequency === 'yearly') {
    var dateParts = nextDueDate.split('-');
    var year = parseInt(dateParts[0]) + 1;
    var month = parseInt(dateParts[1]);
    var day = parseInt(dateParts[2]);
    nextDueDate = year + '-' + month.padStart(2, '0') + '-' + day.padStart(2, '0');
  } else if (frequency === 'custom') {
    // Custom frequency - could be based on a custom interval
    nextDueDate = addDays(today, 30); // default to 30 days
  }

  // Create the next bill occurrence
  var nextBill = {
    bill_id: generateId('b', SHEET_NAMES.BILLS),
    title: bill.title,
    category: bill.category,
    amount: bill.amount,
    due_date: nextDueDate,
    status: 'pending',
    frequency: bill.frequency,
    auto_pay: bill.auto_pay,
    paid_by_user_id: '',
    paid_at: '',
    created_by_user_id: bill.created_by_user_id,
    household_id: bill.household_id,
    created_at: nowIso(),
    updated_at: nowIso()
  };
  appendRow(SHEET_NAMES.BILLS, nextBill);
}

function listShoppingItems(payload) {
  return findManyBy(SHEET_NAMES.SHOPPING_ITEMS, function() {
    return true;
  }).map(mapShoppingItem).sort(function(a, b) {
    if (a.status === b.status) {
      return String(b.createdAt).localeCompare(String(a.createdAt));
    }
    return a.status === 'open' ? -1 : 1;
  });
}

function createShoppingItem(payload) {
  var title = normalizeString(payload.title);
  var category = normalizeString(payload.category);
  var estimatedCost = Number(payload.estimatedCost);
  var addedByUserId = normalizeString(payload.addedByUserId);

  if (!title) {
    throw new Error('title is required.');
  }

  var item = {
    item_id: generateId('i', SHEET_NAMES.SHOPPING_ITEMS),
    title: title,
    category: category,
    estimated_cost: estimatedCost > 0 ? estimatedCost : 0,
    status: 'open',
    purchased_by_user_id: '',
    purchased_at: '',
    added_by_user_id: addedByUserId,
    created_at: nowIso(),
    updated_at: nowIso()
  };
  appendRow(SHEET_NAMES.SHOPPING_ITEMS, item);
  return mapShoppingItem(item);
}

function updateShoppingItem(payload) {
  var itemId = normalizeString(payload.itemId);
  var status = normalizeString(payload.status);
  var purchasedByUserId = normalizeString(payload.purchasedByUserId);

  if (!itemId) {
    throw new Error('itemId is required.');
  }

  var item = findOneBy(SHEET_NAMES.SHOPPING_ITEMS, function(row) {
    return row.item_id === itemId;
  });
  if (!item) {
    throw new Error('Shopping item not found.');
  }

  if (status === 'purchased') {
    item.status = 'purchased';
    item.purchased_by_user_id = purchasedByUserId;
    item.purchased_at = nowIso();
  } else if (status === 'open') {
    item.status = 'open';
    item.purchased_by_user_id = '';
    item.purchased_at = '';
  }
  item.updated_at = nowIso();
  updateRowByIndex(SHEET_NAMES.SHOPPING_ITEMS, item.__rowIndex, item);
  return mapShoppingItem(item);
}

function compareBillsByDueDate(a, b) {
  return String(a.dueDate).localeCompare(String(b.dueDate));
}

function getUserHouseholdId(userId) {
  var user = findOneBy(SHEET_NAMES.USERS, function(row) {
    return row.user_id === userId;
  });
  return user ? user.household_id : '';
}

function getHouseholdBalance(householdId) {
  var transactions = getSheetData(SHEET_NAMES.MONEY_TRANSACTIONS);
  var balance = 0;
  transactions.forEach(function(transaction) {
    if (transaction.household_id !== householdId) return;
    var type = normalizeString(transaction.type);
    var amount = Number(transaction.amount) || 0;
    if (type === 'income') {
      balance += amount;
    } else if (type === 'expense') {
      balance -= amount;
    } else if (type === 'savings_deposit') {
      balance -= amount;
    } else if (type === 'savings_withdraw') {
      balance += amount;
    }
  });
  return Math.round(balance * 100) / 100;
}

function normalizeWallet(value) {
  var wallet = normalizeString(value).toLowerCase();
  return FINANCE_WALLETS.indexOf(wallet) < 0 ? 'joint' : wallet;
}

function normalizeCurrency(value) {
  var currency = normalizeString(value).toUpperCase();
  if (SUPPORTED_CURRENCIES.indexOf(currency) < 0) {
    throw new Error('currency must be one of: ' + SUPPORTED_CURRENCIES.join(', ') + '.');
  }
  return currency;
}

function mapMoneyTransaction(row) {
  return {
    transactionId: row.transaction_id,
    type: row.type || '',
    description: row.description || '',
    category: row.category || '',
    amount: Number(row.amount) || 0,
    wallet: normalizeWallet(row.wallet),
    userId: row.user_id || '',
    createdAt: row.created_at || ''
  };
}

function mapBill(row) {
  return {
    billId: row.bill_id,
    title: row.title || '',
    category: row.category || '',
    amount: Number(row.amount) || 0,
    dueDate: row.due_date || '',
    status: row.status || 'pending',
    paidByUserId: row.paid_by_user_id || '',
    paidAt: row.paid_at || ''
  };
}

function mapShoppingItem(row) {
  return {
    itemId: row.item_id,
    title: row.title || '',
    category: row.category || '',
    estimatedCost: Number(row.estimated_cost) || 0,
    status: row.status || 'open',
    addedByUserId: row.added_by_user_id || '',
    purchasedAt: row.purchased_at || ''
  };
}

function getHouseholdBudgets(householdId) {
  var transactions = getSheetData(SHEET_NAMES.MONEY_TRANSACTIONS);
  var budgets = findManyBy(SHEET_NAMES.BUDGETS, function(row) {
    return row.household_id === householdId;
  });

  // Build a map of expenses per category per month
  var expensesByCategoryMonth = {};
  transactions.forEach(function(t) {
    if (t.household_id !== householdId) return;
    if (t.type !== 'expense') return;
    var month = t.created_at ? t.created_at.substring(0, 7) : ''; // YYYY-MM
    var key = month + '|' + t.category;
    if (!expensesByCategoryMonth[key]) {
      expensesByCategoryMonth[key] = 0;
    }
    expensesByCategoryMonth[key] += Number(t.amount) || 0;
  });

  // Map budgets with auto-calculated currentSpent
  var result = [];
  budgets.forEach(function(budget) {
    var month = budget.month;
    var category = budget.category;
    var budgetLimit = budget.budgetLimit;
    var currentSpent = 0;
    var key = month + '|' + category;
    if (expensesByCategoryMonth[key]) {
      currentSpent = expensesByCategoryMonth[key];
    }
    var progressPct = budgetLimit > 0 ? Math.round((currentSpent / budgetLimit) * 100) : 0;
    var status;
    if (progressPct >= 100) {
      status = 'OVER_BUDGET';
    } else if (progressPct >= 80) {
      status = 'WARNING';
    } else {
      status = 'SAFE';
    }
    result.push({
      budgetId: budget.budget_id,
      category: budget.category,
      month: budget.month,
      budgetLimit: budgetLimit,
      currentSpent: Math.round(currentSpent * 100) / 100,
      progressPct: progressPct,
      status: status
    });
  });
  return result;
}

function createBudget(payload) {
  var category = normalizeString(payload.category);
  var month = normalizeString(payload.month);
  var budgetLimit = Number(payload.budgetLimit);
  var createdByUserId = normalizeString(payload.createdByUserId);

  if (!category) {
    throw new Error('category is required.');
  }
  if (!month) {
    throw new Error('month is required.');
  }
  if (!(budgetLimit > 0)) {
    throw new Error('budgetLimit must be a positive number.');
  }
  if (!createdByUserId) {
    throw new Error('createdByUserId is required.');
  }

  var householdId = getUserHouseholdId(createdByUserId);
  if (!householdId) {
    throw new Error('User not associated with a household.');
  }

  var budgetId = generateId('b', SHEET_NAMES.BUDGETS);
  var budget = {
    budget_id: budgetId,
    household_id: householdId,
    category: category,
    month: month,
    budgetLimit: budgetLimit,
    created_by_user_id: createdByUserId,
    created_at: nowIso(),
    updated_at: nowIso()
  };
  appendRow(SHEET_NAMES.BUDGETS, budget);
  return budget;
}

function updateBudget(payload) {
  var budgetId = normalizeString(payload.budgetId);
  var budgetLimit = Number(payload.budgetLimit);

  if (!budgetId) {
    throw new Error('budgetId is required.');
  }
  if (!(budgetLimit > 0)) {
    throw new Error('budgetLimit must be a positive number.');
  }

  var budget = findOneBy(SHEET_NAMES.BUDGETS, function(row) {
    return row.budget_id === budgetId;
  });
  if (!budget) {
    throw new Error('Budget not found.');
  }

  // Household isolation
  var userId = normalizeString(payload.userId);
  var userHouseholdId = getUserHouseholdId(userId);
  if (userHouseholdId && budget.household_id !== userHouseholdId) {
    throw new Error('Budget does not belong to your household.');
  }

  budget.budgetLimit = budgetLimit;
  // Recalculate progress and status from the auto-calculated currentSpent
  var currentSpent = budget.currentSpent || 0;
  var progressPct = budget.budgetLimit > 0 ? Math.round((currentSpent / budget.budgetLimit) * 100) : 0;
  budget.progressPct = progressPct;
  var status;
  if (progressPct >= 100) {
    status = 'OVER_BUDGET';
  } else if (progressPct >= 80) {
    status = 'WARNING';
  } else {
    status = 'SAFE';
  }
  budget.status = status;
  budget.updated_at = nowIso();
  updateRowByIndex(SHEET_NAMES.BUDGETS, budget.__rowIndex, budget);
  return budget;
}

function createSavingsGoal(payload) {
  var name = normalizeString(payload.name);
  var targetAmount = Number(payload.targetAmount);
  var currency = normalizeString(payload.currency) || 'KES';
  var deadline = normalizeString(payload.deadline);
  var priority = normalizeString(payload.priority) || 'medium';
  var accountId = normalizeString(payload.accountId);
  var createdByUserId = normalizeString(payload.createdByUserId);

  if (!name) {
    throw new Error('name is required.');
  }
  if (!(targetAmount > 0)) {
    throw new Error('targetAmount must be a positive number.');
  }
  if (!createdByUserId) {
    throw new Error('createdByUserId is required.');
  }

  var householdId = getUserHouseholdId(createdByUserId);
  if (!householdId) {
    throw new Error('User not associated with a household.');
  }

  var goalId = generateId('g', SHEET_NAMES.SAVINGS_GOALS);
  var goal = {
    goal_id: goalId,
    household_id: householdId,
    name: name,
    targetAmount: targetAmount,
    currentAmount: 0,
    currency: currency,
    deadline: deadline,
    priority: priority,
    status: 'active',
    created_by_user_id: createdByUserId,
    created_at: nowIso(),
    updated_at: nowIso()
  };
  appendRow(SHEET_NAMES.SAVINGS_GOALS, goal);
  return goal;
}

function updateSavingsGoal(payload) {
  var goalId = normalizeString(payload.goalId);
  var currentAmount = Number(payload.currentAmount);

  if (!goalId) {
    throw new Error('goalId is required.');
  }

  var goal = findOneBy(SHEET_NAMES.SAVINGS_GOALS, function(row) {
    return row.goal_id === goalId;
  });
  if (!goal) {
    throw new Error('Savings goal not found.');
  }

  // Household isolation
  var userId = normalizeString(payload.userId);
  var userHouseholdId = getUserHouseholdId(userId);
  if (userHouseholdId && goal.household_id !== userHouseholdId) {
    throw new Error('Savings goal does not belong to your household.');
  }

  goal.currentAmount = currentAmount;
  // Update status based on progress
  if (goal.targetAmount > 0) {
    var progress = Math.round((currentAmount / goal.targetAmount) * 100);
    if (progress >= 100) {
      goal.status = 'achieved';
    } else if (progress >= 80) {
      goal.status = 'on_track';
    } else {
      goal.status = 'active';
    }
    goal.progressPct = progress;
  }
  goal.updated_at = nowIso();
  updateRowByIndex(SHEET_NAMES.SAVINGS_GOALS, goal.__rowIndex, goal);
  return goal;
}

function listSavingsGoals(payload) {
  var userId = normalizeString(payload.userId);
  var householdId = getUserHouseholdId(userId);
  var goals = findManyBy(SHEET_NAMES.SAVINGS_GOALS, function(row) {
    return row.household_id === householdId;
  }).map(mapSavingsGoal);
  return goals;
}

function mapSavingsGoal(row) {
  return {
    goalId: row.goal_id,
    name: row.name || '',
    targetAmount: row.targetAmount || 0,
    currentAmount: row.currentAmount || 0,
    currency: row.currency || 'KES',
    deadline: row.deadline || '',
    priority: row.priority || 'medium',
    status: row.status || 'active'
  };
}

// Net Worth Snapshot functions

function createNetWorthSnapshot(payload) {
  var householdId = normalizeString(payload.householdId);
  var netWorth = Number(payload.netWorth);

  if (!householdId) {
    throw new Error('householdId is required.');
  }
  if (isNaN(netWorth)) {
    throw new Error('netWorth must be a number.');
  }

  var snapshotId = generateId('nw', SHEET_NAMES.NET_WORTH_SNAPSHOTS);
  var snapshot = {
    snapshot_id: snapshotId,
    household_id: householdId,
    netWorth: netWorth,
    snapshotDate: nowIso(),
    assetBreakdown: '',
    liabilityBreakdown: ''
  };
  appendRow(SHEET_NAMES.NET_WORTH_SNAPSHOTS, snapshot);
  return snapshot;
}

function listNetWorthSnapshots(payload) {
  var householdId = normalizeString(payload.householdId);
  var snapshots = findManyBy(SHEET_NAMES.NET_WORTH_SNAPSHOTS, function(row) {
    return row.household_id === householdId;
  }).sort(function(a, b) {
    return String(b.snapshotDate).localeCompare(String(a.snapshotDate));
  });
  return snapshots.map(function(row) {
    return {
      snapshotId: row.snapshot_id,
      netWorth: row.netWorth,
      snapshotDate: row.snapshotDate,
      assetBreakdown: row.assetBreakdown || '',
      liabilityBreakdown: row.liabilityBreakdown || ''
    };
  });
}
