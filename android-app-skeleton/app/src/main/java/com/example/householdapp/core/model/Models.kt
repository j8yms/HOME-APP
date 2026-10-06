package com.example.householdapp.core.model

data class UserProfile(
    val userId: String,
    val email: String,
    val displayName: String,
    val role: String,
    val xpTotal: Int,
    val level: Int,
    val coinsTotal: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val photoUrl: String = "",
    val lastQualifyingDate: String = "",
    val householdSide: String = "",
    val householdId: String = "",
    val personalIdentity: String = ""
)

data class Task(
    val taskId: String,
    val title: String,
    val description: String?,
    val category: String,
    val assignedToUserId: String,
    val createdByUserId: String,
    val status: String,
    val priority: String,
    val dueDate: String?,
    val repeatRule: String?,
    val recurrenceId: String = "",  // series_id for recurring task groups
    val xpReward: Int,
    val coinReward: Int,
    val streakEligible: Boolean,
    val version: Int,
    val completedAt: String = "",
    val completedByUserId: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
    val assigneeLabel: String = ""
)

data class CouplesStreak(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0
)

data class RewardItem(
    val rewardId: String,
    val title: String,
    val description: String,
    val costCoins: Int,
    val costXp: Int,
    val category: String = "",
    val hideFromPartner: Boolean = false
)

data class DashboardData(
    val currentUser: UserProfile? = null,
    val partner: UserProfile? = null,
    val tasksDueToday: List<Task> = emptyList(),
    val overdueTasks: List<Task> = emptyList(),
    val couplesStreak: CouplesStreak = CouplesStreak(),

    // A. Greeting
    val greeting: GreetingData = GreetingData(),

    // B. TODAY
    val today: TodayData = TodayData(),

    // C. MONEY
    val money: MoneyData = MoneyData(),

    // D. HOUSEHOLD
    val household: HouseholdData = HouseholdData(),

    // E. GOALS
    val goals: GoalsData = GoalsData(),

    // F. ACTIVITY
    val activity: ActivityFeed = ActivityFeed(),

    // G. GAMIFICATION (secondary)
    val gamification: GamificationData = GamificationData()
)

data class HouseholdLogEntry(
    val logId: String,
    val type: String,
    val title: String,
    val details: String = "",
    val userId: String,
    val relatedTaskId: String = "",
    val xpReward: Int = 0,
    val coinReward: Int = 0,
    val loggedAt: String = "",
    val createdAt: String = ""
)

data class WorkoutEntry(
    val workoutId: String,
    val workoutType: String,
    val durationMinutes: Int,
    val intensity: String = "medium",
    val notes: String = "",
    val xpReward: Int = 0,
    val coinReward: Int = 0,
    val loggedAt: String = "",
    val createdAt: String = ""
)

data class LedgerEntry(
    val ledgerId: String,
    val sourceType: String = "",
    val actionType: String = "",
    val xpDelta: Int = 0,
    val coinDelta: Int = 0,
    val reason: String = "",
    val userId: String = "",
    val createdAt: String = "",
    val amount: Double = 0.0,
    val direction: String = "out",
    val category: String = "",
    val sourceId: String = "",
    val description: String = "",
    val timestamp: String = ""
)

data class Redemption(
    val redemptionId: String,
    val rewardId: String = "",
    val rewardTitle: String = "",
    val costCoins: Int,
    val costXp: Int,
    val status: String = "redeemed",
    val redeemedAt: String = "",
    val resolvedAt: String = "",
    val notes: String = "",
    val userId: String = ""
)

data class MoneyTransaction(
    val transactionId: String,
    val type: String,
    val description: String,
    val category: String = "",
    val amount: Double = 0.0,
    val wallet: String = "joint",
    val userId: String = "",
    val createdAt: String = ""
)

data class WalletBreakdown(
    val joint: Double = 0.0,
    val his: Double = 0.0,
    val hers: Double = 0.0
)

data class FinanceGoals(
    val vacationGoal: Double = 0.0,
    val dreamGoal: Double = 0.0
)

data class Bill(
    val billId: String,
    val title: String,
    val category: String = "",
    val amount: Double = 0.0,
    val dueDate: String = "",
    val status: String = "pending",
    val paidByUserId: String = "",
    val paidAt: String = ""
)

data class ShoppingItem(
    val itemId: String,
    val title: String,
    val category: String = "",
    val estimatedCost: Double = 0.0,
    val status: String = "open",
    val addedByUserId: String = "",
    val purchasedAt: String = ""
)

data class FinanceSummary(
    val balance: Double = 0.0,
    val savings: Double = 0.0,
    val wallets: WalletBreakdown = WalletBreakdown(),
    val goals: FinanceGoals = FinanceGoals(),
    val currency: String = "USD",
    val recentTransactions: List<MoneyTransaction> = emptyList(),
    val openBills: List<Bill> = emptyList(),
    val overdueBills: List<Bill> = emptyList(),
    val openShoppingItems: List<ShoppingItem> = emptyList()
)

data class ReferenceEntry(
    val entryId: String,
    val category: String = "",
    val title: String = "",
    val content: String = "",
    val createdByUserId: String = "",
    val updatedAt: String = "",
    val createdAt: String = ""
)

data class AnalyticsChartData(
    val category: String,
    val expenditurePct: Double,
    val totalSpent: Double,
    val categoryType: String,
    val color: String,
    val isActive: Boolean = true
)

data class CategoryInsight(
    val category: String,
    val totalSpent: Double,
    val transactionCount: Int,
    val color: String
)

data class TransferTarget(
    val accountType: String,
    val accountName: String,
    val accountId: String,
    val balance: Double,
    val isJoint: Boolean = false
)

data class TransferRoute(
    val sourceAccount: String,
    val destinationTarget: String,
    val amount: Double,
    val direction: String,
    val balanceCheck: Boolean = false
)

data class TransferConfirmation(
    val sourceAccount: String,
    val destinationTarget: String,
    val amount: Double,
    val direction: String,
    val sourceBalance: Double,
    val destinationBalance: Double,
    val timestamp: String
)

data class SubscriptionInfo(
    val subscriptionId: String,
    val userId: String = "",
    val serviceName: String,
    val nextRenewalDate: String,
    val billingInterval: String,
    val terminationRule: String,
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String
)

data class Budget(
    val budgetId: String,
    val userId: String = "",
    val category: String,
    val month: String,
    val currentSpent: Double = 0.0,
    val budgetLimit: Double = 0.0,
    val progressPct: Int = 0,
    val status: String = "active"
)

// ===== Dashboard Section Models =====

data class GreetingData(
    val currentUser: UserProfile? = null,
    val date: String = "",
    val householdName: String = ""
)

data class TodayData(
    val tasksDueToday: List<Task> = emptyList(),
    val overdueTasks: List<Task> = emptyList(),
    val todayBills: List<Bill> = emptyList(),
    val upcomingBills: List<Bill> = emptyList(),
    val overdueBills: List<Bill> = emptyList(),
    val todayEvents: List<Event> = emptyList(),
    val quickActions: QuickActionsData = QuickActionsData()
)

data class QuickActionsData(
    val incomeThisMonth: Double = 0.0,
    val expensesThisMonth: Double = 0.0,
    val openShoppingItems: List<ShoppingItem> = emptyList(),
    val remainingBudget: Double = 0.0
)

data class MoneyData(
    val totalBalance: Double = 0.0,
    val accountBreakdown: Map<String, Double> = emptyMap(),
    val incomeThisMonth: Double = 0.0,
    val expensesThisMonth: Double = 0.0,
    val remainingBudget: Double = 0.0,
    val subscriptionTotalMonthly: Double = 0.0,
    val upcomingSubscriptions: List<SubscriptionInfo> = emptyList()
)

data class HouseholdData(
    val openTasks: List<Task> = emptyList(),
    val completedTasks: List<Task> = emptyList(),
    val maintenanceIssues: List<MaintenanceItem> = emptyList(),
    val shoppingItems: List<ShoppingItem> = emptyList(),
    val importantReminders: List<Reminder> = emptyList()
)

data class GoalsData(
    val savingsGoals: List<SavingsGoal> = emptyList(),
    val monthlyTargets: Map<String, Double> = emptyMap()
)

data class ActivityEntry(
    val title: String,
    val description: String,
    val type: String, // "task", "bill", "log", "transaction"
    val timestamp: String
)

data class ActivityFeed(
    val entries: List<ActivityEntry> = emptyList()
)

data class GamificationData(
    val xpTotal: Int = 0,
    val level: Int = 1,
    val coinsTotal: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0
)

data class SavingsGoal(
    val goalId: String,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val currency: String,
    val deadline: String,
    val priority: String,
    val status: String
)

// Reminder and Maintenance models

data class Reminder(
    val title: String,
    val description: String,
    val priority: String = "medium"
)

data class MaintenanceItem(
    val maintenanceId: String,
    val title: String,
    val category: String,
    val dueDate: String,
    val priority: String,
    val status: String = "open"
)

data class Event(
    val eventId: String = "",
    val title: String = "",
    val category: String = "",
    val start: String = "",
    val description: String = ""
)