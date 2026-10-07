// Spending analytics for the current month.
// categories: expense breakdown per category (share of monthly spending).
// overview.combinedTotal: total household spending for the month.
// overview.individualBreakdowns: spending per household member.
function getAnalyticsChart(payload) {
  var userId = normalizeString(payload.userId);
  var householdId = getUserHouseholdId(userId);
  var month = normalizeString(payload.month) || todayDateStringByHousehold(householdId).substring(0, 7);

  var expensesByCategory = {};
  var expensesByUser = {};
  var totalSpent = 0;

  findManyBy(SHEET_NAMES.MONEY_TRANSACTIONS, function(row) {
    if (!rowMatchesHousehold(row, householdId)) return false;
    if (normalizeString(row.type) !== 'expense') return false;
    return normalizeString(row.created_at).substring(0, 7) === month;
  }).forEach(function(row) {
    var amount = Number(row.amount) || 0;
    var category = normalizeString(row.category) || 'Uncategorized';
    var spender = normalizeString(row.user_id) || 'unknown';

    expensesByCategory[category] = (expensesByCategory[category] || 0) + amount;
    expensesByUser[spender] = (expensesByUser[spender] || 0) + amount;
    totalSpent += amount;
  });

  var colors = ['#4F8EF7', '#F7784F', '#47C98A', '#F2C14E', '#9B6BF2', '#F26BA8', '#4FC3D9', '#8A9BA8'];
  var categories = Object.keys(expensesByCategory).map(function(category, index) {
    var spent = Math.round(expensesByCategory[category] * 100) / 100;
    return {
      category: category,
      expenditurePct: totalSpent > 0 ? Math.round((expensesByCategory[category] / totalSpent) * 1000) / 10 : 0,
      totalSpent: spent,
      categoryType: 'expense',
      color: colors[index % colors.length]
    };
  }).sort(function(a, b) {
    return b.totalSpent - a.totalSpent;
  });

  var individualBreakdowns = Object.keys(expensesByUser).map(function(spenderId) {
    var member = findOneBy(SHEET_NAMES.USERS, function(row) {
      return row.user_id === spenderId;
    });
    return {
      userId: spenderId,
      displayName: member && member.display_name ? String(member.display_name) : '',
      totalSpent: Math.round(expensesByUser[spenderId] * 100) / 100
    };
  }).sort(function(a, b) {
    return b.totalSpent - a.totalSpent;
  });

  return {
    userId: userId,
    householdId: householdId,
    month: month,
    categories: categories,
    overview: {
      combinedTotal: Math.round(totalSpent * 100) / 100,
      individualBreakdowns: individualBreakdowns
    }
  };
}
