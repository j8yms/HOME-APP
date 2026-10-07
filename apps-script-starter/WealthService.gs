// Wealth module: investments, assets, liabilities, milestones, passive
// income, trading accounts, trades, contributions, prop accounts/payouts,
// financial freedom settings and net worth history.
//
// Every section is stored in its own sheet (see SHEET_NAMES) and exposed
// through generic wealth/list | wealth/save | wealth/delete routes using a
// single flat WealthItem shape so Android renders one model per section.

var WEALTH_SECTIONS = {
  investments: { sheet: SHEET_NAMES.INVESTMENTS, prefix: 'inv', idKey: 'investment_id', nameKey: 'name' },
  assets: { sheet: SHEET_NAMES.ASSETS, prefix: 'ast', idKey: 'asset_id', nameKey: 'name' },
  liabilities: { sheet: SHEET_NAMES.LIABILITIES, prefix: 'lia', idKey: 'liability_id', nameKey: 'name' },
  milestones: { sheet: SHEET_NAMES.WEALTH_MILESTONES, prefix: 'mil', idKey: 'milestone_id', nameKey: 'title' },
  income: { sheet: SHEET_NAMES.PASSIVE_INCOME, prefix: 'pin', idKey: 'income_id', nameKey: 'name' },
  accounts: { sheet: SHEET_NAMES.INVESTMENT_ACCOUNTS, prefix: 'iacc', idKey: 'account_id', nameKey: 'account_name' },
  trades: { sheet: SHEET_NAMES.TRADE_JOURNAL, prefix: 'trd', idKey: 'trade_id', nameKey: 'symbol' },
  contributions: { sheet: SHEET_NAMES.INVESTMENT_TRANSACTIONS, prefix: 'itx', idKey: 'transaction_id', nameKey: 'investment_id' },
  propAccounts: { sheet: SHEET_NAMES.PROP_ACCOUNTS, prefix: 'pac', idKey: 'account_id', nameKey: 'name' },
  propPayouts: { sheet: SHEET_NAMES.PROP_PAYOUTS, prefix: 'ppo', idKey: 'payout_id', nameKey: 'account_id' }
};

var WEALTH_TRANSACTION_TYPES = ['contribution', 'withdrawal', 'return', 'fee'];

function requireWealthSection(sectionName) {
  var section = WEALTH_SECTIONS[sectionName];
  if (!section) {
    throw new Error('Unknown wealth section: ' + sectionName + '. Valid sections: ' + Object.keys(WEALTH_SECTIONS).join(', ') + '.');
  }
  return section;
}

function mapWealthItem(sectionName, row) {
  var item = {
    id: '',
    section: sectionName,
    name: '',
    type: '',
    provider: '',
    reference: '',
    value: 0,
    startingValue: 0,
    targetValue: 0,
    monthlyAmount: 0,
    yieldPct: 0,
    interestRate: 0,
    status: '',
    notes: '',
    date: '',
    currency: '',
    achieved: false,
    targetDate: '',
    isActive: true,
    investmentId: '',
    transactionType: '',
    symbol: '',
    action: '',
    quantity: 0,
    price: 0,
    entryPrice: 0,
    stopLoss: 0,
    takeProfit: 0,
    exitPrice: 0,
    pnl: 0,
    fees: 0,
    strategy: '',
    accountId: '',
    createdAt: '',
    updatedAt: ''
  };

  if (!row) {
    return item;
  }

  var section = WEALTH_SECTIONS[sectionName];
  item.id = row[section.idKey] || '';
  item.createdAt = row.created_at || '';
  item.updatedAt = row.updated_at || '';
  item.notes = row.notes || '';

  switch (sectionName) {
    case 'investments':
      item.name = row.name || '';
      item.type = row.type || '';
      item.provider = row.provider || '';
      item.reference = row.reference || '';
      item.startingValue = Number(row.starting_balance) || 0;
      item.value = Number(row.current_value) || 0;
      item.targetValue = Number(row.amount_invested) || 0;
      item.yieldPct = Number(row.yield_pct) || 0;
      break;
    case 'assets':
      item.name = row.name || '';
      item.type = row.category || '';
      item.value = Number(row.value) || 0;
      break;
    case 'liabilities':
      item.name = row.name || '';
      item.type = row.category || '';
      item.value = Number(row.amount_owed) || 0;
      item.interestRate = Number(row.interest_rate) || 0;
      item.monthlyAmount = Number(row.minimum_payment) || 0;
      break;
    case 'milestones':
      item.name = row.title || '';
      item.targetValue = Number(row.target_amount) || 0;
      item.value = Number(row.current_amount) || 0;
      item.monthlyAmount = Number(row.monthly_contribution) || 0;
      item.targetDate = row.target_date || '';
      item.achieved = toBoolean(row.achieved);
      item.date = row.achieved_at || '';
      break;
    case 'income':
      item.name = row.name || '';
      item.type = row.source_type || '';
      item.value = Number(row.amount_monthly) || 0;
      item.status = row.status || 'active';
      break;
    case 'accounts':
      item.name = row.account_name || '';
      item.type = row.account_type || '';
      item.value = Number(row.balance) || 0;
      item.currency = row.currency || '';
      break;
    case 'trades':
      item.symbol = row.symbol || '';
      item.name = row.symbol || '';
      item.action = row.action || '';
      item.type = row.action || '';
      item.quantity = Number(row.quantity) || 0;
      item.price = Number(row.price) || 0;
      item.entryPrice = Number(row.entry_price) || 0;
      item.stopLoss = Number(row.stop_loss) || 0;
      item.takeProfit = Number(row.take_profit) || 0;
      item.exitPrice = Number(row.exit_price) || 0;
      item.pnl = Number(row.pnl) || 0;
      item.fees = Number(row.fees) || 0;
      item.strategy = row.strategy || '';
      item.status = row.status || 'open';
      item.date = row.closed_at || '';
      break;
    case 'contributions':
      item.investmentId = row.investment_id || '';
      item.name = row.investment_id || '';
      item.transactionType = row.type || '';
      item.value = Number(row.amount) || 0;
      item.date = row.occurred_at || '';
      break;
    case 'propAccounts':
      item.name = row.name || '';
      item.provider = row.provider || '';
      item.value = Number(row.balance) || 0;
      item.currency = row.currency || '';
      item.status = row.status || 'active';
      break;
    case 'propPayouts':
      item.accountId = row.account_id || '';
      item.name = row.account_id || '';
      item.value = Number(row.amount) || 0;
      item.currency = row.currency || '';
      item.status = row.status || 'paid';
      item.date = row.paid_at || '';
      break;
    default:
      break;
  }

  return item;
}

function wealthItemToRow(sectionName, item) {
  var section = WEALTH_SECTIONS[sectionName];
  var row = {};
  row[section.idKey] = normalizeString(item.id);
  row.created_at = normalizeString(item.createdAt) || nowIso();
  row.updated_at = nowIso();
  row.notes = normalizeString(item.notes);

  switch (sectionName) {
    case 'investments':
      row.name = normalizeString(item.name);
      row.type = normalizeString(item.type);
      row.provider = normalizeString(item.provider);
      row.reference = normalizeString(item.reference);
      row.starting_balance = Number(item.startingValue) || 0;
      row.current_value = Number(item.value) || 0;
      row.amount_invested = Number(item.targetValue) || 0;
      row.yield_pct = Number(item.yieldPct) || 0;
      break;
    case 'assets':
      row.name = normalizeString(item.name);
      row.category = normalizeString(item.type);
      row.value = Number(item.value) || 0;
      break;
    case 'liabilities':
      row.name = normalizeString(item.name);
      row.category = normalizeString(item.type);
      row.amount_owed = Number(item.value) || 0;
      row.interest_rate = Number(item.interestRate) || 0;
      row.minimum_payment = Number(item.monthlyAmount) || 0;
      break;
    case 'milestones':
      row.title = normalizeString(item.name);
      row.target_amount = Number(item.targetValue) || 0;
      row.current_amount = Number(item.value) || 0;
      row.monthly_contribution = Number(item.monthlyAmount) || 0;
      row.target_date = normalizeString(item.targetDate);
      row.achieved = String(Boolean(item.achieved));
      row.achieved_at = item.achieved ? (normalizeString(item.date) || nowIso()) : '';
      break;
    case 'income':
      row.name = normalizeString(item.name);
      row.source_type = normalizeString(item.type);
      row.amount_monthly = Number(item.value) || 0;
      row.status = normalizeString(item.status) || 'active';
      break;
    case 'accounts':
      row.account_name = normalizeString(item.name);
      row.account_type = normalizeString(item.type);
      row.balance = Number(item.value) || 0;
      row.currency = normalizeString(item.currency);
      break;
    case 'trades':
      row.symbol = normalizeString(item.symbol || item.name);
      row.action = normalizeString(item.action || item.type);
      row.quantity = Number(item.quantity) || 0;
      row.price = Number(item.price) || 0;
      row.entry_price = Number(item.entryPrice) || 0;
      row.stop_loss = Number(item.stopLoss) || 0;
      row.take_profit = Number(item.takeProfit) || 0;
      row.exit_price = Number(item.exitPrice) || 0;
      row.pnl = Number(item.pnl) || 0;
      row.fees = Number(item.fees) || 0;
      row.strategy = normalizeString(item.strategy);
      row.status = normalizeString(item.status) || 'open';
      row.closed_at = normalizeString(item.date);
      break;
    case 'contributions':
      row.investment_id = normalizeString(item.investmentId);
      row.type = normalizeString(item.transactionType);
      row.amount = Number(item.value) || 0;
      row.occurred_at = normalizeString(item.date) || nowIso();
      break;
    case 'propAccounts':
      row.name = normalizeString(item.name);
      row.provider = normalizeString(item.provider);
      row.balance = Number(item.value) || 0;
      row.currency = normalizeString(item.currency);
      row.status = normalizeString(item.status) || 'active';
      break;
    case 'propPayouts':
      row.account_id = normalizeString(item.accountId);
      row.amount = Number(item.value) || 0;
      row.currency = normalizeString(item.currency);
      row.status = normalizeString(item.status) || 'paid';
      row.paid_at = normalizeString(item.date) || nowIso();
      break;
    default:
      break;
  }

  return row;
}

function validateWealthItem(sectionName, item) {
  if (sectionName === 'contributions') {
    if (!normalizeString(item.investmentId)) {
      throw new Error('investmentId is required.');
    }
    if (!(Number(item.value) > 0)) {
      throw new Error('amount must be a positive number.');
    }
    if (WEALTH_TRANSACTION_TYPES.indexOf(normalizeString(item.transactionType)) < 0) {
      throw new Error('transactionType must be one of: ' + WEALTH_TRANSACTION_TYPES.join(', ') + '.');
    }
    return;
  }

  if (sectionName === 'trades') {
    if (!normalizeString(item.symbol || item.name)) {
      throw new Error('symbol is required.');
    }
    if (!(Number(item.quantity) > 0)) {
      throw new Error('quantity must be a positive number.');
    }
    return;
  }

  if (sectionName === 'propPayouts') {
    if (!normalizeString(item.accountId)) {
      throw new Error('accountId is required.');
    }
    if (!(Number(item.value) > 0)) {
      throw new Error('amount must be a positive number.');
    }
    return;
  }

  if (!normalizeString(item.name)) {
    throw new Error('name is required.');
  }
}

function listWealthSection(payload) {
  var sectionName = normalizeString(payload.section);
  var section = requireWealthSection(sectionName);
  var userId = normalizeString(payload.userId);
  var householdId = getUserHouseholdId(userId);

  var rows = findManyBy(section.sheet, function(row) {
    return rowMatchesHousehold(row, householdId);
  });

  if (sectionName === 'contributions' && normalizeString(payload.investmentId)) {
    var investmentId = normalizeString(payload.investmentId);
    rows = rows.filter(function(row) {
      return row.investment_id === investmentId;
    });
  }

  rows.sort(function(a, b) {
    var aTime = String(a.updated_at || a.created_at || '');
    var bTime = String(b.updated_at || b.created_at || '');
    if (aTime === bTime) {
      var aName = String(a[section.nameKey] || '');
      var bName = String(b[section.nameKey] || '');
      return aName.localeCompare(bName);
    }
    return bTime.localeCompare(aTime);
  });

  return {
    section: sectionName,
    items: rows.map(function(row) {
      return mapWealthItem(sectionName, row);
    })
  };
}

function saveWealthItem(payload) {
  var sectionName = normalizeString(payload.section);
  var section = requireWealthSection(sectionName);
  var item = payload.item || {};
  var userId = normalizeString(payload.userId);
  var householdId = getUserHouseholdId(userId);

  validateWealthItem(sectionName, item);

  var existing = null;
  var itemId = normalizeString(item.id);
  if (itemId) {
    existing = findOneBy(section.sheet, function(row) {
      return row[section.idKey] === itemId;
    });
    if (!existing) {
      throw new Error('Wealth item not found.');
    }
  }

  if (sectionName === 'contributions' && existing) {
    // Reversing an old contribution before applying the new values keeps the
    // investment totals consistent when the entry is edited.
    applyInvestmentTransaction(existing.investment_id, existing.type, -(Number(existing.amount) || 0));
  }

  var row = wealthItemToRow(sectionName, item);
  row.household_id = householdId;
  row.user_id = userId;

  if (existing) {
    row[section.idKey] = existing[section.idKey];
    row.created_at = existing.created_at || row.created_at;
    // Merge so any sheet column outside the mapped fields keeps its value.
    var merged = Object.assign({}, existing, row);
    updateRowByIndex(section.sheet, existing.__rowIndex, merged);
  } else {
    row[section.idKey] = generateId(section.prefix, section.sheet);
    appendRow(section.sheet, row);
    item.id = row[section.idKey];
  }

  if (sectionName === 'contributions') {
    applyInvestmentTransaction(row.investment_id, row.type, Number(row.amount) || 0);
  }

  var saved = findOneBy(section.sheet, function(candidate) {
    return candidate[section.idKey] === row[section.idKey];
  });

  return {
    item: mapWealthItem(sectionName, saved || row)
  };
}

function deleteWealthItem(payload) {
  var sectionName = normalizeString(payload.section);
  var section = requireWealthSection(sectionName);
  var itemId = normalizeString(payload.id);

  if (!itemId) {
    throw new Error('id is required.');
  }

  var row = findOneBy(section.sheet, function(candidate) {
    return candidate[section.idKey] === itemId;
  });
  if (!row) {
    throw new Error('Wealth item not found.');
  }

  if (sectionName === 'contributions') {
    applyInvestmentTransaction(row.investment_id, row.type, -(Number(row.amount) || 0));
  }

  getSheet(section.sheet).deleteRow(row.__rowIndex);
  invalidateSheetCache(section.sheet);

  return {
    id: itemId,
    section: sectionName,
    deleted: true
  };
}

// Investment totals follow simple, explicit rules:
//   contribution -> invested and current value both rise
//   withdrawal   -> current value falls (invested stays as cost basis)
//   return       -> current value rises (gain)
//   fee          -> current value falls
function applyInvestmentTransaction(investmentId, transactionType, amount) {
  if (!investmentId || !amount) {
    return;
  }

  var investment = findOneBy(SHEET_NAMES.INVESTMENTS, function(row) {
    return row.investment_id === investmentId;
  });
  if (!investment) {
    return;
  }

  var currentValue = Number(investment.current_value) || 0;
  var invested = Number(investment.amount_invested) || 0;

  switch (normalizeString(transactionType)) {
    case 'contribution':
      invested += amount;
      currentValue += amount;
      break;
    case 'withdrawal':
      currentValue = Math.max(0, currentValue - amount);
      break;
    case 'return':
      currentValue += amount;
      break;
    case 'fee':
      currentValue = Math.max(0, currentValue - amount);
      break;
    default:
      return;
  }

  investment.current_value = Math.round(currentValue * 100) / 100;
  investment.amount_invested = Math.round(invested * 100) / 100;
  investment.updated_at = nowIso();
  updateRowByIndex(SHEET_NAMES.INVESTMENTS, investment.__rowIndex, investment);
}

function sumWealthValues(sheetName, valueGetter, householdId) {
  var total = 0;
  findManyBy(sheetName, function(row) {
    return rowMatchesHousehold(row, householdId);
  }).forEach(function(row) {
    total += valueGetter(row) || 0;
  });
  return Math.round(total * 100) / 100;
}

function countWealthRows(sheetName, householdId) {
  return findManyBy(sheetName, function(row) {
    return rowMatchesHousehold(row, householdId);
  }).length;
}

function computePassiveMonthly(householdId) {
  return sumWealthValues(SHEET_NAMES.PASSIVE_INCOME, function(row) {
    var status = normalizeString(row.status || 'active').toLowerCase();
    if (status === 'inactive' || status === 'paused') {
      return 0;
    }
    return Number(row.amount_monthly) || 0;
  }, householdId);
}

function getFinancialFreedom(payload) {
  var raw = getConfigString('financial_freedom', '');
  var settings = {
    monthlyRequirement: 0,
    annualRequirement: 0,
    targetPassiveIncome: 0,
    targetNetWorth: 0,
    targetDate: '',
    monthlyContribution: 0,
    updatedAt: ''
  };

  if (raw) {
    try {
      var parsed = JSON.parse(raw);
      Object.keys(settings).forEach(function(key) {
        if (parsed[key] !== undefined && parsed[key] !== null) {
          settings[key] = parsed[key];
        }
      });
    } catch (e) {
      // Corrupt config: fall back to defaults rather than failing the screen.
    }
  }

  var householdId = normalizeString(payload.householdId);
  var currentPassive = computePassiveMonthly(householdId);

  return {
    monthlyRequirement: Number(settings.monthlyRequirement) || 0,
    annualRequirement: Number(settings.annualRequirement) || 0,
    targetPassiveIncome: Number(settings.targetPassiveIncome) || 0,
    targetNetWorth: Number(settings.targetNetWorth) || 0,
    targetDate: String(settings.targetDate || ''),
    monthlyContribution: Number(settings.monthlyContribution) || 0,
    currentPassiveIncome: currentPassive,
    progressPct: Number(settings.targetPassiveIncome) > 0
      ? Math.min(100, Math.round((currentPassive / Number(settings.targetPassiveIncome)) * 100))
      : 0,
    updatedAt: String(settings.updatedAt || '')
  };
}

function saveFinancialFreedom(payload) {
  var current = getFinancialFreedom({});
  var settings = {
    monthlyRequirement: payload.monthlyRequirement !== undefined ? Number(payload.monthlyRequirement) || 0 : current.monthlyRequirement,
    annualRequirement: payload.annualRequirement !== undefined ? Number(payload.annualRequirement) || 0 : current.annualRequirement,
    targetPassiveIncome: payload.targetPassiveIncome !== undefined ? Number(payload.targetPassiveIncome) || 0 : current.targetPassiveIncome,
    targetNetWorth: payload.targetNetWorth !== undefined ? Number(payload.targetNetWorth) || 0 : current.targetNetWorth,
    targetDate: payload.targetDate !== undefined ? normalizeString(payload.targetDate) : current.targetDate,
    monthlyContribution: payload.monthlyContribution !== undefined ? Number(payload.monthlyContribution) || 0 : current.monthlyContribution,
    updatedAt: nowIso()
  };

  if (settings.monthlyRequirement < 0 || settings.annualRequirement < 0 ||
      settings.targetPassiveIncome < 0 || settings.targetNetWorth < 0) {
    throw new Error('Financial freedom targets must be non-negative numbers.');
  }

  upsertConfig('financial_freedom', JSON.stringify(settings));
  return getFinancialFreedom({});
}

function computeWealthTotals(householdId) {
  var cashSummary = getFinanceSummaryByHousehold(householdId);

  var assets = sumWealthValues(SHEET_NAMES.ASSETS, function(row) {
    return Number(row.value) || 0;
  }, householdId);

  var investmentsValue = sumWealthValues(SHEET_NAMES.INVESTMENTS, function(row) {
    return Number(row.current_value) || 0;
  }, householdId);

  var invested = sumWealthValues(SHEET_NAMES.INVESTMENTS, function(row) {
    return Number(row.amount_invested) || 0;
  }, householdId);

  var liabilities = sumWealthValues(SHEET_NAMES.LIABILITIES, function(row) {
    return Number(row.amount_owed) || 0;
  }, householdId);

  var cash = Math.round(((cashSummary.balance || 0) + (cashSummary.savings || 0)) * 100) / 100;
  var netWorth = Math.round((cash + assets + investmentsValue - liabilities) * 100) / 100;

  return {
    cash: cash,
    assets: assets,
    investments: investmentsValue,
    invested: invested,
    liabilities: liabilities,
    netWorth: netWorth
  };
}

function getWealthSummary(payload) {
  var userId = normalizeString(payload.userId);
  var householdId = getUserHouseholdId(userId);
  var totals = computeWealthTotals(householdId);
  var freedom = getFinancialFreedom({ householdId: householdId });

  var passiveMonthly = computePassiveMonthly(householdId);

  var milestoneRows = findManyBy(SHEET_NAMES.WEALTH_MILESTONES, function(row) {
    return rowMatchesHousehold(row, householdId);
  });
  var achievedMilestones = milestoneRows.filter(function(row) {
    return toBoolean(row.achieved);
  }).length;

  var snapshots = listWealthHistory({ householdId: householdId, limit: 2 });
  var monthlyChange = 0;
  var monthlyChangePct = 0;
  if (snapshots.length >= 2) {
    monthlyChange = Math.round((snapshots[0].netWorth - snapshots[1].netWorth) * 100) / 100;
    if (snapshots[1].netWorth !== 0) {
      monthlyChangePct = Math.round((monthlyChange / Math.abs(snapshots[1].netWorth)) * 1000) / 10;
    }
  }

  recordNetWorthSnapshotIfStale(householdId, totals);

  var targetPassive = Number(freedom.targetPassiveIncome) || 0;
  var targetNetWorth = Number(freedom.targetNetWorth) || 0;

  return {
    currency: getConfigString('currency', 'USD'),
    totals: {
      cash: totals.cash,
      assets: totals.assets,
      investments: totals.investments,
      invested: totals.invested,
      gainLoss: Math.round((totals.investments - totals.invested) * 100) / 100,
      liabilities: totals.liabilities,
      netWorth: totals.netWorth,
      passiveMonthly: Math.round(passiveMonthly * 100) / 100,
      targetPassive: targetPassive,
      passiveProgressPct: targetPassive > 0 ? Math.min(100, Math.round((passiveMonthly / targetPassive) * 100)) : 0,
      targetNetWorth: targetNetWorth,
      netWorthProgressPct: targetNetWorth > 0 ? Math.min(100, Math.round((totals.netWorth / targetNetWorth) * 100)) : 0,
      monthlyChange: monthlyChange,
      monthlyChangePct: monthlyChangePct
    },
    counts: {
      investments: countWealthRows(SHEET_NAMES.INVESTMENTS, householdId),
      assets: countWealthRows(SHEET_NAMES.ASSETS, householdId),
      liabilities: countWealthRows(SHEET_NAMES.LIABILITIES, householdId),
      milestones: milestoneRows.length,
      accounts: countWealthRows(SHEET_NAMES.INVESTMENT_ACCOUNTS, householdId),
      trades: countWealthRows(SHEET_NAMES.TRADE_JOURNAL, householdId),
      income: countWealthRows(SHEET_NAMES.PASSIVE_INCOME, householdId),
      contributions: countWealthRows(SHEET_NAMES.INVESTMENT_TRANSACTIONS, householdId),
      propAccounts: countWealthRows(SHEET_NAMES.PROP_ACCOUNTS, householdId),
      propPayouts: countWealthRows(SHEET_NAMES.PROP_PAYOUTS, householdId)
    },
    milestones: {
      total: milestoneRows.length,
      achieved: achievedMilestones
    },
    freedom: freedom
  };
}

function listWealthHistory(payload) {
  var householdId = normalizeString(payload.householdId);
  if (!householdId && payload.userId) {
    householdId = getUserHouseholdId(normalizeString(payload.userId));
  }
  var limit = Math.min(toInt(payload.limit, 24) || 24, 120);

  return findManyBy(SHEET_NAMES.NET_WORTH_SNAPSHOTS, function(row) {
    return rowMatchesHousehold(row, householdId);
  }).sort(function(a, b) {
    return String(b.snapshot_date).localeCompare(String(a.snapshot_date));
  }).slice(0, limit).map(function(row) {
    return {
      snapshotId: row.snapshot_id,
      netWorth: Number(row.net_worth) || 0,
      totalAssets: Number(row.total_assets) || 0,
      totalLiabilities: Number(row.total_liabilities) || 0,
      snapshotDate: row.snapshot_date || row.created_at || ''
    };
  });
}

// One truthful snapshot per day. Called from wealth/summary so the history
// chart has data without a separate write call from the client.
function recordNetWorthSnapshotIfStale(householdId, totals) {
  var todayPrefix = todayDateString();
  var existingToday = findOneBy(SHEET_NAMES.NET_WORTH_SNAPSHOTS, function(row) {
    return rowMatchesHousehold(row, householdId) && String(row.snapshot_date).substring(0, 10) === todayPrefix;
  });
  if (existingToday) {
    return;
  }

  appendRow(SHEET_NAMES.NET_WORTH_SNAPSHOTS, {
    snapshot_id: generateId('nw', SHEET_NAMES.NET_WORTH_SNAPSHOTS),
    household_id: householdId,
    total_assets: Math.round(((totals.assets || 0) + (totals.investments || 0) + (totals.cash || 0)) * 100) / 100,
    total_liabilities: totals.liabilities,
    net_worth: totals.netWorth,
    snapshot_date: nowIso(),
    created_at: nowIso()
  });
}
