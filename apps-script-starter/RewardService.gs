function getRewards(payload) {
  var userId = normalizeString(payload.userId);
  return findManyBy(SHEET_NAMES.REWARDS_STORE, function(row) {
    var active = toBoolean(row.is_active);
    if (!active) {
      return false;
    }
    var hidden = toBoolean(row.hide_from_partner);
    if (hidden && userId) {
      return normalizeString(row.created_by_user_id) === userId;
    }
    return true;
  }).map(mapRewardItem);
}

function createReward(payload) {
  var title = normalizeString(payload.title);
  var userId = normalizeString(payload.userId);
  if (!title) {
    throw new Error('title is required.');
  }
  if (!userId) {
    throw new Error('userId is required.');
  }

  var reward = {
    reward_id: generateId('r', SHEET_NAMES.REWARDS_STORE),
    title: title,
    description: normalizeString(payload.description),
    cost_coins: String(toInt(payload.costCoins)),
    cost_xp: String(toInt(payload.costXp)),
    category: normalizeString(payload.category) || 'Custom',
    is_active: 'true',
    hide_from_partner: String(Boolean(payload.hideFromPartner)),
    created_by_user_id: userId,
    created_at: nowIso(),
    updated_at: nowIso()
  };

  appendRow(SHEET_NAMES.REWARDS_STORE, reward);
  return mapRewardItem(reward);
}

function deleteReward(payload) {
  var rewardId = normalizeString(payload.rewardId);
  var userId = normalizeString(payload.userId);
  if (!rewardId || !userId) {
    throw new Error('rewardId and userId are required.');
  }

  var reward = getRewardById(rewardId);
  if (!reward) {
    throw new Error('Reward not found.');
  }

  if (normalizeString(reward.created_by_user_id) !== userId) {
    throw new Error('Not authorized to delete this reward.');
  }

  reward.is_active = 'false';
  reward.updated_at = nowIso();
  updateRowByIndex(SHEET_NAMES.REWARDS_STORE, reward.__rowIndex, reward);
  return mapRewardItem(reward);
}

function listRedemptions(payload) {
  var userId = normalizeString(payload.userId);
  var rewardRows = getSheetData(SHEET_NAMES.REWARDS_STORE);
  var rewardById = {};
  rewardRows.forEach(function(row) {
    rewardById[row.reward_id] = row;
  });

  var redemptions = findManyBy(SHEET_NAMES.REWARD_REDEMPTIONS, function() {
    return true;
  }).map(function(row) {
    var reward = rewardById[row.reward_id] || {};
    return {
      redemptionId: row.redemption_id,
      rewardId: row.reward_id || '',
      rewardTitle: reward.title || 'Reward',
      costCoins: toInt(row.cost_coins),
      costXp: toInt(row.cost_xp),
      status: row.status || 'pending_approval',
      redeemedAt: row.redeemed_at || '',
      resolvedAt: row.resolved_at || '',
      notes: row.notes || '',
      userId: row.user_id || ''
    };
  });

  if (userId) {
    redemptions = redemptions.filter(function(redemption) {
      return redemption.userId === userId;
    });
  }

  redemptions.sort(function(a, b) {
    return String(b.redeemedAt).localeCompare(String(a.redeemedAt));
  });

  return redemptions;
}

function redeemReward(payload) {
  var userId = normalizeString(payload.userId);
  var rewardId = normalizeString(payload.rewardId);
  var notes = normalizeString(payload.notes);
  var requestId = payload.requestId || generateId('rwd', SHEET_NAMES.REWARD_REDEMPTIONS);

  if (!userId || !rewardId) {
    throw new Error('userId and rewardId are required.');
  }

  var user = getUserById(userId);
  if (!user) {
    throw new Error('User not found.');
  }

  var householdId = getUserHouseholdId(userId);
  if (!householdId) {
    throw new Error('User not associated with a household.');
  }

  var reward = getRewardById(rewardId);
  if (!reward || !toBoolean(reward.is_active)) {
    throw new Error('Reward not found or inactive.');
  }

  // Household isolation: verify reward belongs to user's household
  var rewardHouseholdId = getRewardHouseholdId(rewardId);
  if (rewardHouseholdId !== householdId) {
    throw new Error('This reward does not belong to your household.');
  }

  var costCoins = toInt(reward.cost_coins);
  var costXp = toInt(reward.cost_xp);

  // Use LockService for atomic operation
  var lock = LockService.getScriptLock();
  lock.waitLock(30);  // Wait up to 30 seconds for lock

  try {
    // Check idempotency - if a redemption with this requestId already exists, skip
    var existingRedemptions = findManyBy(SHEET_NAMES.REWARD_REDEMPTIONS, function(row) {
      return row.request_id === requestId;
    });
    if (existingRedemptions.length > 0) {
      // Return existing redemption data
      var existing = existingRedemptions[0];
      var updatedUser = getUserById(userId);
      return {
        reward: mapRewardItem(reward),
        redemptionId: existing.redemption_id,
        user: Object.assign({}, mapUserSummary(updatedUser), {
          xpTotal: updatedUser.xp_total,
          coinsTotal: updatedUser.coins_total,
          currentStreak: updatedUser.currentStreak,
          longestStreak: updatedUser.longestStreak
        })
      };
    }

    // Validate sufficient coins and XP
    if (toInt(user.coins_total) < costCoins) {
      throw new Error('Insufficient coins to redeem this reward. You have ' + user.coins_total + ' coins.');
    }

    if (toInt(user.xp_total) < costXp) {
      throw new Error('Insufficient XP to redeem this reward. You have ' + user.xp_total + ' XP.');
    }

    var redemptionId = generateId('r', SHEET_NAMES.REWARD_REDEMPTIONS);
    appendRow(SHEET_NAMES.REWARD_REDEMPTIONS, {
      redemption_id: redemptionId,
      request_id: requestId,
      reward_id: rewardId,
      user_id: userId,
      cost_coins: costCoins,
      cost_xp: costXp,
      status: 'pending_approval',
      redeemed_at: nowIso(),
      resolved_at: '',
      notes: notes
    });

    appendLedgerEntry({
      userId: userId,
      sourceType: 'reward',
      sourceId: rewardId,
      actionType: 'reward_redeem',
      xpDelta: -costXp,
      coinDelta: -costCoins,
      reason: 'Reward redeemed: ' + reward.title
    });

    var totals = updateUserTotals(userId, -costXp, -costCoins);
    var updatedUser = getUserById(userId);

    return {
      reward: mapRewardItem(reward),
      redemptionId: redemptionId,
      user: Object.assign({}, mapUserSummary(updatedUser), totals)
    };
  } finally {
    lock.release();
  }
}

function getRewardById(rewardId) {
  return findOneBy(SHEET_NAMES.REWARDS_STORE, function(row) {
    return row.reward_id === rewardId;
  });
}

function mapRewardItem(row) {
  return {
    rewardId: row.reward_id,
    title: row.title,
    description: row.description || '',
    costCoins: toInt(row.cost_coins),
    costXp: toInt(row.cost_xp),
    category: row.category || '',
    isActive: toBoolean(row.is_active),
    hideFromPartner: toBoolean(row.hide_from_partner)
  };
}

function getUserHouseholdId(userId) {
  var user = findOneBy(SHEET_NAMES.USERS, function(row) {
    return row.user_id === userId;
  });
  return user ? user.household_id : '';
}

function getRewardHouseholdId(rewardId) {
  var reward = getRewardById(rewardId);
  return reward ? reward.household_id : '';
}
