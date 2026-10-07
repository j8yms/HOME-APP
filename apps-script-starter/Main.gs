function doGet(e) {
  try {
    var payload = Object.assign({}, (e && e.parameter) || {});
    var route = resolveRoute(e, payload);
    return dispatchRoute(route, payload, false);
  } catch (error) {
    return handleRouteError(error);
  }
}

function doPost(e) {
  try {
    var payload = parsePostBody(e);
    payload.__viaPost = true;
    var route = resolveRoute(e, payload);
    var requestId = normalizeString(payload && payload.requestId);

    // Idempotent replay: a retried POST with the same requestId returns the
    // first successful response instead of running the operation twice.
    if (requestId) {
      var cached = findCachedResponse(requestId);
      if (cached) {
        return ContentService.createTextOutput(cached).setMimeType(ContentService.MimeType.JSON);
      }
    }

    var output = dispatchRoute(route, payload, true);

    if (requestId && output && typeof output.getContent === 'function') {
      var content = output.getContent();
      if (content.indexOf('{"success":true') === 0) {
        storeCachedResponse(requestId, route, content);
      }
    }

    return output;
  } catch (error) {
    return handleRouteError(error);
  }
}

function resolveRoute(e, payload) {
  // POST bodies carry `route` (Android DTOs), but allow query-string and
  // pathInfo routes too so GET and forced-POST calls behave the same.
  var route = normalizeString(payload && payload.route);
  if (route) {
    return route;
  }

  route = normalizeString(e && e.parameter && e.parameter.route);
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
    var missingBodyError = new Error('Request body was lost in transit. Retry the request.');
    missingBodyError.code = 'BODY_REQUIRED';
    throw missingBodyError;
  }

  try {
    var parsed = JSON.parse(e.postData.contents);
    if (parsed && typeof parsed === 'object') {
      return parsed;
    }
    throw new Error('Request body must be a JSON object.');
  } catch (error) {
    if (error.code === 'BODY_REQUIRED') {
      throw error;
    }
    if (error instanceof SyntaxError || String(error.message).indexOf('JSON object') >= 0) {
      var requestError = new Error('Request body was lost in transit. Retry the request.');
      requestError.code = 'BODY_REQUIRED';
      throw requestError;
    }
    throw error;
  }
}

// Routes that mutate state must only run for POSTs whose JSON body arrived.
// Google's redirect chain can re-execute a deployment as a GET, which would
// otherwise run mutations with only query parameters (silently losing fields).
var POST_ONLY_ROUTES = {
  'profile/update': true,
  'tasks/create': true,
  'tasks/update': true,
  'tasks/complete': true,
  'tasks/claim': true,
  'tasks/delete': true,
  'workouts/log': true,
  'rewards/create': true,
  'rewards/delete': true,
  'rewards/redeem': true,
  'household/log/create': true,
  'finance/add_transaction': true,
  'finance/net_worth_snapshot': true,
  'budgets/create': true,
  'budgets/update': true,
  'subscriptions/create': true,
  'subscriptions/update': true,
  'transfers/execute': true,
  'wealth/save': true,
  'wealth/delete': true,
  'wealth/freedom/save': true,
  'library/create': true,
  'library/update': true,
  'library/delete': true,
  'bills/create': true,
  'bills/update': true,
  'shopping/create': true,
  'shopping/update': true
};

function dispatchRoute(route, payload, viaPost) {
  if (POST_ONLY_ROUTES[route] && !viaPost) {
    var lostBodyError = new Error('Request body was lost in transit. Retry the request.');
    lostBodyError.code = 'BODY_REQUIRED';
    throw lostBodyError;
  }

  switch (route) {
    case 'health':
      return jsonSuccess({ status: 'ok' });

    case 'setup/migrate':
      return jsonSuccess(migrateHouseholdSpreadsheet());
    case 'setup/seed':
      return jsonSuccess(seedDemoConfig());
    case 'setup/schema':
      return jsonSuccess(describeSpreadsheetSchema());

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
    case 'ledger/list':
      return jsonSuccess({ entries: listLedger(payload) });

    case 'finance/summary':
      return jsonSuccess(getFinanceSummary(payload));

    case 'finance/add_transaction':
      return jsonSuccess(addTransaction(payload));

    case 'finance/goals':
      return jsonSuccess(setFinanceGoals(payload));

    case 'finance/currency':
      return jsonSuccess(setFinanceCurrency(payload));

    case 'finance/net_worth_snapshot':
      return jsonSuccess(createNetWorthSnapshot(payload));
    case 'finance/net_worth_history':
      return jsonSuccess({ snapshots: listNetWorthSnapshots(payload) });

    case 'budgets/list':
      return jsonSuccess({ budgets: listBudgets(payload) });

    case 'budgets/create':
      return jsonSuccess({ budget: createBudget(payload) });

    case 'budgets/update':
      return jsonSuccess({ budget: updateBudget(payload) });

    case 'subscriptions/list':
      return jsonSuccess({ subscriptions: listSubscriptions(payload) });

    case 'subscriptions/create':
      return jsonSuccess({ subscription: createSubscription(payload) });

    case 'subscriptions/update':
      return jsonSuccess({ subscription: updateSubscription(payload) });

    case 'transfers/execute':
      return jsonSuccess(executeTransfer(payload));

    case 'transfers/list':
      return jsonSuccess({ transfers: listTransfers(payload) });

    case 'analytics/chart':
      return jsonSuccess(getAnalyticsChart(payload));

    case 'wealth/summary':
      return jsonSuccess(getWealthSummary(payload));

    case 'wealth/list':
      return jsonSuccess(listWealthSection(payload));

    case 'wealth/save':
      return jsonSuccess(saveWealthItem(payload));

    case 'wealth/delete':
      return jsonSuccess(deleteWealthItem(payload));

    case 'wealth/history':
      return jsonSuccess({ snapshots: listWealthHistory(payload) });

    case 'wealth/freedom/get':
      return jsonSuccess(getFinancialFreedom(payload));

    case 'wealth/freedom/save':
      return jsonSuccess(saveFinancialFreedom(payload));

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

function describeSpreadsheetSchema() {
  var spreadsheet = getSpreadsheet();
  return {
    sheets: spreadsheet.getSheets().map(function(sheet) {
      var name = sheet.getName();
      var lastColumn = Math.max(sheet.getLastColumn(), 1);
      return {
        name: name,
        headers: sheet.getRange(1, 1, 1, lastColumn).getValues()[0]
      };
    })
  };
}

function handleRouteError(error) {
  var code = normalizeErrorCode(error);
  return jsonError(code, error.message || 'Unexpected server error.');
}

function ping() {
  return {status: 'ok'};
}

function normalizeErrorCode(error) {
  if (error && error.code === 'BODY_REQUIRED') {
    return 'BODY_REQUIRED';
  }

  if (error && error.code === 'INVALID_REQUEST') {
    return 'INVALID_REQUEST';
  }

  var message = (error && error.message) || 'SERVER_ERROR';

  if (message === 'TASK_NOT_FOUND') return 'TASK_NOT_FOUND';
  if (message === 'TASK_ALREADY_COMPLETED') return 'TASK_ALREADY_COMPLETED';
  if (message === 'TASK_NOT_CLAIMABLE') return 'TASK_NOT_CLAIMABLE';
  if (message === 'STALE_TASK_VERSION') return 'STALE_TASK_VERSION';
  if (message.indexOf('Invalid request body') >= 0) return 'INVALID_REQUEST';
  if (message === 'Route is required.') return 'INVALID_REQUEST';
  if (message.indexOf('required') >= 0) return 'INVALID_REQUEST';
  if (message.indexOf('must be') >= 0) return 'VALIDATION_ERROR';
  if (message.indexOf('not found') >= 0) return 'NOT_FOUND';

  return 'SERVER_ERROR';
}
