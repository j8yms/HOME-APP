var LIBRARY_CATEGORIES = ['maintenance', 'vault', 'health'];

function listReferenceEntries(payload) {
  var category = normalizeString(payload.category);
  var entries = findManyBy(SHEET_NAMES.REFERENCE_LIBRARY, function(row) {
    return true;
  }).map(mapReferenceEntry);

  if (category) {
    entries = entries.filter(function(entry) {
      return entry.category === category;
    });
  }

  entries.sort(function(a, b) {
    return String(b.updatedAt).localeCompare(String(a.updatedAt));
  });

  return entries;
}

function createReferenceEntry(payload) {
  var category = normalizeReferenceCategory(payload.category);
  var title = normalizeString(payload.title);
  var content = normalizeString(payload.content);
  var userId = normalizeString(payload.userId);

  if (!category) {
    throw new Error('category is required.');
  }
  if (!title) {
    throw new Error('title is required.');
  }

  var entry = {
    entry_id: generateId('l', SHEET_NAMES.REFERENCE_LIBRARY),
    category: category,
    title: title,
    content: content,
    created_by_user_id: userId,
    updated_at: nowIso(),
    created_at: nowIso()
  };
  appendRow(SHEET_NAMES.REFERENCE_LIBRARY, entry);
  return mapReferenceEntry(entry);
}

function updateReferenceEntry(payload) {
  var entryId = normalizeString(payload.entryId);
  var title = normalizeString(payload.title);
  var content = normalizeString(payload.content);

  if (!entryId) {
    throw new Error('entryId is required.');
  }
  if (!title) {
    throw new Error('title is required.');
  }

  var entry = findOneBy(SHEET_NAMES.REFERENCE_LIBRARY, function(row) {
    return row.entry_id === entryId;
  });
  if (!entry) {
    throw new Error('Reference entry not found.');
  }

  entry.title = title;
  entry.content = content;
  entry.updated_at = nowIso();
  updateRowByIndex(SHEET_NAMES.REFERENCE_LIBRARY, entry.__rowIndex, entry);
  return mapReferenceEntry(entry);
}

function deleteReferenceEntry(payload) {
  var entryId = normalizeString(payload.entryId);
  if (!entryId) {
    throw new Error('entryId is required.');
  }

  var entry = findOneBy(SHEET_NAMES.REFERENCE_LIBRARY, function(row) {
    return row.entry_id === entryId;
  });
  if (!entry) {
    throw new Error('Reference entry not found.');
  }

  getSheet(SHEET_NAMES.REFERENCE_LIBRARY).deleteRow(entry.__rowIndex);
  return { entryId: entryId, deleted: true };
}

function normalizeReferenceCategory(value) {
  var category = normalizeString(value).toLowerCase();
  return LIBRARY_CATEGORIES.indexOf(category) < 0 ? '' : category;
}

function mapReferenceEntry(row) {
  return {
    entryId: row.entry_id,
    category: row.category || '',
    title: row.title || '',
    content: row.content || '',
    createdByUserId: row.created_by_user_id || '',
    updatedAt: row.updated_at || '',
    createdAt: row.created_at || ''
  };
}
