Start-Sleep -Seconds 12

$baseUrl = "http://localhost:8080"

Write-Host "=== Testing Direct Answer Submission on CREATED Interview State ==="
$loginPage = Invoke-WebRequest -Uri "$baseUrl/login" -SessionVariable session -UseBasicParsing
$csrfToken = ""
if ($loginPage.Content -match 'name="_csrf" value="([^"]+)"') {
    $csrfToken = $matches[1]
}

# Register & Login new user
$uEmail = "createduser$(Get-Random)@example.com"
$regBody = @{
    name = "Created State User"
    email = $uEmail
    password = "Password123!"
    targetRole = "Java Developer"
    experienceLevel = "MID"
    _csrf = $csrfToken
}
$regPost = Invoke-WebRequest -Uri "$baseUrl/register" -Method Post -Body $regBody -WebSession $session -UseBasicParsing -MaximumRedirection 0 -ErrorAction SilentlyContinue

$loginPage = Invoke-WebRequest -Uri "$baseUrl/login" -SessionVariable loginSession -UseBasicParsing
$loginCsrf = ""
if ($loginPage.Content -match 'name="_csrf" value="([^"]+)"') {
    $loginCsrf = $matches[1]
}
$loginBody = @{
    email = $uEmail
    password = "Password123!"
    _csrf = $loginCsrf
}
$loginPost = Invoke-WebRequest -Uri "$baseUrl/login" -Method Post -Body $loginBody -WebSession $loginSession -UseBasicParsing -MaximumRedirection 0 -ErrorAction SilentlyContinue

$dashPage = Invoke-WebRequest -Uri "$baseUrl/dashboard" -WebSession $loginSession -UseBasicParsing
$apiCsrf = ""
if ($dashPage.Content -match 'name="_csrf" value="([^"]+)"') {
    $apiCsrf = $matches[1]
}

$headers = @{ "Content-Type" = "application/json" }
if ($apiCsrf) { $headers["X-CSRF-TOKEN"] = $apiCsrf }

# 1. Create interview (Status will be CREATED)
$createBody = @{
    targetRole = "Java Developer"
    interviewType = "TECHNICAL"
    difficulty = "MEDIUM"
    totalQuestions = 5
    topics = @("OOP")
} | ConvertTo-Json

$interview = Invoke-RestMethod -Uri "$baseUrl/api/interviews" -Method Post -Body $createBody -Headers $headers -WebSession $loginSession
$interviewId = $interview.id
Write-Host "Created Interview ID: $interviewId"

# Get current question (this generates question 1, status remains CREATED or transitions)
$curQ = Invoke-RestMethod -Uri "$baseUrl/api/interviews/$interviewId/question" -Method Get -Headers $headers -WebSession $loginSession
Write-Host "Fetched Question ID: $($curQ.id)"

# NOW submit answer DIRECTLY without calling /start endpoint first!
$ansBody = @{
    answerText = "Java Records were introduced in Java 14+ to provide a compact syntax for declaring classes that are transparent holders for shallowly immutable data."
} | ConvertTo-Json

$eval = Invoke-RestMethod -Uri "$baseUrl/api/interviews/$interviewId/questions/$($curQ.id)/answer" -Method Post -Body $ansBody -Headers $headers -WebSession $loginSession
Write-Host "Direct Answer Submission Status: SUCCESS!"
Write-Host "Evaluation Score: $($eval.score)/10"

if ($eval.score -gt 0) {
    Write-Host "PASS: Direct answer submission on CREATED status succeeded without any InvalidInterviewStateException error!"
} else {
    Write-Host "FAIL: Answer submission score was 0."
}
