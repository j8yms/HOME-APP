package com.example.householdapp.core.network

import com.example.householdapp.BuildConfig
import com.example.householdapp.core.model.FinanceSummary
import com.example.householdapp.core.model.UserProfile
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.IOException
import java.nio.charset.Charset
import java.util.UUID
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.Buffer
import org.json.JSONObject
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

    @GET
    suspend fun getWealthSummary(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Query("route") route: String = "wealth/summary",
        @Query("userId") userId: String
    ): ApiResponse<WealthSummaryResponse>

    @GET
    suspend fun listWealthSection(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Query("route") route: String = "wealth/list",
        @Query("section") section: String,
        @Query("userId") userId: String,
        @Query("investmentId") investmentId: String = ""
    ): ApiResponse<WealthItemsResponse>

    @POST
    suspend fun saveWealthItem(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Query("route") route: String = "wealth/save",
        @Body request: SaveWealthItemRequest
    ): ApiResponse<SaveWealthItemResponse>

    @POST
    suspend fun deleteWealthItem(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Query("route") route: String = "wealth/delete",
        @Body request: DeleteWealthItemRequest
    ): ApiResponse<DeleteWealthItemResponse>

    @GET
    suspend fun getWealthHistory(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Query("route") route: String = "wealth/history",
        @Query("userId") userId: String
    ): ApiResponse<WealthHistoryResponse>

    @GET
    suspend fun getFinancialFreedom(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Query("route") route: String = "wealth/freedom/get",
        @Query("userId") userId: String = ""
    ): ApiResponse<FinancialFreedom>

    @POST
    suspend fun saveFinancialFreedom(
        @Url path: String = BuildConfig.APPS_SCRIPT_DEPLOYMENT_PATH,
        @Query("route") route: String = "wealth/freedom/save",
        @Body request: SaveFinancialFreedomRequest
    ): ApiResponse<FinancialFreedom>

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

/**
 * Makes calls to the Apps Script deployment safe to retry:
 *
 * - Stamps a unique requestId on every POST body so the server can replay
 *   the first successful response instead of running a mutation twice.
 * - Limits how many requests run at the same time (the Google redirect hop
 *   fails intermittently under bursts).
 * - Retries transient failures (connection errors, HTML error pages from the
 *   redirect hop, 408/429/5xx) with a short backoff.
 */
private class ResilientAppsScriptInterceptor : Interceptor {
    private val inFlight = Semaphore(MAX_CONCURRENT_REQUESTS)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = withRequestId(chain.request())
        inFlight.acquire()
        try {
            var attempt = 1
            while (true) {
                if (attempt > 1) {
                    Thread.sleep(RETRY_BACKOFF_MS * attempt)
                }
                val response = try {
                    chain.proceed(request)
                } catch (io: IOException) {
                    if (attempt >= MAX_ATTEMPTS) throw io
                    attempt += 1
                    continue
                }
                if (attempt < MAX_ATTEMPTS && isRetryable(response)) {
                    response.close()
                    attempt += 1
                    continue
                }
                return response
            }
        } finally {
            inFlight.release()
        }
    }

    private fun isRetryable(response: Response): Boolean {
        val code = response.code()
        if (code == 408 || code == 429) return true
        if (code in 500..599) return true
        if (code in 200..499) {
            val peek = try { response.peekBody(2048).string() } catch (e: Exception) { "" }
            // Google's redirect chain can re-run the deployment as a GET and
            // drop the POST body; the server flags that so we can retry once.
            if (peek.contains("BODY_REQUIRED")) return true
            if (code in 400..499 && peek.contains("<!doctype", ignoreCase = true)) return true
        }
        return false
    }

    private fun withRequestId(request: Request): Request {
        if (request.method() != "POST") return request
        val body = request.body() ?: return request

        val contentType = body.contentType()
        val charset = contentType?.charset(UTF_8) ?: UTF_8
        val original = try {
            val buffer = Buffer()
            body.writeTo(buffer)
            buffer.readString(charset)
        } catch (e: Exception) {
            return request
        }
        val json = try {
            JSONObject(original)
        } catch (e: Exception) {
            return request
        }
        if (json.optString("requestId", "").isNotBlank()) return request

        json.put("requestId", UUID.randomUUID().toString())
        val newBody = RequestBody.create(contentType, json.toString())
        return request.newBuilder().method(request.method(), newBody).build()
    }

    companion object {
        private const val MAX_ATTEMPTS = 3
        private const val RETRY_BACKOFF_MS = 300L
        private const val MAX_CONCURRENT_REQUESTS = 3
        private val UTF_8 = Charset.forName("UTF-8")
    }
}

object ApiFactory {
    private const val CONNECT_TIMEOUT_SECONDS = 30L
    private const val READ_TIMEOUT_SECONDS = 120L
    private const val WRITE_TIMEOUT_SECONDS = 60L
    private const val CALL_TIMEOUT_SECONDS = 120L

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(ResilientAppsScriptInterceptor())
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
