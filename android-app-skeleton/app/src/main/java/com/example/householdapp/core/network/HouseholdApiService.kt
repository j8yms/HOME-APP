package com.example.householdapp.core.network

import com.example.householdapp.BuildConfig
import com.example.householdapp.core.model.FinanceSummary
import com.example.householdapp.core.model.UserProfile
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

interface HouseholdApiService {
    @POST
    suspend fun bootstrap(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Body request: BootstrapRequest
    ): ApiResponse<BootstrapResponse>

    @POST
    suspend fun bootstrapDevice(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Body request: DeviceBootstrapRequest
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
        @Body request: CreateTaskRequest
    ): ApiResponse<CreateTaskResponse>

    @POST
    suspend fun updateTask(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Body request: UpdateTaskRequest
    ): ApiResponse<UpdateTaskResponse>

    @POST
    suspend fun completeTask(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Body request: CompleteTaskRequest
    ): ApiResponse<CompleteTaskResponse>

    @POST
    suspend fun deleteTask(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Body request: DeleteTaskRequest
    ): ApiResponse<DeleteTaskResponse>

    @POST
    suspend fun claimTask(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Body request: ClaimTaskRequest
    ): ApiResponse<ClaimTaskResponse>

    @POST
    suspend fun logWorkout(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
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
        @Body request: CreateRewardRequest
    ): ApiResponse<CreateRewardResponse>

    @POST
    suspend fun redeemReward(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
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
        @Body request: CreateHouseholdLogRequest
    ): ApiResponse<CreateHouseholdLogResponse>

    @GET
    suspend fun listLedger(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Query("route") route: String = "history/ledger",
        @Query("userId") userId: String = ""
    ): ApiResponse<LedgerResponse>

    @POST
    suspend fun updateProfile(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
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
        @Body request: AddTransactionRequest
    ): ApiResponse<AddTransactionResponse>

    @POST
    suspend fun setFinanceGoals(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Body request: SetFinanceGoalsRequest
    ): ApiResponse<SetFinanceGoalsResponse>

    @POST
    suspend fun setFinanceCurrency(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
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
        @Body request: CreateReferenceEntryRequest
    ): ApiResponse<CreateReferenceEntryResponse>

    @POST
    suspend fun updateReferenceEntry(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Body request: UpdateReferenceEntryRequest
    ): ApiResponse<UpdateReferenceEntryResponse>

    @POST
    suspend fun deleteReferenceEntry(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
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
        @Body request: CreateBillRequest
    ): ApiResponse<CreateBillResponse>

    @POST
    suspend fun updateBill(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
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
        @Body request: CreateShoppingItemRequest
    ): ApiResponse<CreateShoppingItemResponse>

    @POST
    suspend fun updateShoppingItem(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Body request: UpdateShoppingItemRequest
    ): ApiResponse<UpdateShoppingItemResponse>
}

object ApiFactory {
    private const val CONNECT_TIMEOUT_SECONDS = 30L
    private const val READ_TIMEOUT_SECONDS = 120L
    private const val WRITE_TIMEOUT_SECONDS = 60L
    private const val CALL_TIMEOUT_SECONDS = 120L

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
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
