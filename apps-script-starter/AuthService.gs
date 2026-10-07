function bootstrapUser(payload) {
  payload = payload || {};
  var email = normalizeString(payload.googleEmail).toLowerCase();
  if (!email) {
    throw new Error('googleEmail is required.');
  }

  var user = getActiveUserByEmail(email);
  if (!user) {
    return {
      authorized: false,
      reason: 'Email is not authorized for this household.'
    };
  }

  user = ensureUserHousehold(user);

  var partner = findActivePartner(user);

  return {
    authorized: true,
    user: mapUserSummary(user),
    partner: partner ? mapUserSummary(partner) : null
  };
}

function bootstrapDevice(payload) {
  payload = payload || {};
  var deviceId = normalizeString(payload.deviceId);
  if (!deviceId) {
    throw new Error('deviceId is required.');
  }

  var user = getUserByDeviceId(deviceId);
  if (!user) {
    var activeUsers = findManyBy(SHEET_NAMES.USERS, function(row) {
      return toBoolean(row.is_active);
    });
    var registeredDevices = activeUsers.filter(function(row) {
      return normalizeString(row.device_id) !== '';
    });
    var openSlot = activeUsers.find(function(row) {
      return normalizeString(row.device_id) === '';
    }) || null;

    if (openSlot) {
      openSlot.device_id = deviceId;
      openSlot.household_id = ensureHouseholdId();
      openSlot.updated_at = nowIso();
      if (!normalizeString(openSlot.display_name)) {
        openSlot.display_name = 'Home Member ' + String(Math.min(activeUsers.indexOf(openSlot) + 1, 2));
      }
      updateRowByIndex(SHEET_NAMES.USERS, openSlot.__rowIndex, openSlot);
      user = getUserById(openSlot.user_id);
    } else if (registeredDevices.length < 2 && activeUsers.length < 2) {
      var slotNumber = activeUsers.length + 1;
      var newUserId = generateId('u', SHEET_NAMES.USERS);
      appendRow(SHEET_NAMES.USERS, {
        user_id: newUserId,
        device_id: deviceId,
        email: '',
        display_name: 'Home Member ' + slotNumber,
        photo_url: '',
        role: 'member',
        household_side: '',
        household_id: ensureHouseholdId(),
        xp_total: 0,
        level: 1,
        coins_total: 0,
        current_streak: 0,
        longest_streak: 0,
        last_qualifying_date: '',
        created_at: nowIso(),
        updated_at: nowIso(),
        is_active: 'true'
      });

      user = getUserById(newUserId);
    } else {
      return {
        authorized: false,
        reason: 'This household already has two registered devices.'
      };
    }
  } else {
    user = ensureUserHousehold(user);
  }

  var partner = findActivePartner(user);

  return {
    authorized: true,
    user: mapUserSummary(user),
    partner: partner ? mapUserSummary(partner) : null
  };
}

function ensureHouseholdId() {
  var configured = normalizeString(getConfigString('household_id', ''));
  if (configured) return configured;

  var withHousehold = findManyBy(SHEET_NAMES.USERS, function(row) {
    return toBoolean(row.is_active) && normalizeString(row.household_id);
  })[0];
  if (withHousehold) {
    var inherited = normalizeString(withHousehold.household_id);
    upsertConfig('household_id', inherited);
    return inherited;
  }

  var generated = 'hh_' + Utilities.getUuid().replace(/-/g, '').substring(0, 10).toUpperCase();
  upsertConfig('household_id', generated);
  return generated;
}

function ensureUserHousehold(user) {
  if (!user || normalizeString(user.household_id)) return user;
  user.household_id = ensureHouseholdId();
  user.updated_at = nowIso();
  updateRowByIndex(SHEET_NAMES.USERS, user.__rowIndex, user);
  return user;
}

function findActivePartner(user) {
  if (!user) return null;
  var householdId = normalizeString(user.household_id);
  return findManyBy(SHEET_NAMES.USERS, function(row) {
    if (!toBoolean(row.is_active) || row.user_id === user.user_id) return false;
    if (!householdId) return true;
    var rowHousehold = normalizeString(row.household_id);
    return !rowHousehold || rowHousehold === householdId;
  })[0] || null;
}

function getActiveUserByEmail(email) {
  return findOneBy(SHEET_NAMES.USERS, function(row) {
    return normalizeString(row.email).toLowerCase() === email && toBoolean(row.is_active);
  });
}

function updateProfile(payload) {
  var userId = normalizeString(payload.userId);
  var displayName = normalizeString(payload.displayName);

  if (!userId) {
    throw new Error('userId is required.');
  }
  if (!displayName) {
    throw new Error('displayName is required.');
  }

  var user = getUserById(userId);
  if (!user) {
    throw new Error('User not found.');
  }

  user.display_name = displayName;

  var side = normalizeString(payload.householdSide).toLowerCase();
  if (side === 'his' || side === 'hers' || side === '') {
    user.household_side = side;
  }

  user.updated_at = nowIso();
  updateRowByIndex(SHEET_NAMES.USERS, user.__rowIndex, user);

  return mapUserSummary(getUserById(userId));
}

function getUserById(userId) {
  return findOneBy(SHEET_NAMES.USERS, function(row) {
    return row.user_id === userId && toBoolean(row.is_active);
  });
}

function getUserByDeviceId(deviceId) {
  return findOneBy(SHEET_NAMES.USERS, function(row) {
    return normalizeString(row.device_id) === deviceId && toBoolean(row.is_active);
  });
}

function mapUserSummary(row) {
  return {
    userId: row.user_id,
    email: row.email,
    displayName: row.display_name,
    photoUrl: row.photo_url || '',
    role: row.role,
    householdSide: row.household_side || '',
    householdId: row.household_id || '',
    xpTotal: toInt(row.xp_total),
    level: toInt(row.level, 1),
    coinsTotal: toInt(row.coins_total),
    currentStreak: toInt(row.current_streak),
    longestStreak: toInt(row.longest_streak),
    lastQualifyingDate: row.last_qualifying_date || ''
  };
}
