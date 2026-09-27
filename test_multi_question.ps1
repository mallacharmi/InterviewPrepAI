Start-Sleep -Seconds 12

$baseUrl = "http://localhost:8080"

Write-Host "=== Multi-Question Full Interview Flow Test ==="
$loginPage = Invoke-WebRequest -Uri "$baseUrl/login" -SessionVariable session -UseBasicParsing
$csrfToken = ""
if ($loginPage.Content -match 'name="_csrf" value="([^"]+)"') {
    $csrfToken = $matches[1]
}

# Register & Login new user
$uEmail = "multiquser$(Get-Random)@example.com"
$regBody = @{
    name = "Multi Question User"
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

# 1. Create interview for 5 questions
$createBody = @{
    targetRole = "Java Developer"
    interviewType = "TECHNICAL"
    difficulty = "MEDIUM"
    totalQuestions = 5
    topics = @("OOP", "Collections", "Multithreading", "Spring Boot", "Databases")
} | ConvertTo-Json

$interview = Invoke-RestMethod -Uri "$baseUrl/api/interviews" -Method Post -Body $createBody -Headers $headers -WebSession $loginSession
$interviewId = $interview.id
Write-Host "Created Interview ID: $interviewId"

# 2. Start interview
$q1 = Invoke-RestMethod -Uri "$baseUrl/api/interviews/$interviewId/start" -Method Post -Headers $headers -WebSession $loginSession
Write-Host "`n[Question 1]: $($q1.questionText)"

for ($step = 1; $step -le 10; $step++) {
    # Get current question
    $curQ = Invoke-RestMethod -Uri "$baseUrl/api/interviews/$interviewId/question" -Method Get -Headers $headers -WebSession $loginSession -ErrorAction SilentlyContinue
    if (-not $curQ -or -not $curQ.id) {
        Write-Host "No more questions available or interview completed!"
        break
    }
    
    Write-Host "`n[Step $step] Answering Question ID $($curQ.id) (Seq: $($curQ.sequenceNumber), Topic: $($curQ.topic)):"
    Write-Host "  Text: $($curQ.questionText)"
    
    $ansBody = @{
        answerText = "In Java programming, polymorphism allows objects to be treated as instances of their parent class while executing overriding methods dynamically at runtime via the JVM vtable."
    } | ConvertTo-Json
    
    $eval = Invoke-RestMethod -Uri "$baseUrl/api/interviews/$interviewId/questions/$($curQ.id)/answer" -Method Post -Body $ansBody -Headers $headers -WebSession $loginSession
    Write-Host "  Evaluation Score: $($eval.score)/10 | Has FollowUp: $($eval.hasFollowUp)"
    if ($eval.hasFollowUp) {
        Write-Host "  Follow-up Question: $($eval.followUpQuestion)"
    }
}

# Complete interview
$report = Invoke-RestMethod -Uri "$baseUrl/api/interviews/$interviewId/complete" -Method Post -Headers $headers -WebSession $loginSession
Write-Host "`nFinal Report Score: $($report.overallScore), Total Questions Answered: $($report.questionResults.Count)"

if ($report.questionResults.Count -ge 5) {
    Write-Host "`nPASS: Successfully navigated and answered all 5+ questions without any error!"
} else {
    Write-Host "`nFAIL: Answered count was $($report.questionResults.Count)"
}
