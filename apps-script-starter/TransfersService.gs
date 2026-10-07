var WALLET_NAMES = ['joint', 'his', 'hers'];
var GOAL_TARGETS = ['vacation', 'dream'];

function mapTransfer(row) {
  return {
    transferId: row.transfer_id,
    sourceAccount: row.source_account || '',
    destinationTarget: row.destination_target || '',
    amount: Number(row.amount) || 0,
    direction: row.direction || 'out',
    timestamp: row.created_at || ''
  };
}

function isWalletAccount(name) {
  return WALLET_NAMES.indexOf(normalizeString(name).toLowerCase()) >= 0;
}

function computeWalletBalances(householdId) {
  var balances = { joint: 0, his: 0, hers: 0 };
  getSheetData(SHEET_NAMES.MONEY_TRANSACTIONS).forEach(function(transaction) {
    if (!rowMatchesHousehold(transaction, householdId)) return;
    var wallet = normalizeWallet(transaction.wallet);
    var amount = Number(transaction.amount) || 0;
    var type = normalizeString(transaction.type);
    if (type === 'income') {
      balances[wallet] += amount;
    } else if (type === 'expense') {
      balances[wallet] -= amount;
    } else if (type === 'savings_deposit') {
      balances[wallet] -= amount;
    } else if (type === 'savings_withdraw') {
      balances[wallet] += amount;
    }
  });
  return balances;
}

function goalBucketBalance(householdId, goalName) {
  var target = normalizeString(goalName).toLowerCase();
  var balance = 0;
  findManyBy(SHEET_NAMES.TRANSFERS, function(row) {
    return rowMatchesHousehold(row, householdId);
  }).forEach(function(row) {
    var amount = Number(row.amount) || 0;
    if (normalizeString(row.destination_target).toLowerCase() === target) {
      balance += amount;
    }
    if (normalizeString(row.source_account).toLowerCase() === target) {
      balance -= amount;
    }
  });
  return Math.round(balance * 100) / 100;
}

function executeTransfer(payload) {
  var userId = normalizeString(payload.userId);
  var sourceAccount = normalizeString(payload.sourceAccount).toLowerCase();
  var destinationTarget = normalizeString(payload.destinationTarget).toLowerCase();
  var amount = Number(payload.amount);
  var direction = normalizeString(payload.direction) || 'out';

  if (!userId) {
    throw new Error('userId is required.');
  }
  if (!sourceAccount) {
    throw new Error('sourceAccount is required.');
  }
  if (!destinationTarget) {
    throw new Error('destinationTarget is required.');
  }
  if (!(amount > 0)) {
    throw new Error('amount must be a positive number.');
  }
  if (sourceAccount === destinationTarget) {
    throw new Error('sourceAccount and destinationTarget must be different.');
  }

  var sourceIsWallet = isWalletAccount(sourceAccount);
  var destinationIsWallet = isWalletAccount(destinationTarget);
  if (!sourceIsWallet) {
    throw new Error('sourceAccount must be one of: ' + WALLET_NAMES.join(', ') + '.');
  }

  var householdId = getUserHouseholdId(userId);

  var lock = LockService.getScriptLock();
  lock.waitLock(30000);
  try {
    var now = nowIso();

    if (destinationIsWallet) {
      // Wallet to wallet: money leaves one wallet and arrives in the other.
      // Net household balance stays unchanged; the wallet breakdown moves.
      appendRow(SHEET_NAMES.MONEY_TRANSACTIONS, {
        transaction_id: generateId('m', SHEET_NAMES.MONEY_TRANSACTIONS),
        type: 'expense',
        description: 'Transfer to ' + destinationTarget,
        category: 'transfer',
        amount: amount,
        wallet: sourceAccount,
        user_id: userId,
        household_id: householdId,
        created_at: now
      });
      appendRow(SHEET_NAMES.MONEY_TRANSACTIONS, {
        transaction_id: generateId('m', SHEET_NAMES.MONEY_TRANSACTIONS),
        type: 'income',
        description: 'Transfer from ' + sourceAccount,
        category: 'transfer',
        amount: amount,
        wallet: destinationTarget,
        user_id: userId,
        household_id: householdId,
        created_at: now
      });
    } else if (GOAL_TARGETS.indexOf(destinationTarget) >= 0) {
      // Wallet to savings goal: recorded as a savings deposit so the balance
      // drops and the savings total rises, matching the Money screen math.
      appendRow(SHEET_NAMES.MONEY_TRANSACTIONS, {
        transaction_id: generateId('m', SHEET_NAMES.MONEY_TRANSACTIONS),
        type: 'savings_deposit',
        description: 'Transfer to ' + destinationTarget + ' goal',
        category: destinationTarget,
        amount: amount,
        wallet: sourceAccount,
        user_id: userId,
        household_id: householdId,
        created_at: now
      });
    } else {
      throw new Error('destinationTarget must be one of: ' + WALLET_NAMES.concat(GOAL_TARGETS).join(', ') + '.');
    }

    var wallets = computeWalletBalances(householdId);
    var sourceBalance = Math.round((wallets[sourceAccount] || 0) * 100) / 100;
    var destinationBalance = destinationIsWallet
      ? Math.round((wallets[destinationTarget] || 0) * 100) / 100
      : goalBucketBalance(householdId, destinationTarget);

    var transfer = {
      transfer_id: generateId('t', SHEET_NAMES.TRANSFERS),
      source_account: sourceAccount,
      destination_target: destinationTarget,
      amount: amount,
      direction: direction,
      source_balance: sourceBalance,
      destination_balance: destinationBalance,
      user_id: userId,
      household_id: householdId,
      created_at: now
    };
    appendRow(SHEET_NAMES.TRANSFERS, transfer);

    return {
      transfer: {
        sourceAccount: transfer.source_account,
        destinationTarget: transfer.destination_target,
        amount: transfer.amount,
        direction: transfer.direction,
        sourceBalance: sourceBalance,
        destinationBalance: destinationBalance,
        timestamp: transfer.created_at,
        transferId: transfer.transfer_id
      }
    };
  } finally {
    lock.releaseLock();
  }
}

function listTransfers(payload) {
  var userId = normalizeString(payload.userId);
  var householdId = getUserHouseholdId(userId);
  var limit = Math.min(toInt(payload.limit, 50) || 50, 200);

  var transfers = findManyBy(SHEET_NAMES.TRANSFERS, function(row) {
    return rowMatchesHousehold(row, householdId);
  }).sort(function(a, b) {
    return String(b.created_at).localeCompare(String(a.created_at));
  });

  return transfers.slice(0, limit).map(mapTransfer);
}
