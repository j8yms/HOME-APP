param(
    [string]$BaseUrl = "https://script.google.com/macros/s/AKfycbySdX4LCqpZVaH6N0a-CuTE0kh9qC5BCmGBE2JnUsl3j9O7XtM7rS2Jg46idWkJ_gD3TQ/exec"
)

$ErrorActionPreference = "Stop"
$script:passCount = 0
$script:failCount = 0
$script:failures = @()

function Pass([string]$name) {
    $script:passCount++
    Write-Host "PASS  $name" -ForegroundColor Green
}

function Fail([string]$name, [string]$reason) {
    $script:failCount++
    $script:failures += "$name :: $reason"
    Write-Host "FAIL  $name :: $reason" -ForegroundColor Red
}

function Invoke-Exec {
    param([string]$Route, [string]$Method = "GET", [string]$JsonBody)
    $parts = $Route -split '&', 2
    $query = "?route=" + [uri]::EscapeDataString($parts[0])
    if ($parts.Count -gt 1) { $query += "&" + $parts[1] }
    $uri = $script:BaseUrl + $query
    $lastError = $null
    for ($attempt = 1; $attempt -le 5; $attempt++) {
        try {
            if ($Method -eq "POST") {
                $resp = Invoke-WebRequest -Uri $uri -Method Post -ContentType "application/json; charset=utf-8" -Body $JsonBody -UseBasicParsing -TimeoutSec 120
            } else {
                $resp = Invoke-WebRequest -Uri $uri -Method Get -UseBasicParsing -TimeoutSec 120
            }
            $content = [string]$resp.Content
            # Server flags requests whose body was dropped by Google's redirect
            # chain (macro ran as GET / body failed to parse) -> safe to retry.
            if ($Method -eq "POST" -and $content -like "*BODY_REQUIRED*" -and $attempt -lt 5) {
                Start-Sleep -Milliseconds (400 * $attempt)
                continue
            }
            return $content
        } catch {
            $lastError = $_
            $status = $null
            if ($_.Exception.Response) { $status = [int]$_.Exception.Response.StatusCode }
            $retryable = ($status -eq 404 -or $status -eq 408 -or $status -eq 429 -or $status -ge 500 -or $null -eq $status)
            if (-not $retryable -or $attempt -eq 5) { throw }
            Start-Sleep -Milliseconds (400 * $attempt)
        }
    }
    throw $lastError
}

function Get-Json {
    param([string]$Route, [string]$Method = "GET", [string]$JsonBody, [string]$Name, [bool]$ExpectSuccess = $true)
    $content = $null
    try {
        $content = Invoke-Exec -Route $Route -Method $Method -JsonBody $JsonBody
    } catch {
        Fail $Name "transport error: $($_.Exception.Message)"
        return $null
    }
    $lower = $content.TrimStart().Substring(0, [Math]::Min(30, $content.TrimStart().Length)).ToLower()
    if ($lower.StartsWith("<")) {
        Fail $Name "HTML response instead of JSON: $($content.Substring(0, [Math]::Min(160, $content.Length)))"
        return $null
    }
    $json = $null
    try { $json = $content | ConvertFrom-Json } catch { }
    if ($null -eq $json) {
        Fail $Name "not parseable JSON: $($content.Substring(0, [Math]::Min(160, $content.Length)))"
        return $null
    }
    if ($null -eq $json.PSObject.Properties["success"]) {
        Fail $Name "missing success envelope"
        return $null
    }
    if ($ExpectSuccess -and -not $json.success) {
        Fail $Name "success=false code=$($json.error.code) message=$($json.error.message)"
        return $json
    }
    Pass $Name
    return $json
}

function Assert-True {
    param([string]$Name, [bool]$Condition, [string]$Reason)
    if ($Condition) { Pass $Name } else { Fail $Name $Reason }
}

$today = Get-Date -Format "yyyy-MM-dd"
$month = Get-Date -Format "yyyy-MM"
$renewal = (Get-Date).AddDays(30).ToString("yyyy-MM-dd")
$requestId = "ct-" + [guid]::NewGuid().ToString("N")

Write-Host "=== Phase A: schema migration ===" -ForegroundColor Cyan
$migrate = Get-Json -Route "setup/migrate" -Name "setup/migrate"
$schema = Get-Json -Route "setup/schema" -Name "setup/schema"
if ($schema -and $schema.data -and $schema.data.sheets) {
    $sheetNames = @($schema.data.sheets | ForEach-Object { $_.name })
    foreach ($required in @("Request_Log", "Investments", "Trade_Journal", "Investment_Transactions", "Passive_Income", "Prop_Accounts", "Prop_Payouts", "Transfers", "Budgets", "Net_Worth_Snapshots")) {
        Assert-True -Name "schema has sheet $required" -Condition ($sheetNames -contains $required) -Reason "sheet missing"
    }
    $usersSheet = $schema.data.sheets | Where-Object { $_.name -eq "Users" }
    if ($usersSheet) {
        Assert-True -Name "schema Users.household_id" -Condition ($usersSheet.headers -contains "household_id") -Reason "household_id column missing"
    }
    $budgetsSheet = $schema.data.sheets | Where-Object { $_.name -eq "Budgets" }
    if ($budgetsSheet) {
        Assert-True -Name "schema Budgets.budget_limit" -Condition ($budgetsSheet.headers -contains "budget_limit") -Reason "budget_limit column missing (legacy 'limit' only?)"
    }
}

Write-Host "=== Phase B: discover a real userId ===" -ForegroundColor Cyan
$candidates = @()
$tasks = Get-Json -Route "tasks/list" -Name "tasks/list"
if ($tasks -and $tasks.data -and $tasks.data.tasks) {
    foreach ($t in $tasks.data.tasks) {
        if ($t.PSObject.Properties["createdByUserId"] -and $t.createdByUserId) { $candidates += $t.createdByUserId }
        if ($t.PSObject.Properties["assignedToUserId"] -and $t.assignedToUserId) { $candidates += $t.assignedToUserId }
    }
}
$ledger = Get-Json -Route "history/ledger" -Name "history/ledger"
if ($ledger -and $ledger.data -and $ledger.data.entries) {
    foreach ($e in $ledger.data.entries) {
        if ($e.PSObject.Properties["userId"] -and $e.userId) { $candidates += $e.userId }
    }
}
$logs = Get-Json -Route "household/log/list" -Name "household/log/list"
if ($logs -and $logs.data -and $logs.data.logs) {
    foreach ($l in $logs.data.logs) {
        if ($l.PSObject.Properties["userId"] -and $l.userId) { $candidates += $l.userId }
    }
}
$candidates = @($candidates | Sort-Object -Unique | Where-Object { $_ })
$userId = ""
foreach ($candidate in $candidates) {
    $probe = Get-Json -Route "dashboard&userId=$([uri]::EscapeDataString($candidate))" -Name "dashboard probe ($candidate)" -ExpectSuccess $false
    if ($probe -and $probe.success) { $userId = $candidate; break }
}
if ($userId) { Pass "discovered valid userId ($userId)" } else { Fail "discovered userId" "no candidate passed dashboard probe; candidates=$($candidates -join ',')" }

Write-Host "=== Phase C: read routes ===" -ForegroundColor Cyan
$health = Get-Json -Route "health" -Name "health"
$dashboard = Get-Json -Route "dashboard&userId=$([uri]::EscapeDataString($userId))" -Name "dashboard"
if ($dashboard -and $dashboard.data) {
    $d = $dashboard.data
    Assert-True -Name "dashboard has currentUser" -Condition ($null -ne $d.currentUser) -Reason "currentUser missing"
    $entries = @()
    if ($d.PSObject.Properties["activity"] -and $d.activity -and $d.activity.PSObject.Properties["entries"]) { $entries = @($d.activity.entries) }
    Assert-True -Name "dashboard activity is array" -Condition ($null -ne $entries) -Reason "activity.entries missing"
    $titlesOk = $true
    foreach ($entry in $entries) {
        if (-not ($entry.PSObject.Properties["title"] -and $entry.title)) { $titlesOk = $false }
    }
    Assert-True -Name "dashboard activity entries all have title" -Condition $titlesOk -Reason "an activity entry is missing title"
    Assert-True -Name "dashboard has money" -Condition ($null -ne $d.money) -Reason "money missing"
    Assert-True -Name "dashboard has goals" -Condition ($null -ne $d.goals) -Reason "goals missing"
    Assert-True -Name "dashboard has gamification" -Condition ($null -ne $d.gamification) -Reason "gamification missing"
}

$ledger = Get-Json -Route "ledger/list&userId=$([uri]::EscapeDataString($userId))" -Name "ledger/list"
$finance = Get-Json -Route "finance/summary&userId=$([uri]::EscapeDataString($userId))" -Name "finance/summary"
$budgets = Get-Json -Route "budgets/list&userId=$([uri]::EscapeDataString($userId))" -Name "budgets/list"
$subs = Get-Json -Route "subscriptions/list&userId=$([uri]::EscapeDataString($userId))" -Name "subscriptions/list"
$transfers = Get-Json -Route "transfers/list&userId=$([uri]::EscapeDataString($userId))" -Name "transfers/list"
$analytics = Get-Json -Route "analytics/chart&userId=$([uri]::EscapeDataString($userId))" -Name "analytics/chart"
if ($analytics -and $analytics.data) {
    Assert-True -Name "analytics has overview" -Condition ($null -ne $analytics.data.overview) -Reason "overview missing"
    Assert-True -Name "analytics has categories" -Condition ($null -ne $analytics.data.categories) -Reason "categories missing"
}
$wealth = Get-Json -Route "wealth/summary&userId=$([uri]::EscapeDataString($userId))" -Name "wealth/summary"
if ($wealth -and $wealth.data) {
    foreach ($field in @("currency", "totals", "counts", "milestones", "freedom")) {
        Assert-True -Name "wealth/summary.$field" -Condition ($null -ne $wealth.data.PSObject.Properties[$field]) -Reason "$field missing"
    }
}
$freedom = Get-Json -Route "wealth/freedom/get&userId=$([uri]::EscapeDataString($userId))" -Name "wealth/freedom/get"
$history = Get-Json -Route "wealth/history&userId=$([uri]::EscapeDataString($userId))" -Name "wealth/history"
foreach ($section in @("investments", "assets", "liabilities", "milestones", "income", "accounts", "trades", "contributions", "propAccounts", "propPayouts")) {
    $list = Get-Json -Route "wealth/list&section=$section&userId=$([uri]::EscapeDataString($userId))" -Name "wealth/list $section"
    if ($list -and $list.data) {
        Assert-True -Name "wealth/list $section has items array" -Condition ($null -ne $list.data.items) -Reason "items missing"
    }
}
$bills = Get-Json -Route "bills/list" -Name "bills/list"
$shopping = Get-Json -Route "shopping/list" -Name "shopping/list"
$library = Get-Json -Route "library/list" -Name "library/list"
$rewards = Get-Json -Route "rewards" -Name "rewards"
$redemptions = Get-Json -Route "rewards/redemptions&userId=$([uri]::EscapeDataString($userId))" -Name "rewards/redemptions"
$workouts = Get-Json -Route "workouts/list&userId=$([uri]::EscapeDataString($userId))" -Name "workouts/list"
$netWorthHistory = Get-Json -Route "finance/net_worth_history" -Name "finance/net_worth_history"
$currency = Get-Json -Route "finance/currency&userId=$([uri]::EscapeDataString($userId))" -Name "finance/currency (GET read)"

Write-Host "=== Phase D: mutation routes ===" -ForegroundColor Cyan

$budgetBody = @{ route = "budgets/create"; userId = $userId; category = "ContractTest"; month = $month; budgetLimit = 100; requestId = "ct-" + [guid]::NewGuid().ToString("N") } | ConvertTo-Json
$budgetCreate = Get-Json -Route "budgets/create" -Method POST -JsonBody $budgetBody -Name "budgets/create"
$budgetId = $null
if ($budgetCreate -and $budgetCreate.data -and $budgetCreate.data.budget) { $budgetId = $budgetCreate.data.budget.budgetId }
if ($budgetId) {
    Pass "budgets/create returned budgetId ($budgetId)"
    $budgetUpdateBody = @{ route = "budgets/update"; userId = $userId; budgetId = $budgetId; budgetLimit = 150; requestId = "ct-" + [guid]::NewGuid().ToString("N") } | ConvertTo-Json
    $budgetUpdate = Get-Json -Route "budgets/update" -Method POST -JsonBody $budgetUpdateBody -Name "budgets/update"
    $budgetsAfter = Get-Json -Route "budgets/list&userId=$([uri]::EscapeDataString($userId))" -Name "budgets/list after update"
    if ($budgetsAfter -and $budgetsAfter.data -and $budgetsAfter.data.budgets) {
        $found = @($budgetsAfter.data.budgets | Where-Object { $_.budgetId -eq $budgetId -and $_.budgetLimit -eq 150 })
        Assert-True -Name "budgets/update persisted budgetLimit=150" -Condition ($found.Count -gt 0) -Reason "budget not updated"
    }
} else {
    Fail "budgets/create returned budgetId" "no budget in data"
}

$subBody = @{ route = "subscriptions/create"; userId = $userId; serviceName = "ContractTest Sub"; nextRenewalDate = $renewal; billingInterval = "monthly"; terminationRule = "cancel"; requestId = "ct-" + [guid]::NewGuid().ToString("N") } | ConvertTo-Json
$subCreate = Get-Json -Route "subscriptions/create" -Method POST -JsonBody $subBody -Name "subscriptions/create"
$subId = $null
if ($subCreate -and $subCreate.data -and $subCreate.data.subscription) { $subId = $subCreate.data.subscription.subscriptionId }
if ($subId) {
    $subUpdateBody = @{ route = "subscriptions/update"; userId = $userId; subscriptionId = $subId; isActive = $false; requestId = "ct-" + [guid]::NewGuid().ToString("N") } | ConvertTo-Json
    Get-Json -Route "subscriptions/update" -Method POST -JsonBody $subUpdateBody -Name "subscriptions/update (deactivate)"
} else {
    Fail "subscriptions/create returned subscriptionId" "no subscription in data"
}

$wealthBody = @{
    route = "wealth/save"
    userId = $userId
    section = "investments"
    item = @{ name = "ContractTest Fund"; type = "test"; provider = "QA"; value = 1234.56; yieldPct = 5.5; isActive = $true }
    requestId = "ct-" + [guid]::NewGuid().ToString("N")
} | ConvertTo-Json -Depth 5
$wealthSave = Get-Json -Route "wealth/save" -Method POST -JsonBody $wealthBody -Name "wealth/save (investments)"
$wealthItemId = $null
if ($wealthSave -and $wealthSave.data -and $wealthSave.data.item) { $wealthItemId = $wealthSave.data.item.id }
if ($wealthItemId) {
    Pass "wealth/save returned item id ($wealthItemId)"
    $wealthList = Get-Json -Route "wealth/list&section=investments&userId=$([uri]::EscapeDataString($userId))" -Name "wealth/list after save"
    if ($wealthList -and $wealthList.data) {
        $found = @($wealthList.data.items | Where-Object { $_.id -eq $wealthItemId })
        Assert-True -Name "wealth/list contains saved item" -Condition ($found.Count -eq 1) -Reason "item not found (count=$($found.Count))"
    }
    $wealthSummaryAfter = Get-Json -Route "wealth/summary&userId=$([uri]::EscapeDataString($userId))" -Name "wealth/summary after save"
    if ($wealthSummaryAfter -and $wealthSummaryAfter.data) {
        Assert-True -Name "wealth summary counts investment" -Condition ($wealthSummaryAfter.data.counts.investments -ge 1) -Reason "investment count did not increase"
    }
    $delBody = @{ route = "wealth/delete"; userId = $userId; section = "investments"; id = $wealthItemId; requestId = "ct-" + [guid]::NewGuid().ToString("N") } | ConvertTo-Json
    $wealthDelete = Get-Json -Route "wealth/delete" -Method POST -JsonBody $delBody -Name "wealth/delete"
    if ($wealthDelete -and $wealthDelete.data) {
        Assert-True -Name "wealth/delete marked deleted" -Condition ($wealthDelete.data.deleted -eq $true) -Reason "deleted flag not true"
    }
} else {
    Fail "wealth/save returned item id" "no item in data"
}

$freedomBody = @{ route = "wealth/freedom/save"; monthlyRequirement = 5000; annualRequirement = 60000; targetPassiveIncome = 750; targetNetWorth = 250000; targetDate = "2031-01-01"; monthlyContribution = 250; requestId = "ct-" + [guid]::NewGuid().ToString("N") } | ConvertTo-Json
$freedomSave = Get-Json -Route "wealth/freedom/save" -Method POST -JsonBody $freedomBody -Name "wealth/freedom/save"
if ($freedomSave -and $freedomSave.data) {
    Assert-True -Name "freedom save targetPassiveIncome=750" -Condition ($freedomSave.data.targetPassiveIncome -eq 750) -Reason "value not returned"
    $freedomRead = Get-Json -Route "wealth/freedom/get" -Name "wealth/freedom/get after save"
    if ($freedomRead -and $freedomRead.data) {
        Assert-True -Name "freedom get persisted targetPassiveIncome=750" -Condition ($freedomRead.data.targetPassiveIncome -eq 750) -Reason "value not persisted"
    }
}

$transferBody = @{ route = "transfers/execute"; userId = $userId; sourceAccount = "joint"; destinationTarget = "vacation"; amount = 1.0; direction = "out"; requestId = "ct-" + [guid]::NewGuid().ToString("N") } | ConvertTo-Json
$transfer = Get-Json -Route "transfers/execute" -Method POST -JsonBody $transferBody -Name "transfers/execute (joint->vacation 1.00)"
if ($transfer -and $transfer.data) {
    Assert-True -Name "transfers/execute returned transfer" -Condition ($null -ne $transfer.data.transfer) -Reason "transfer missing"
}

$txnBody = @{ route = "finance/add_transaction"; userId = $userId; type = "expense"; description = "ContractTest expense"; category = "test"; amount = 1.0; wallet = "joint"; requestId = $requestId } | ConvertTo-Json
$txn = Get-Json -Route "finance/add_transaction" -Method POST -JsonBody $txnBody -Name "finance/add_transaction (with requestId)"
$txnId = $null
if ($txn -and $txn.data -and $txn.data.transaction) { $txnId = $txn.data.transaction.transactionId }
Assert-True -Name "add_transaction returned transactionId" -Condition ($null -ne $txnId) -Reason "no transaction"

$txnAgain = Get-Json -Route "finance/add_transaction" -Method POST -JsonBody $txnBody -Name "add_transaction replay (same requestId)"
if ($txnAgain -and $txnAgain.data -and $txnAgain.data.transaction) {
    Assert-True -Name "idempotent replay returns same transaction" -Condition ($txnAgain.data.transaction.transactionId -eq $txnId) -Reason "different transactionId: $($txnAgain.data.transaction.transactionId) vs $txnId"
}
$summaryAfter = Get-Json -Route "finance/summary&userId=$([uri]::EscapeDataString($userId))" -Name "finance/summary after transaction"
if ($summaryAfter -and $summaryAfter.data -and $txnId) {
    $recent = @()
    if ($summaryAfter.data.PSObject.Properties["recentTransactions"]) { $recent = @($summaryAfter.data.recentTransactions) }
    $matches = @($recent | Where-Object { $_.transactionId -eq $txnId })
    Assert-True -Name "finance summary contains new transaction" -Condition ($matches.Count -ge 1) -Reason "transaction $txnId not in recentTransactions"
    $ledgerRows = @($recent | Where-Object { $_.description -eq "ContractTest expense" -and $_.amount -eq 1.0 })
    Assert-True -Name "recent transaction has amount=1.0 description=ContractTest expense" -Condition ($ledgerRows.Count -ge 1) -Reason "transaction fields wrong"
}

Write-Host "=== Phase E: transient HTML sampling (30 rapid GETs) ===" -ForegroundColor Cyan
$htmlCount = 0
$errorCount = 0
for ($i = 1; $i -le 30; $i++) {
    try {
        $c = Invoke-Exec -Route "health" -Method GET
        $trimmed = $c.TrimStart()
        if ($trimmed.Length -gt 0 -and $trimmed.Substring(0, 1) -eq "<") { $htmlCount++ }
        else {
            $parsed = $null
            try { $parsed = $c | ConvertFrom-Json } catch { }
            if ($null -eq $parsed) { $htmlCount++ }
        }
    } catch {
        $errorCount++
    }
}
Write-Host "HTML/garbage responses after retries: $htmlCount, transport errors after retries: $errorCount / 30"
Assert-True -Name "no HTML responses survive client-style retries (30 samples)" -Condition ($htmlCount -eq 0 -and $errorCount -eq 0) -Reason "html=$htmlCount errors=$errorCount"

Write-Host ""
Write-Host "==================================" -ForegroundColor Cyan
Write-Host "PASS: $script:passCount   FAIL: $script:failCount"
if ($script:failCount -gt 0) {
    Write-Host "Failures:" -ForegroundColor Red
    $script:failures | ForEach-Object { Write-Host "  - $_" -ForegroundColor Red }
    exit 1
}
exit 0
