function mapSubscription(row) {
  return {
    subscriptionId: row.subscription_id,
    userId: row.user_id || '',
    serviceName: row.service_name || '',
    nextRenewalDate: row.next_renewal_date || '',
    billingInterval: row.billing_interval || '',
    terminationRule: row.termination_rule || '',
    isActive: toBoolean(row.is_active),
    createdAt: row.created_at || '',
    updatedAt: row.updated_at || ''
  };
}

function listSubscriptions(payload) {
  var userId = normalizeString(payload.userId);
  var householdId = getUserHouseholdId(userId);
  var today = todayDateStringByHousehold(householdId);

  return findManyBy(SHEET_NAMES.SUBSCRIPTIONS, function(row) {
    return rowMatchesHousehold(row, householdId);
  }).sort(function(a, b) {
    var aDate = normalizeString(a.next_renewal_date) || '9999-12-31';
    var bDate = normalizeString(b.next_renewal_date) || '9999-12-31';
    if (aDate === bDate) {
      return String(a.service_name).localeCompare(String(b.service_name));
    }
    return aDate < bDate ? -1 : 1;
  }).map(function(row) {
    var mapped = mapSubscription(row);
    mapped.renewalDue = Boolean(mapped.nextRenewalDate && mapped.nextRenewalDate <= addDays(today, 7));
    return mapped;
  });
}

function createSubscription(payload) {
  var userId = normalizeString(payload.userId);
  var serviceName = normalizeString(payload.serviceName);
  var nextRenewalDate = normalizeString(payload.nextRenewalDate);
  var billingInterval = normalizeString(payload.billingInterval) || 'monthly';
  var terminationRule = normalizeString(payload.terminationRule);
  var amount = Number(payload.amount) || 0;

  if (!userId) {
    throw new Error('userId is required.');
  }
  if (!serviceName) {
    throw new Error('serviceName is required.');
  }
  if (!nextRenewalDate) {
    throw new Error('nextRenewalDate is required.');
  }

  var householdId = getUserHouseholdId(userId);
  var subscription = {
    subscription_id: generateId('sub', SHEET_NAMES.SUBSCRIPTIONS),
    service_name: serviceName,
    amount: amount,
    next_renewal_date: nextRenewalDate,
    billing_interval: billingInterval,
    termination_rule: terminationRule,
    is_active: 'true',
    user_id: userId,
    household_id: householdId,
    created_at: nowIso(),
    updated_at: nowIso()
  };

  appendRow(SHEET_NAMES.SUBSCRIPTIONS, subscription);
  return mapSubscription(subscription);
}

function updateSubscription(payload) {
  var subscriptionId = normalizeString(payload.subscriptionId);
  if (!subscriptionId) {
    throw new Error('subscriptionId is required.');
  }

  var subscription = findOneBy(SHEET_NAMES.SUBSCRIPTIONS, function(row) {
    return row.subscription_id === subscriptionId;
  });
  if (!subscription) {
    throw new Error('Subscription not found.');
  }

  var userId = normalizeString(payload.userId);
  var userHouseholdId = getUserHouseholdId(userId);
  if (userHouseholdId && subscription.household_id && subscription.household_id !== userHouseholdId) {
    throw new Error('Subscription does not belong to your household.');
  }

  if (payload.nextRenewalDate !== undefined && payload.nextRenewalDate !== null && payload.nextRenewalDate !== '') {
    subscription.next_renewal_date = normalizeString(payload.nextRenewalDate);
  }
  if (payload.isActive !== undefined && payload.isActive !== null) {
    subscription.is_active = String(Boolean(payload.isActive));
  }
  if (payload.serviceName !== undefined && payload.serviceName !== null && normalizeString(payload.serviceName)) {
    subscription.service_name = normalizeString(payload.serviceName);
  }
  if (payload.amount !== undefined && payload.amount !== null && payload.amount !== '') {
    subscription.amount = Number(payload.amount) || 0;
  }
  if (payload.billingInterval !== undefined && normalizeString(payload.billingInterval)) {
    subscription.billing_interval = normalizeString(payload.billingInterval);
  }
  if (payload.terminationRule !== undefined && normalizeString(payload.terminationRule)) {
    subscription.termination_rule = normalizeString(payload.terminationRule);
  }

  subscription.updated_at = nowIso();
  updateRowByIndex(SHEET_NAMES.SUBSCRIPTIONS, subscription.__rowIndex, subscription);
  return mapSubscription(subscription);
}
