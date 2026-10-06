package com.example.householdapp.core.network

import com.example.householdapp.BuildConfig
import com.example.householdapp.core.model.FinanceSummary
import com.example.householdapp.core.model.UserProfile
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

interface HouseholdApiService {
    @GET
    suspend fun bootstrap(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Query("route") route: String = "auth/bootstrap",
        @Query("googleEmail") googleEmail: String
    ): ApiResponse<BootstrapResponse>

    @GET
    suspend fun bootstrapDevice(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Query("route") route: String = "auth/bootstrap_device",
        @Query("deviceId") deviceId: String
    ): ApiResponse<BootstrapResponse>

    @GET
    suspend fun getDashboard(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Query("route") route: String = "dashboard",
        @Query("userId") userId: String
    ): ApiResponse<DashboardResponse>

    @GET
    suspend fun listTasks(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Query("route") route: String = "tasks/list"
    ): ApiResponse<TasksResponse>

    @POST
    suspend fun createTask(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Query("route") route: String = "tasks/create",
        @Body request: CreateTaskRequest
    ): ApiResponse<CreateTaskResponse>

    @POST
    suspend fun updateTask(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Query("route") route: String = "tasks/update",
        @Body request: UpdateTaskRequest
    ): ApiResponse<UpdateTaskResponse>

    @POST
    suspend fun completeTask(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Query("route") route: String = "tasks/complete",
        @Body request: CompleteTaskRequest
    ): ApiResponse<CompleteTaskResponse>

    @POST
    suspend fun deleteTask(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Query("route") route: String = "tasks/delete",
        @Body request: DeleteTaskRequest
    ): ApiResponse<DeleteTaskResponse>

    @POST
    suspend fun claimTask(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Query("route") route: String = "tasks/claim",
        @Body request: ClaimTaskRequest
    ): ApiResponse<ClaimTaskResponse>

    @POST
    suspend fun logWorkout(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Query("route") route: String = "workouts/log",
        @Body request: LogWorkoutRequest
    ): ApiResponse<WorkoutResponse>

    @GET
    suspend fun listWorkouts(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Query("route") route: String = "workouts/list",
        @Query("userId") userId: String = ""
    ): ApiResponse<WorkoutsResponse>

    @GET
    suspend fun getRewards(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Query("route") route: String = "rewards",
        @Query("userId") userId: String = ""
    ): ApiResponse<RewardsResponse>

    @POST
    suspend fun createReward(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Query("route") route: String = "rewards/create",
        @Body request: CreateRewardRequest
    ): ApiResponse<CreateRewardResponse>

    @POST
    suspend fun redeemReward(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Query("route") route: String = "rewards/redeem",
        @Body request: RedeemRewardRequest
    ): ApiResponse<RedeemRewardResponse>

    @GET
    suspend fun listRedemptions(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Query("route") route: String = "rewards/redemptions",
        @Query("userId") userId: String = ""
    ): ApiResponse<RedemptionsResponse>

    @GET
    suspend fun listHouseholdLogs(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Query("route") route: String = "household/log/list",
        @Query("userId") userId: String = ""
    ): ApiResponse<HouseholdLogsResponse>

    @POST
    suspend fun createHouseholdLog(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Query("route") route: String = "household/log/create",
        @Body request: CreateHouseholdLogRequest
    ): ApiResponse<CreateHouseholdLogResponse>

    @GET
    suspend fun listLedger(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Query("route") route: String = "ledger/list",
        @Query("userId") userId: String = ""
    ): ApiResponse<LedgerResponse>

    @POST
    suspend fun updateProfile(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Query("route") route: String = "profile/update",
        @Body request: UpdateProfileRequest
    ): ApiResponse<UserProfile>

    @GET
    suspend fun getFinanceSummary(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Query("route") route: String = "finance/summary"
    ): ApiResponse<FinanceSummary>

    @POST
    suspend fun addTransaction(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Query("route") route: String = "finance/add_transaction",
        @Body request: AddTransactionRequest
    ): ApiResponse<AddTransactionResponse>

    @POST
    suspend fun setFinanceGoals(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Query("route") route: String = "finance/goals",
        @Body request: SetFinanceGoalsRequest
    ): ApiResponse<SetFinanceGoalsResponse>

    @POST
    suspend fun setFinanceCurrency(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Query("route") route: String = "finance/currency",
        @Body request: SetFinanceCurrencyRequest
    ): ApiResponse<SetFinanceCurrencyResponse>

    @GET
    suspend fun listReferenceEntries(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Query("route") route: String = "library/list",
        @Query("category") category: String = ""
    ): ApiResponse<ReferenceEntriesResponse>

    @POST
    suspend fun createReferenceEntry(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Query("route") route: String = "library/create",
        @Body request: CreateReferenceEntryRequest
    ): ApiResponse<CreateReferenceEntryResponse>

    @POST
    suspend fun updateReferenceEntry(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Query("route") route: String = "library/update",
        @Body request: UpdateReferenceEntryRequest
    ): ApiResponse<UpdateReferenceEntryResponse>

    @POST
    suspend fun deleteReferenceEntry(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Query("route") route: String = "library/delete",
        @Body request: DeleteReferenceEntryRequest
    ): ApiResponse<DeleteReferenceEntryResponse>

    @GET
    suspend fun listBills(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Query("route") route: String = "bills/list"
    ): ApiResponse<BillsResponse>

    @POST
    suspend fun createBill(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Query("route") route: String = "bills/create",
        @Body request: CreateBillRequest
    ): ApiResponse<CreateBillResponse>

    @POST
    suspend fun updateBill(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Query("route") route: String = "bills/update",
        @Body request: UpdateBillRequest
    ): ApiResponse<UpdateBillResponse>

    @GET
    suspend fun listShoppingItems(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Query("route") route: String = "shopping/list"
    ): ApiResponse<ShoppingItemsResponse>

    @POST
    suspend fun createShoppingItem(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Query("route") route: String = "shopping/create",
        @Body request: CreateShoppingItemRequest
    ): ApiResponse<CreateShoppingItemResponse>

    @POST
    suspend fun updateShoppingItem(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Query("route") route: String = "shopping/update",
        @Body request: UpdateShoppingItemRequest
    ): ApiResponse<UpdateShoppingItemResponse>

    @POST
    suspend fun executeTransfer(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Query("route") route: String = "transfers/execute",
        @Body request: ExecuteTransferRequest
    ): ApiResponse<ExecuteTransferResponse>

    @GET
    suspend fun listTransfers(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Query("route") route: String = "transfers/list",
        @Query("userId") userId: String = ""
    ): ApiResponse<TransfersResponse>

    @GET
    suspend fun getAnalytics(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Query("route") route: String = "analytics/chart",
        @Query("userId") userId: String
    ): ApiResponse<AnalyticsResponse>

    @GET
    suspend fun listBudgets(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Query("route") route: String = "budgets/list",
        @Query("userId") userId: String = ""
    ): ApiResponse<BudgetsResponse>

    @POST
    suspend fun createBudget(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Query("route") route: String = "budgets/create",
        @Body request: CreateBudgetRequest
    ): ApiResponse<CreateBudgetResponse>

    @GET
    suspend fun listSubscriptions(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Query("route") route: String = "subscriptions/list",
        @Query("userId") userId: String = ""
    ): ApiResponse<SubscriptionsResponse>

    @POST
    suspend fun createSubscription(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Query("route") route: String = "subscriptions/create",
        @Body request: CreateSubscriptionRequest
    ): ApiResponse<CreateSubscriptionResponse>

    @POST
    suspend fun updateSubscription(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Query("route") route: String = "subscriptions/update",
        @Body request: UpdateSubscriptionRequest
    ): ApiResponse<UpdateSubscriptionResponse>
}

private class JsonResponseInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        
        val contentType = response.header("Content-Type")?.lowercase().orEmpty()
        if (contentType.contains("text/html")) {
            val peek = try { response.peekBody(1024).string().lowercase() } catch (e: Exception) { "" }
            if (peek.contains("<!doctype") || peek.contains("<html")) {
                throw IOException("Server returned an HTML response instead of JSON. Check server deployment.")
            }
        }
        return response
    }
}

object ApiFactory {
    private const val CONNECT_TIMEOUT_SECONDS = 30L
    private const val READ_TIMEOUT_SECONDS = 120L
    private const val WRITE_TIMEOUT_SECONDS = 60L
    private const val CALL_TIMEOUT_SECONDS = 120L

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(JsonResponseInterceptor())
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    fun create(): HouseholdApiService {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.APPS_SCRIPT_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(
                MoshiConverterFactory.create(
                    com.squareup.moshi.Moshi.Builder()
                        .add(KotlinJsonAdapterFactory())
                        .build()
                )
            )
            .build()
            .create(HouseholdApiService::class.java)
    }
}
