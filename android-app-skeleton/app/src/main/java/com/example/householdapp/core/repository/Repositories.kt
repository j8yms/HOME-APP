package com.example.householdapp.core.repository

import java.util.UUID
import com.example.householdapp.core.model.Task
import com.example.householdapp.core.model.UserProfile
import com.example.householdapp.core.network.AddTransactionRequest
import com.example.householdapp.core.network.ApiFactory
import com.example.householdapp.core.network.BootstrapRequest
import com.example.householdapp.core.network.CompleteTaskRequest
import com.example.householdapp.core.network.ClaimTaskRequest
import com.example.householdapp.core.network.DashboardResponse
import com.example.householdapp.core.network.CreateBillRequest
import com.example.householdapp.core.network.CreateBudgetRequest
import com.example.householdapp.core.network.CreateHouseholdLogRequest
import com.example.householdapp.core.network.CreateReferenceEntryRequest
import com.example.householdapp.core.network.CreateRewardRequest
import com.example.householdapp.core.network.CreateShoppingItemRequest
import com.example.householdapp.core.network.CreateSubscriptionRequest
import com.example.householdapp.core.network.CreateTaskRequest
import com.example.householdapp.core.network.DeleteReferenceEntryRequest
import com.example.householdapp.core.network.DeleteTaskRequest
import com.example.householdapp.core.network.DeleteWealthItemRequest
import com.example.householdapp.core.network.DeviceBootstrapRequest
import com.example.householdapp.core.network.ExecuteTransferRequest
import com.example.householdapp.core.network.LogWorkoutRequest
import com.example.householdapp.core.network.RedeemRewardRequest
import com.example.householdapp.core.network.SaveFinancialFreedomRequest
import com.example.householdapp.core.network.SaveWealthItemRequest
import com.example.householdapp.core.network.SetFinanceCurrencyRequest
import com.example.householdapp.core.network.SetFinanceGoalsRequest
import com.example.householdapp.core.network.UpdateBillRequest
import com.example.householdapp.core.network.UpdateReferenceEntryRequest
import com.example.householdapp.core.network.UpdateShoppingItemRequest
import com.example.householdapp.core.network.UpdateSubscriptionRequest
import com.example.householdapp.core.network.UpdateTaskRequest
import com.example.householdapp.core.network.UpdateProfileRequest
import com.example.householdapp.core.network.WealthItem

class AuthRepository {
    private val api = ApiFactory.create()

    suspend fun bootstrap(email: String) =
        api.bootstrap(googleEmail = email)

    suspend fun bootstrapDevice(deviceId: String) =
        api.bootstrapDevice(deviceId = deviceId)
}

class DashboardRepository {
    private val api = ApiFactory.create()

    suspend fun getDashboard(userId: String) =
        api.getDashboard(userId = userId)
}

class TaskRepository {
    private val api = ApiFactory.create()

    suspend fun listTasks() = api.listTasks()

    suspend fun createTask(request: CreateTaskRequest) = api.createTask(request = request)

    suspend fun updateTask(request: UpdateTaskRequest) = api.updateTask(request = request)

    suspend fun completeTask(taskId: String, userId: String) =
        api.completeTask(request = CompleteTaskRequest(taskId = taskId, completedByUserId = userId))

    suspend fun deleteTask(taskId: String, version: Int) =
        api.deleteTask(request = DeleteTaskRequest(taskId = taskId, version = version))

    suspend fun claimTask(taskId: String, userId: String) =
        api.claimTask(request = ClaimTaskRequest(taskId = taskId, claimedByUserId = userId))
}

class WorkoutRepository {
    private val api = ApiFactory.create()

    suspend fun listWorkouts(userId: String) = api.listWorkouts(userId = userId)

    suspend fun logWorkout(userId: String, workoutType: String, durationMinutes: Int, intensity: String, notes: String, bothPartners: Boolean = false) =
        api.logWorkout(request = LogWorkoutRequest(
            userId = userId,
            workoutType = workoutType,
            durationMinutes = durationMinutes,
            intensity = intensity,
            notes = notes,
            bothPartners = bothPartners
        ))
}

class HistoryRepository {
    private val api = ApiFactory.create()

    suspend fun getLedger(userId: String = "") = api.listLedger(userId = userId)
}

class ProfileRepository {
    private val api = ApiFactory.create()

    suspend fun updateDisplayName(userId: String, displayName: String, householdSide: String = "") =
        api.updateProfile(
            request = UpdateProfileRequest(
                userId = userId,
                displayName = displayName,
                householdSide = householdSide
            )
        )
}

class RewardsRepository {
    private val api = ApiFactory.create()

    suspend fun getRewards(userId: String = "") = api.getRewards(userId = userId)

    suspend fun createReward(userId: String, title: String, description: String = "", costCoins: Int = 0, costXp: Int = 0, category: String = "", hideFromPartner: Boolean = false) =
        api.createReward(
            request = CreateRewardRequest(
                userId = userId,
                title = title,
                description = description,
                costCoins = costCoins,
                costXp = costXp,
                category = category,
                hideFromPartner = hideFromPartner
            )
        )

    suspend fun listRedemptions(userId: String = "") = api.listRedemptions(userId = userId)

    suspend fun redeemReward(userId: String, rewardId: String, notes: String = "") =
        api.redeemReward(request = RedeemRewardRequest(
            rewardId = rewardId,
            userId = userId,
            notes = notes
        ))
}

class HouseholdLogRepository {
    private val api = ApiFactory.create()

    suspend fun listLogs(userId: String = "") = api.listHouseholdLogs(userId = userId)

    suspend fun createLog(userId: String, type: String, title: String, details: String = "") =
        api.createHouseholdLog(
            request = CreateHouseholdLogRequest(
                userId = userId,
                type = type,
                title = title,
                details = details
            )
        )
}

class FinanceRepository {
    private val api = ApiFactory.create()

    suspend fun getSummary() = api.getFinanceSummary()

    suspend fun addTransaction(type: String, description: String, category: String, amount: Double, wallet: String, userId: String) =
        api.addTransaction(
            request = AddTransactionRequest(
                type = type,
                description = description,
                category = category,
                amount = amount,
                wallet = wallet,
                userId = userId,
                requestId = UUID.randomUUID().toString()
            )
        )

    suspend fun setGoals(vacationGoal: Double, dreamGoal: Double) =
        api.setFinanceGoals(
            request = SetFinanceGoalsRequest(
                vacationGoal = vacationGoal,
                dreamGoal = dreamGoal
            )
        )

    suspend fun setCurrency(currency: String) =
        api.setFinanceCurrency(
            request = SetFinanceCurrencyRequest(
                currency = currency
            )
        )

    suspend fun listBills() = api.listBills()

    suspend fun createBill(title: String, category: String, amount: Double, dueDate: String, userId: String) =
        api.createBill(
            request = CreateBillRequest(
                title = title,
                category = category,
                amount = amount,
                dueDate = dueDate,
                createdByUserId = userId
            )
        )

    suspend fun markBillPaid(billId: String, userId: String) =
        api.updateBill(
            request = UpdateBillRequest(
                billId = billId,
                status = "paid",
                paidByUserId = userId
            )
        )

    suspend fun listShoppingItems() = api.listShoppingItems()

    suspend fun createShoppingItem(title: String, category: String, estimatedCost: Double, userId: String) =
        api.createShoppingItem(
            request = CreateShoppingItemRequest(
                title = title,
                category = category,
                estimatedCost = estimatedCost,
                addedByUserId = userId
            )
        )

    suspend fun markItemPurchased(itemId: String, userId: String) =
        api.updateShoppingItem(
            request = UpdateShoppingItemRequest(
                itemId = itemId,
                status = "purchased",
                purchasedByUserId = userId
            )
        )

    suspend fun executeTransfer(sourceAccount: String, destinationTarget: String, amount: Double, userId: String) =
        api.executeTransfer(
            request = ExecuteTransferRequest(
                sourceAccount = sourceAccount,
                destinationTarget = destinationTarget,
                amount = amount,
                direction = "out",
                userId = userId
            )
        )

    suspend fun listTransfers(userId: String) = api.listTransfers(userId = userId)

    suspend fun getLedgerEntries(userId: String = "") = api.listLedger(userId = userId)

    suspend fun getAnalytics(userId: String) = api.getAnalytics(userId = userId)

    suspend fun listBudgets(userId: String = "") = api.listBudgets(userId = userId)

    suspend fun createBudget(userId: String, category: String, month: String, budgetLimit: Double) =
        api.createBudget(
            request = CreateBudgetRequest(
                userId = userId,
                category = category,
                month = month,
                budgetLimit = budgetLimit
            )
        )

    suspend fun listSubscriptions(userId: String = "") = api.listSubscriptions(userId = userId)

    suspend fun createSubscription(userId: String, serviceName: String, nextRenewalDate: String, billingInterval: String = "", terminationRule: String = "") =
        api.createSubscription(
            request = CreateSubscriptionRequest(
                userId = userId,
                serviceName = serviceName,
                nextRenewalDate = nextRenewalDate,
                billingInterval = billingInterval,
                terminationRule = terminationRule
            )
        )

    suspend fun updateSubscription(subscriptionId: String, nextRenewalDate: String? = null, isActive: Boolean? = null) =
        api.updateSubscription(
            request = UpdateSubscriptionRequest(
                subscriptionId = subscriptionId,
                nextRenewalDate = nextRenewalDate,
                isActive = isActive
            )
        )
}

class WealthRepository {
    private val api = ApiFactory.create()

    suspend fun getSummary(userId: String) = api.getWealthSummary(userId = userId)

    suspend fun listSection(userId: String, section: String, investmentId: String = "") =
        api.listWealthSection(section = section, userId = userId, investmentId = investmentId)

    suspend fun saveItem(userId: String, section: String, item: WealthItem) =
        api.saveWealthItem(
            request = SaveWealthItemRequest(
                section = section,
                item = item,
                userId = userId
            )
        )

    suspend fun deleteItem(userId: String, section: String, id: String) =
        api.deleteWealthItem(
            request = DeleteWealthItemRequest(
                section = section,
                id = id,
                userId = userId
            )
        )

    suspend fun getHistory(userId: String) = api.getWealthHistory(userId = userId)

    suspend fun getFreedom() = api.getFinancialFreedom()

    suspend fun saveFreedom(request: SaveFinancialFreedomRequest) =
        api.saveFinancialFreedom(request = request)
}

class ReferenceRepository {
    private val api = ApiFactory.create()

    suspend fun listEntries(category: String = "") = api.listReferenceEntries(category = category)

    suspend fun createEntry(category: String, title: String, content: String, userId: String) =
        api.createReferenceEntry(
            request = CreateReferenceEntryRequest(
                category = category,
                title = title,
                content = content,
                userId = userId
            )
        )

    suspend fun updateEntry(entryId: String, title: String, content: String) =
        api.updateReferenceEntry(
            request = UpdateReferenceEntryRequest(
                entryId = entryId,
                title = title,
                content = content
            )
        )

    suspend fun deleteEntry(entryId: String) =
        api.deleteReferenceEntry(request = DeleteReferenceEntryRequest(entryId = entryId))
}

object PlaceholderData {
    val demoUser = UserProfile(
        userId = "u_001",
        email = "alex@example.com",
        displayName = "Alex",
        role = "member",
        xpTotal = 120,
        level = 2,
        coinsTotal = 35,
        currentStreak = 3,
        longestStreak = 7
    )

    val demoTasks = listOf(
        Task(
            taskId = "t_001",
            title = "Take out trash",
            description = "Before 8 PM",
            category = "Cleaning",
            assignedToUserId = "u_001",
            createdByUserId = "u_002",
            status = "open",
            priority = "medium",
            dueDate = "2026-06-08",
            repeatRule = null,
            xpReward = 20,
            coinReward = 5,
            streakEligible = true,
            version = 1
        ),
        Task(
            taskId = "t_002",
            title = "Laundry",
            description = "One full cycle",
            category = "Cleaning",
            assignedToUserId = "u_002",
            createdByUserId = "u_001",
            status = "open",
            priority = "high",
            dueDate = "2026-06-08",
            repeatRule = null,
            xpReward = 35,
            coinReward = 8,
            streakEligible = true,
            version = 1
        )
    )
}
