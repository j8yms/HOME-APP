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
    val householdSide: String = ""
)

data class Task(
    val taskId: String,
    val title: String,
    val description: String,
    val category: String,
    val assignedToUserId: String,
    val createdByUserId: String,
    val status: String,
    val priority: String,
    val dueDate: String,
    val xpReward: Int,
    val coinReward: Int,
    val streakEligible: Boolean,
    val version: Int,
    val repeatRule: String = "",
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
    val couplesStreak: CouplesStreak = CouplesStreak()
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
    val createdAt: String = ""
)

data class Redemption(
    val redemptionId: String,
    val rewardId: String = "",
    val rewardTitle: String,
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
