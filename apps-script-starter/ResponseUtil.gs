function jsonSuccess(data, meta) {
  return jsonOutput({
    success: true,
    data: data || {},
    meta: Object.assign({ serverTime: nowIso() }, meta || {})
  });
}

function jsonError(code, message, details) {
  return jsonOutput({
    success: false,
    error: {
      code: code,
      message: message,
      details: details || null
    }
  });
}

function jsonOutput(payload) {
  return ContentService
    .createTextOutput(JSON.stringify(payload))
    .setMimeType(ContentService.MimeType.JSON);
}

function nowIso() {
  return new Date().toISOString();
}
