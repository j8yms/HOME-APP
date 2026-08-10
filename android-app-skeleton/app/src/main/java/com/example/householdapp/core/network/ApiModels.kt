package com.example.householdapp.core.network

import com.example.householdapp.core.model.Bill
import com.example.householdapp.core.model.CouplesStreak
import com.example.householdapp.core.model.FinanceSummary
import com.example.householdapp.core.model.HouseholdLogEntry
import com.example.householdapp.core.model.LedgerEntry
import com.example.householdapp.core.model.MoneyTransaction
import com.example.householdapp.core.model.Redemption
import com.example.householdapp.core.model.ReferenceEntry
import com.example.householdapp.core.model.RewardItem
import com.example.householdapp.core.model.ShoppingItem
import com.example.householdapp.core.model.Task
import com.example.householdapp.core.model.UserProfile
import com.example.householdapp.core.model.WorkoutEntry

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ApiError? = null
)

data class ApiError(
    val code: String,
    val message: String
)

data class BootstrapRequest(
    val route: String = "auth/bootstrap",
    val googleEmail: String
)

data class BootstrapResponse(
    val authorized: Boolean,
    val reason: String? = null,
    val user: UserProfile? = null,
    val partner: UserProfile? = null
)

data class DeviceBootstrapRequest(
    val route: String = "auth/bootstrap_device",
    val deviceId: String
)

data class DashboardResponse(
    val currentUser: UserProfile? = null,
    val partner: UserProfile? = null,
    val tasksDueToday: List<Task> = emptyList(),
    val overdueTasks: List<Task> = emptyList(),
    val couplesStreak: CouplesStreak = CouplesStreak()
)

data class TasksResponse(
    val tasks: List<Task> = emptyList()
)

data class CreateTaskRequest(
    val route: String = "tasks/create",
    val title: String,
    val description: String,
    val category: String,
    val assignedToUserId: String,
    val assigneeLabel: String = "",
    val createdByUserId: String,
    val priority: String,
    val dueDate: String,
    val repeatRule: String = "",
    val streakEligible: Boolean = true
)

data class CreateTaskResponse(
    val task: Task
)

data class UpdateTaskRequest(
    val route: String = "tasks/update",
    val taskId: String,
    val title: String,
    val description: String,
    val category: String,
    val assignedToUserId: String,
    val assigneeLabel: String = "",
    val priority: String,
    val dueDate: String,
    val repeatRule: String = "",
    val streakEligible: Boolean = true,
    val version: Int
)

data class UpdateTaskResponse(
    val task: Task
)

data class CompleteTaskRequest(
    val route: String = "tasks/complete",
    val taskId: String,
    val completedByUserId: String
)

data class CompleteTaskResponse(
    val task: Task,
    val user: UserProfile,
    val recurringTask: Task? = null
)

data class DeleteTaskRequest(
    val route: String = "tasks/delete",
    val taskId: String,
    val version: Int
)

data class DeleteTaskResponse(
    val task: Task
)

data class ClaimTaskRequest(
    val route: String = "tasks/claim",
    val taskId: String,
    val claimedByUserId: String
)

data class ClaimTaskResponse(
    val task: Task,
    val user: UserProfile? = null
)

data class LogWorkoutRequest(
    val route: String = "workouts/log",
    val userId: String,
    val workoutType: String,
    val durationMinutes: Int,
    val intensity: String,
    val notes: String,
    val bothPartners: Boolean = false
)

data class WorkoutResponse(
    val workoutId: String,
    val xpReward: Int,
    val coinReward: Int,
    val user: UserProfile,
    val loggedForBoth: Boolean = false,
    val partnerWorkoutId: String = "",
    val partnerXpReward: Int = 0,
    val partnerCoinReward: Int = 0,
    val partnerUser: UserProfile? = null
)

data class WorkoutsResponse(
    val workouts: List<WorkoutEntry> = emptyList()
)

data class RewardsResponse(
    val rewards: List<RewardItem> = emptyList()
)

data class CreateRewardRequest(
    val route: String = "rewards/create",
    val title: String,
    val description: String = "",
    val costCoins: Int = 0,
    val costXp: Int = 0,
    val category: String = "",
    val hideFromPartner: Boolean = false,
    val userId: String
)

data class CreateRewardResponse(
    val reward: RewardItem
)

data class RedeemRewardRequest(
    val route: String = "rewards/redeem",
    val rewardId: String,
    val userId: String,
    val notes: String = ""
)

data class RedeemRewardResponse(
    val reward: RewardItem,
    val redemptionId: String,
    val user: UserProfile
)

data class RedemptionsResponse(
    val redemptions: List<Redemption> = emptyList()
)

data class HouseholdLogsResponse(
    val logs: List<HouseholdLogEntry> = emptyList()
)

data class CreateHouseholdLogRequest(
    val route: String = "household/log/create",
    val userId: String,
    val type: String,
    val title: String,
    val details: String = "",
    val relatedTaskId: String = ""
)

data class CreateHouseholdLogResponse(
    val log: HouseholdLogEntry,
    val user: UserProfile
)

data class LedgerResponse(
    val entries: List<LedgerEntry> = emptyList()
)

data class UpdateProfileRequest(
    val route: String = "profile/update",
    val userId: String,
    val displayName: String,
    val householdSide: String = ""
)

data class AddTransactionRequest(
    val route: String = "finance/add_transaction",
    val type: String,
    val description: String,
    val category: String = "",
    val amount: Double,
    val wallet: String = "joint",
    val userId: String
)

data class AddTransactionResponse(
    val transaction: MoneyTransaction,
    val summary: FinanceSummary
)

data class SetFinanceGoalsRequest(
    val route: String = "finance/goals",
    val vacationGoal: Double,
    val dreamGoal: Double
)

data class SetFinanceGoalsResponse(
    val vacationGoal: Double = 0.0,
    val dreamGoal: Double = 0.0
)

data class SetFinanceCurrencyRequest(
    val route: String = "finance/currency",
    val currency: String
)

data class SetFinanceCurrencyResponse(
    val currency: String = "USD"
)

data class BillsResponse(
    val bills: List<Bill> = emptyList()
)

data class CreateBillRequest(
    val route: String = "bills/create",
    val title: String,
    val category: String = "",
    val amount: Double,
    val dueDate: String,
    val createdByUserId: String
)

data class CreateBillResponse(
    val bill: Bill
)

data class UpdateBillRequest(
    val route: String = "bills/update",
    val billId: String,
    val status: String,
    val paidByUserId: String = ""
)

data class UpdateBillResponse(
    val bill: Bill
)

data class ShoppingItemsResponse(
    val items: List<ShoppingItem> = emptyList()
)

data class CreateShoppingItemRequest(
    val route: String = "shopping/create",
    val title: String,
    val category: String = "",
    val estimatedCost: Double = 0.0,
    val addedByUserId: String
)

data class CreateShoppingItemResponse(
    val item: ShoppingItem
)

data class UpdateShoppingItemRequest(
    val route: String = "shopping/update",
    val itemId: String,
    val status: String,
    val purchasedByUserId: String = ""
)

data class UpdateShoppingItemResponse(
    val item: ShoppingItem
)

data class ReferenceEntriesResponse(
    val entries: List<ReferenceEntry> = emptyList()
)

data class CreateReferenceEntryRequest(
    val route: String = "library/create",
    val category: String,
    val title: String,
    val content: String = "",
    val userId: String
)

data class CreateReferenceEntryResponse(
    val entry: ReferenceEntry
)

data class UpdateReferenceEntryRequest(
    val route: String = "library/update",
    val entryId: String,
    val title: String,
    val content: String = ""
)

data class UpdateReferenceEntryResponse(
    val entry: ReferenceEntry
)

data class DeleteReferenceEntryRequest(
    val route: String = "library/delete",
    val entryId: String
)

data class DeleteReferenceEntryResponse(
    val entryId: String,
    val deleted: Boolean = false
)
