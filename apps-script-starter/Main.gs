function doGet(e) {
  try {
    var payload = Object.assign({}, (e && e.parameter) || {});
    var route = resolveRoute(e, payload);
    return dispatchRoute(route, payload);
  } catch (error) {
    return handleRouteError(error);
  }
}

function doPost(e) {
  try {
    var payload = parsePostBody(e);
    var route = resolveRoute(e, payload);
    return dispatchRoute(route, payload);
  } catch (error) {
    return handleRouteError(error);
  }
}

function resolveRoute(e, payload) {
  var route = normalizeString((payload && payload.route) || '');
  if (route) {
    return route;
  }

  if (e && e.pathInfo) {
    return normalizeString(e.pathInfo);
  }

  throw new Error('Route is required.');
}

function parsePostBody(e) {
  if (!e || !e.postData || !e.postData.contents) {
    return {};
  }

  return JSON.parse(e.postData.contents);
}

function dispatchRoute(route, payload) {
  switch (route) {
    case 'health':
      return jsonSuccess({ status: 'ok' });

    case 'setup/migrate':
      return jsonSuccess(migrateHouseholdSpreadsheet());

    case 'auth/bootstrap':
      return jsonSuccess(bootstrapUser(payload));
    case 'auth/bootstrap_device':
      return jsonSuccess(bootstrapDevice(payload));
    case 'profile/update':
      return jsonSuccess(updateProfile(payload));

    case 'dashboard':
      return jsonSuccess(getDashboard(payload));

    case 'tasks/list':
      return jsonSuccess({ tasks: listTasks(payload) });

    case 'tasks/create':
      return jsonSuccess({ task: createTask(payload) });

    case 'tasks/update':
      return jsonSuccess({ task: updateTask(payload) });

    case 'tasks/complete':
      return jsonSuccess(completeTask(payload));

    case 'tasks/claim':
      return jsonSuccess(claimTask(payload));

    case 'tasks/delete':
      return jsonSuccess({ task: deleteTask(payload) });

    case 'workouts/list':
      return jsonSuccess({ workouts: listWorkouts(payload) });

    case 'workouts/log':
      return jsonSuccess(logWorkout(payload));

    case 'rewards':
      return jsonSuccess({ rewards: getRewards(payload) });

    case 'rewards/create':
      return jsonSuccess({ reward: createReward(payload) });

    case 'rewards/delete':
      return jsonSuccess({ reward: deleteReward(payload) });

    case 'rewards/redeem':
      return jsonSuccess(redeemReward(payload));

    case 'rewards/redemptions':
      return jsonSuccess({ redemptions: listRedemptions(payload) });

    case 'household/log/list':
      return jsonSuccess({ logs: listHouseholdLogs(payload) });

    case 'household/log/create':
      return jsonSuccess(createHouseholdLog(payload));

    case 'history/ledger':
      return jsonSuccess({ entries: listLedger(payload) });

    case 'finance/summary':
      return jsonSuccess(getFinanceSummary(payload));

    case 'finance/add_transaction':
      return jsonSuccess(addTransaction(payload));

    case 'finance/goals':
      return jsonSuccess(setFinanceGoals(payload));

    case 'finance/currency':
      return jsonSuccess(setFinanceCurrency(payload));

    case 'library/list':
      return jsonSuccess({ entries: listReferenceEntries(payload) });

    case 'library/create':
      return jsonSuccess({ entry: createReferenceEntry(payload) });

    case 'library/update':
      return jsonSuccess({ entry: updateReferenceEntry(payload) });

    case 'library/delete':
      return jsonSuccess(deleteReferenceEntry(payload));

    case 'bills/list':
      return jsonSuccess({ bills: listBills(payload) });

    case 'bills/create':
      return jsonSuccess({ bill: createBill(payload) });

    case 'bills/update':
      return jsonSuccess({ bill: updateBill(payload) });

    case 'shopping/list':
      return jsonSuccess({ items: listShoppingItems(payload) });

    case 'shopping/create':
      return jsonSuccess({ item: createShoppingItem(payload) });

    case 'shopping/update':
      return jsonSuccess({ item: updateShoppingItem(payload) });

    default:
      return jsonError('ROUTE_NOT_FOUND', 'Unsupported route: ' + route);
  }
}

function handleRouteError(error) {
  var code = normalizeErrorCode(error);
  return jsonError(code, error.message || 'Unexpected server error.');
}

function ping() {
  return {status: 'ok'};
}

function normalizeErrorCode(error) {
  var message = (error && error.message) || 'SERVER_ERROR';

  if (message === 'TASK_NOT_FOUND') return 'TASK_NOT_FOUND';
  if (message === 'TASK_ALREADY_COMPLETED') return 'TASK_ALREADY_COMPLETED';
  if (message === 'TASK_NOT_CLAIMABLE') return 'TASK_NOT_CLAIMABLE';
  if (message === 'STALE_TASK_VERSION') return 'STALE_TASK_VERSION';
  if (message.indexOf('required') >= 0) return 'INVALID_REQUEST';

  return 'SERVER_ERROR';
}
