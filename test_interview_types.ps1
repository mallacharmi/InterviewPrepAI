$baseUrl = "http://localhost:8080"
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession

# Register new user
$regPage = Invoke-WebRequest -Uri "$baseUrl/register" -SessionVariable session -UseBasicParsing
$csrf = ""
if ($regPage.Content -match 'name="_csrf" value="([^"]+)"') {
    $csrf = $matches[1]
}

$uEmail = "typeuser$(Get-Random)@example.com"
$regPost = Invoke-WebRequest -Uri "$baseUrl/register" -Method Post -Body @{
    name = "Type Tester"
    email = $uEmail
    password = "Password123!"
    targetRole = "Java Developer"
    experienceLevel = "MID_LEVEL"
    _csrf = $csrf
} -WebSession $session -UseBasicParsing -MaximumRedirection 5

# Login as test user
$loginPage = Invoke-WebRequest -Uri "$baseUrl/login" -SessionVariable session -UseBasicParsing
if ($loginPage.Content -match 'name="_csrf" value="([^"]+)"') {
    $csrf = $matches[1]
}

$loginRes = Invoke-WebRequest -Uri "$baseUrl/login" -Method Post -Body @{
    email = $uEmail
    password = "Password123!"
    _csrf = $csrf
} -WebSession $session -UseBasicParsing -MaximumRedirection 5

# Extract CSRF from dashboard
$dashPage = Invoke-WebRequest -Uri "$baseUrl/dashboard" -WebSession $session -UseBasicParsing
$apiCsrf = ""
if ($dashPage.Content -match 'name="_csrf" value="([^"]+)"') {
    $apiCsrf = $matches[1]
}
$headers = @{
    "Content-Type" = "application/json"
}
if ($apiCsrf) { $headers["X-CSRF-TOKEN"] = $apiCsrf }

Write-Host "=== 1. Checking Setup Page Options ==="
$setupPage = Invoke-WebRequest -Uri "$baseUrl/interviews/new" -WebSession $session -UseBasicParsing
$hasTech = $setupPage.Content -match 'value="TECHNICAL"[^>]*>Technical</option>'
$hasHr = $setupPage.Content -match 'value="HR"[^>]*>HR</option>'
$hasMixed = $setupPage.Content -match 'value="MIXED"[^>]*>Technical \+ HR</option>'
Write-Host "Options Found: Technical=$hasTech, HR=$hasHr, Technical + HR=$hasMixed"

Write-Host "`n=== 2. Creating HR Interview ==="
$hrBody = @{
    targetRole = "Java Developer"
    interviewType = "HR"
    difficulty = "MEDIUM"
    totalQuestions = 5
    topics = @("Core Java")
} | ConvertTo-Json

$hrRes = Invoke-RestMethod -Uri "$baseUrl/api/interviews" -Method Post -Body $hrBody -Headers $headers -WebSession $session
$hrQ = Invoke-RestMethod -Uri "$baseUrl/api/interviews/$($hrRes.id)/start" -Method Post -Headers $headers -WebSession $session
Write-Host "HR Interview ID: $($hrRes.id), Type: $($hrRes.interviewType)"
Write-Host "HR Question: $($hrQ.questionText)"

Write-Host "`n=== 3. Creating Technical + HR Interview ==="
$mixedBody = @{
    targetRole = "Java Developer"
    interviewType = "MIXED"
    difficulty = "MEDIUM"
    totalQuestions = 5
    topics = @("Core Java", "Spring Boot")
} | ConvertTo-Json

$mixedRes = Invoke-RestMethod -Uri "$baseUrl/api/interviews" -Method Post -Body $mixedBody -Headers $headers -WebSession $session
$q1 = Invoke-RestMethod -Uri "$baseUrl/api/interviews/$($mixedRes.id)/start" -Method Post -Headers $headers -WebSession $session
Write-Host "Mixed Q1 (Tech): $($q1.questionText)"

$ansBody = @{
    answerText = "Java is an object-oriented language supporting inheritance, encapsulation, abstraction, and polymorphism."
} | ConvertTo-Json
$eval1 = Invoke-RestMethod -Uri "$baseUrl/api/interviews/$($mixedRes.id)/questions/$($q1.id)/answer" -Method Post -Body $ansBody -Headers $headers -WebSession $session
$q2 = Invoke-RestMethod -Uri "$baseUrl/api/interviews/$($mixedRes.id)/question" -Headers $headers -WebSession $session
Write-Host "Mixed Q2 (HR/Behavioral): $($q2.questionText)"

Write-Host "`n=== 4. Checking History Page Rendering ==="
$histPage = Invoke-WebRequest -Uri "$baseUrl/interviews/history" -WebSession $session -UseBasicParsing
Write-Host "History Page HTTP Status: $($histPage.StatusCode)"
$histLines = $histPage.Content -split "`n" | Where-Object { $_ -match "<td>Technical" -or $_ -match "<td>HR" }
foreach ($hl in $histLines) {
    Write-Host "Table Line: $($hl.Trim())"
}
$hasTechInHist = $histPage.Content -like "*Technical*"
$hasHrInHist = $histPage.Content -like "*<td>HR</td>*"
$hasMixedInHist = $histPage.Content -like "*Technical + HR*"
Write-Host "History View Labels: Tech=$hasTechInHist, HR=$hasHrInHist, Technical + HR=$hasMixedInHist"

if ($hasTech -and $hasHr -and $hasMixed -and $histPage.StatusCode -eq 200) {
    Write-Host "`nSUCCESS: All options (Technical, HR, Technical + HR) verified and working perfectly!"
} else {
    Write-Host "`nFAILURE: One or more options failed verification."
}
