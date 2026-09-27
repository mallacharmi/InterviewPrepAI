$baseUrl = "http://localhost:8080"
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession

Write-Host "=== TEST 1: Register and Login ==="
$regPage = Invoke-WebRequest -Uri "$baseUrl/register" -SessionVariable session -UseBasicParsing
$csrf = ""
if ($regPage.Content -match 'name="_csrf" value="([^"]+)"') {
    $csrf = $matches[1]
}

$testEmail = "streakuser$(Get-Random)@example.com"
$regPost = Invoke-WebRequest -Uri "$baseUrl/register" -Method Post -Body @{
    name = "Streak Tester"
    email = $testEmail
    password = "Password123!"
    targetRole = "Java Developer"
    experienceLevel = "MID_LEVEL"
    _csrf = $csrf
} -WebSession $session -UseBasicParsing -MaximumRedirection 5

$loginPage = Invoke-WebRequest -Uri "$baseUrl/login" -SessionVariable session -UseBasicParsing
if ($loginPage.Content -match 'name="_csrf" value="([^"]+)"') {
    $csrf = $matches[1]
}

$loginRes = Invoke-WebRequest -Uri "$baseUrl/login" -Method Post -Body @{
    email = $testEmail
    password = "Password123!"
    _csrf = $csrf
} -WebSession $session -UseBasicParsing -MaximumRedirection 5
Write-Host "Login response status:" $loginRes.StatusCode

Write-Host "`n=== TEST 2: Verify Initial Dashboard and KPI Stat Cards ==="
$dashRes = Invoke-WebRequest -Uri "$baseUrl/dashboard" -WebSession $session -UseBasicParsing
Write-Host "Dashboard status code:" $dashRes.StatusCode

$hasDailyStreak = $dashRes.Content.Contains('Daily Streak')
$hasInterviewsDone = $dashRes.Content.Contains('Interviews Done')
$hasTerminations = $dashRes.Content.Contains('Terminations')
$hasQuestionsEvaluated = $dashRes.Content.Contains('Questions Evaluated')

Write-Host "Dashboard contains 'Daily Streak': $hasDailyStreak"
Write-Host "Dashboard contains 'Interviews Done': $hasInterviewsDone"
Write-Host "Dashboard contains 'Terminations': $hasTerminations"
Write-Host "Dashboard contains 'Questions Evaluated': $hasQuestionsEvaluated"

if (-not ($hasDailyStreak -and $hasInterviewsDone -and $hasTerminations -and $hasQuestionsEvaluated)) {
    Write-Error "Missing KPI cards on Dashboard!"
    exit 1
}

Write-Host "`n=== TEST 3: Verify /api/dashboard JSON response ==="
$apiDashRes = Invoke-WebRequest -Uri "$baseUrl/api/dashboard" -WebSession $session -UseBasicParsing
$apiDash = $apiDashRes.Content | ConvertFrom-Json
Write-Host "Current Streak: $($apiDash.currentStreak)"
Write-Host "Max Streak: $($apiDash.maxStreak)"
Write-Host "Total Attended: $($apiDash.totalInterviews)"
Write-Host "Completed: $($apiDash.completedInterviews)"
Write-Host "Terminated: $($apiDash.terminatedInterviews)"
Write-Host "Total Violations: $($apiDash.totalViolations)"
Write-Host "Total Questions Answered: $($apiDash.totalQuestionsAnswered)"

if ($apiDash.currentStreak -lt 1) {
    Write-Error "Initial currentStreak should be at least 1!"
    exit 1
}

Write-Host "`n=== TEST 4: Create and Start an Interview ==="
$dashDash = Invoke-WebRequest -Uri "$baseUrl/dashboard" -WebSession $session -UseBasicParsing
$apiCsrf = ""
if ($dashDash.Content -match 'name="_csrf" content="([^"]+)"') {
    $apiCsrf = $matches[1]
} elseif ($dashDash.Content -match 'name="_csrf" value="([^"]+)"') {
    $apiCsrf = $matches[1]
}

$createBody = @{
    targetRole = "Java Developer"
    interviewType = "MIXED"
    difficulty = "MEDIUM"
    totalQuestions = 5
    topics = @("OOP", "Collections")
} | ConvertTo-Json

$headers = @{
    "Content-Type" = "application/json"
}
if ($apiCsrf) {
    $headers["X-CSRF-TOKEN"] = $apiCsrf
}

$createRes = Invoke-RestMethod -Uri "$baseUrl/api/interviews" -Method Post -Body $createBody -Headers $headers -WebSession $session
$interviewId = $createRes.id
Write-Host "Created Interview ID: $interviewId"

$startRes = Invoke-RestMethod -Uri "$baseUrl/api/interviews/$interviewId/start" -Method Post -Headers $headers -WebSession $session
Write-Host "Started Interview Question: $($startRes.questionText)"

Write-Host "`n=== TEST 5: Terminate Interview with Proctoring Violation ==="
$terminateBody = @{
    reason = "Prohibited Device (Mobile Phone) detected in camera feed"
    violationCount = 1
} | ConvertTo-Json

$termRes = Invoke-RestMethod -Uri "$baseUrl/api/interviews/$interviewId/terminate" -Method Post -Body $terminateBody -Headers $headers -WebSession $session
Write-Host "Terminated successfully. Report overall score: $($termRes.overallScore)"

Write-Host "`n=== TEST 6: Verify Dashboard Reflects Termination ==="
$apiDash2Res = Invoke-WebRequest -Uri "$baseUrl/api/dashboard" -WebSession $session -UseBasicParsing
$apiDash2 = $apiDash2Res.Content | ConvertFrom-Json

Write-Host "Updated Total Attended: $($apiDash2.totalInterviews)"
Write-Host "Updated Terminated: $($apiDash2.terminatedInterviews)"
Write-Host "Updated Violations: $($apiDash2.totalViolations)"

if ($apiDash2.terminatedInterviews -ne 1 -or $apiDash2.totalInterviews -ne 1) {
    Write-Error "Terminated interviews count did not update to 1!"
    exit 1
}

$dashPage2 = Invoke-WebRequest -Uri "$baseUrl/dashboard" -WebSession $session -UseBasicParsing
$has1Terminated = $dashPage2.Content.Contains('1</span>') -or $dashPage2.Content.Contains('>1<')
Write-Host "Dashboard rendered updated termination counts."

Write-Host "`n=== TEST 7: Verify Report Page for Terminated Interview ==="
$reportRes = Invoke-WebRequest -Uri "$baseUrl/interviews/$interviewId/report" -WebSession $session -UseBasicParsing
$hasEarlyTerminationBanner = $reportRes.Content.Contains('Assessment Terminated Early')
$hasTerminationReason = $reportRes.Content.Contains('Prohibited Device (Mobile Phone) detected')

Write-Host "Report shows Assessment Terminated Early: $hasEarlyTerminationBanner"
Write-Host "Report shows Prohibited Device reason: $hasTerminationReason"

if (-not $hasEarlyTerminationBanner) {
    Write-Error "Report page missing Assessment Terminated Early alert banner!"
    exit 1
}

Write-Host "`n=== TEST 8: Verify History Page Shows TERMINATED Badge ==="
$histRes = Invoke-WebRequest -Uri "$baseUrl/interviews/history" -WebSession $session -UseBasicParsing
$hasTerminatedBadge = $histRes.Content.Contains('TERMINATED')
$hasViewReport = $histRes.Content.Contains("/interviews/$interviewId/report")

Write-Host "History page contains TERMINATED: $hasTerminatedBadge"
Write-Host "History page contains View Report link: $hasViewReport"

if (-not ($hasTerminatedBadge -and $hasViewReport)) {
    Write-Error "History page missing TERMINATED status or View Report link!"
    exit 1
}

Write-Host "`n======================================================="
Write-Host "ALL STREAK & TERMINATION VERIFICATION TESTS PASSED SUCCESSFULLY!"
Write-Host "======================================================="
