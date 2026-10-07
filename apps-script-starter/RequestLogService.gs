// Request-level idempotency. The Android client attaches a unique
// requestId to every POST; successful responses are cached here so a
// retried request (transport failures can be retried safely) returns the
// original result instead of executing the operation twice.
function findCachedResponse(requestId) {
  if (!requestId) {
    return '';
  }
  var row = findOneBy(SHEET_NAMES.REQUEST_LOG, function(candidate) {
    return candidate.request_id === requestId;
  });
  return row ? String(row.response || '') : '';
}

function storeCachedResponse(requestId, route, response) {
  if (!requestId || !response) {
    return;
  }
  if (findCachedResponse(requestId)) {
    return;
  }

  // Keep the log bounded so the sheet never grows without limit.
  var rows = getSheetData(SHEET_NAMES.REQUEST_LOG);
  if (rows.length >= 800) {
    var sheet = getSheet(SHEET_NAMES.REQUEST_LOG);
    var deleteCount = Math.min(200, rows.length - 400);
    if (deleteCount > 0 && sheet.getLastRow() > deleteCount + 1) {
      sheet.deleteRows(2, deleteCount);
      invalidateSheetCache(SHEET_NAMES.REQUEST_LOG);
    }
  }

  appendRow(SHEET_NAMES.REQUEST_LOG, {
    request_id: requestId,
    route: route,
    response: response,
    created_at: nowIso()
  });
}
