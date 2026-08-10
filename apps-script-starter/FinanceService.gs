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
  var type = normalizeString(payload.type);
  var description = normalizeString(payload.description);
  var category = normalizeString(payload.category);
  var amount = Number(payload.amount);
  var userId = normalizeString(payload.userId);
  var wallet = normalizeWallet(payload.wallet);

  if (FINANCE_TYPES.indexOf(type) < 0) {
    throw new Error('type must be one of: income, expense, savings_deposit, savings_withdraw.');
  }
  if (!description) {
    throw new Error('description is required.');
  }
  if (!(amount > 0)) {
    throw new Error('amount must be a positive number.');
  }

  var transaction = {
    transaction_id: generateId('m', SHEET_NAMES.MONEY_TRANSACTIONS),
    type: type,
    description: description,
    category: category,
    amount: amount,
    wallet: wallet,
    user_id: userId,
    created_at: nowIso()
  };
  appendRow(SHEET_NAMES.MONEY_TRANSACTIONS, transaction);

  return {
    transaction: mapMoneyTransaction(transaction),
    summary: getFinanceSummary({})
  };
}

function listBills(payload) {
  return findManyBy(SHEET_NAMES.BILLS, function() {
    return true;
  }).map(mapBill).sort(compareBillsByDueDate);
}

function createBill(payload) {
  var title = normalizeString(payload.title);
  var category = normalizeString(payload.category);
  var amount = Number(payload.amount);
  var dueDate = normalizeString(payload.dueDate);
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

  var bill = {
    bill_id: generateId('b', SHEET_NAMES.BILLS),
    title: title,
    category: category,
    amount: amount,
    due_date: dueDate,
    status: 'pending',
    paid_by_user_id: '',
    paid_at: '',
    created_by_user_id: createdByUserId,
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

  if (!billId) {
    throw new Error('billId is required.');
  }

  var bill = findOneBy(SHEET_NAMES.BILLS, function(row) {
    return row.bill_id === billId;
  });
  if (!bill) {
    throw new Error('Bill not found.');
  }

  if (status === 'paid') {
    bill.status = 'paid';
    bill.paid_by_user_id = paidByUserId;
    bill.paid_at = nowIso();
  } else if (status === 'pending') {
    bill.status = 'pending';
    bill.paid_by_user_id = '';
    bill.paid_at = '';
  }
  bill.updated_at = nowIso();
  updateRowByIndex(SHEET_NAMES.BILLS, bill.__rowIndex, bill);
  return mapBill(bill);
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
