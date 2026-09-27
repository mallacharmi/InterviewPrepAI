$baseUrl = "http://localhost:8080"
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession

Write-Host "=== 1. Register & Login Test User ==="
$regPage = Invoke-WebRequest -Uri "$baseUrl/register" -SessionVariable session -UseBasicParsing
$csrf = ""
if ($regPage.Content -match 'name="_csrf" value="([^"]+)"') {
    $csrf = $matches[1]
}

$uEmail = "expression$(Get-Random)@example.com"
$regPost = Invoke-WebRequest -Uri "$baseUrl/register" -Method Post -Body @{
    name = "Expression Candidate"
    email = $uEmail
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
    email = $uEmail
    password = "Password123!"
    _csrf = $csrf
} -WebSession $session -UseBasicParsing -MaximumRedirection 5
Write-Host "Login Status:" $loginRes.StatusCode

# Extract CSRF token from dashboard
$dashPage = Invoke-WebRequest -Uri "$baseUrl/dashboard" -WebSession $session -UseBasicParsing
$apiCsrf = ""
if ($dashPage.Content -match 'name="_csrf" value="([^"]+)"') {
    $apiCsrf = $matches[1]
}

$headers = @{ "Content-Type" = "application/json" }
if ($apiCsrf) { $headers["X-CSRF-TOKEN"] = $apiCsrf }

Write-Host "`n=== 2. Verify interview.html has live facial expression pill ==="
$setupPage = Invoke-WebRequest -Uri "$baseUrl/interviews/new" -WebSession $session -UseBasicParsing
$createBody = @{
    targetRole = "Java Developer"
    interviewType = "MIXED"
    difficulty = "MEDIUM"
    totalQuestions = 5
    topics = @("Java Core", "Spring Boot")
} | ConvertTo-Json

$createRes = Invoke-WebRequest -Uri "$baseUrl/api/interviews" -Method Post -Headers $headers -Body $createBody -WebSession $session -UseBasicParsing
$createdInterview = $createRes.Content | ConvertFrom-Json
$intId = $createdInterview.id
Write-Host "Created Interview ID: $intId"

$liveInterviewPage = Invoke-WebRequest -Uri "$baseUrl/interviews/$intId" -WebSession $session -UseBasicParsing
$hasPipExpr = $liveInterviewPage.Content.Contains('id="pip-expression-status"')
$hasExprLabel = $liveInterviewPage.Content.Contains('id="current-expression-label"')
$hasExprActive = $liveInterviewPage.Content.Contains('Facial Expression Analysis Active')
Write-Host "Live WebCam PiP Expression Status Present: $hasPipExpr"
Write-Host "Live Current Expression Label Present: $hasExprLabel"
Write-Host "Live Status Bar Expression Active Present: $hasExprActive"

if (-not ($hasPipExpr -and $hasExprLabel -and $hasExprActive)) {
    Write-Error "FAIL: Live interview page missing facial expression UI elements!"
    exit 1
}

Write-Host "`n=== 3. Answer Questions & Save Facial Expressions ==="
for ($q = 1; $q -le 5; $q++) {
    $qRes = Invoke-WebRequest -Uri "$baseUrl/api/interviews/$intId/question" -WebSession $session -UseBasicParsing
    $qObj = $qRes.Content | ConvertFrom-Json
    $ansBody = @{
        answerText = "In Java and Spring Boot, we implement scalable microservices using dependency injection, clear separation of concerns, transactional boundaries, and robust error handling."
    } | ConvertTo-Json
    $subRes = Invoke-WebRequest -Uri "$baseUrl/api/interviews/$intId/questions/$($qObj.id)/answer" -Method Post -Headers $headers -Body $ansBody -WebSession $session -UseBasicParsing
}

# Post Facial Expressions metrics
$exprBody = @{
    confidence = 74.0
    calm = 16.0
    fear = 6.0
    shy = 4.0
    summary = "Facial expression analysis: 74% Confident, 16% Calm & Composed, 6% Fear/Nervousness, 4% Shy/Hesitant."
} | ConvertTo-Json

$exprRes = Invoke-WebRequest -Uri "$baseUrl/api/interviews/$intId/expressions" -Method Post -Headers $headers -Body $exprBody -WebSession $session -UseBasicParsing
Write-Host "Save Facial Expressions API Status: $($exprRes.StatusCode), Body: $($exprRes.Content)"

# Complete Interview
$compRes = Invoke-WebRequest -Uri "$baseUrl/api/interviews/$intId/complete" -Method Post -Headers $headers -WebSession $session -UseBasicParsing
Write-Host "Complete Interview API Status: $($compRes.StatusCode)"

Write-Host "`n=== 4. Verify API Report Contains Facial Expressions ==="
$reportApiRes = Invoke-WebRequest -Uri "$baseUrl/api/interviews/$intId/report" -WebSession $session -UseBasicParsing
$reportJson = $reportApiRes.Content | ConvertFrom-Json
Write-Host "Report Confidence Score: $($reportJson.confidenceScore)%"
Write-Host "Report Calm Score: $($reportJson.calmScore)%"
Write-Host "Report Fear Score: $($reportJson.fearScore)%"
Write-Host "Report Shy Score: $($reportJson.shyScore)%"
Write-Host "Report Expression Summary: $($reportJson.expressionSummary)"
Write-Host "Report Expression Feedback Count: $($reportJson.expressionFeedback.Count)"
foreach ($fb in $reportJson.expressionFeedback) {
    Write-Host "  - $fb"
}

if ($reportJson.confidenceScore -ne 74.0 -or $reportJson.fearScore -ne 6.0 -or $reportJson.shyScore -ne 4.0) {
    Write-Error "FAIL: Report API does not have the expected facial expression scores!"
    exit 1
}

Write-Host "`n=== 5. Verify HTML Report Card Contains Facial Expressions Pie Chart ==="
$reportHtmlRes = Invoke-WebRequest -Uri "$baseUrl/interviews/$intId/report" -WebSession $session -UseBasicParsing
Write-Host "Report HTML Status:" $reportHtmlRes.StatusCode

$hasCard = $reportHtmlRes.Content.Contains('id="facial-expressions-card"')
$hasPieCanvas = $reportHtmlRes.Content.Contains('id="expressionPieChart"')
$hasPieScript = $reportHtmlRes.Content.Contains("type: 'pie'")
$hasConfidence = $reportHtmlRes.Content.Contains('Confidence')
$hasCalm = $reportHtmlRes.Content.Contains('Calm & Composed')
$hasFear = $reportHtmlRes.Content.Contains('Fear / Nervousness')
$hasShy = $reportHtmlRes.Content.Contains('Shy / Hesitant')
$hasCoaching = $reportHtmlRes.Content.Contains('AI Delivery & Composure Coaching')

Write-Host "Contains Facial Expressions Card: $hasCard"
Write-Host "Contains Expression Pie Chart Canvas: $hasPieCanvas"
Write-Host "Contains Chart.js Pie Type Script: $hasPieScript"
Write-Host "Contains Confidence: $hasConfidence"
Write-Host "Contains Calm & Composed: $hasCalm"
Write-Host "Contains Fear / Nervousness: $hasFear"
Write-Host "Contains Shy / Hesitant: $hasShy"
Write-Host "Contains AI Coaching Section: $hasCoaching"

if ($hasCard -and $hasPieCanvas -and $hasPieScript -and $hasConfidence -and $hasCalm -and $hasFear -and $hasShy -and $hasCoaching) {
    Write-Host "`n>>> SUCCESS: Facial Expressions capture and Pie Chart report verified 100%! <<<"
} else {
    Write-Error "FAIL: One or more HTML report facial expression components were missing!"
    exit 1
}
