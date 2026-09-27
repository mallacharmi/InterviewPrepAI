Start-Sleep -Seconds 12

$baseUrl = "http://localhost:8080"

Write-Host "=== Testing ATS Gibberish Validation & File Upload ==="
$loginPage = Invoke-WebRequest -Uri "$baseUrl/login" -SessionVariable session -UseBasicParsing
$csrfToken = ""
if ($loginPage.Content -match 'name="_csrf" value="([^"]+)"') {
    $csrfToken = $matches[1]
}

# Register & Login new user
$uEmail = "atsuser$(Get-Random)@example.com"
$regBody = @{
    name = "ATS Test User"
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

# 1. Test Gibberish Rejection (qqqqqqqqqqqqqqqqqqqq)
$gibberishBody = @{
    resumeText = "qqqqqqqqqqqqqqqqqqqq"
    jobDescription = "qqqqqqqqqqqqqqqqqqqq"
} | ConvertTo-Json

Write-Host "`n1. Submitting Gibberish Input ('qqqqqqqqqqqqqqqqqqqq')..."
try {
    $gibResult = Invoke-RestMethod -Uri "$baseUrl/api/ats/analyze" -Method Post -Body $gibberishBody -Headers $headers -WebSession $loginSession
    Write-Host "FAIL: Gibberish was accepted with score: $($gibResult.atsScore)%"
} catch {
    Write-Host "PASS: Gibberish input was correctly REJECTED with error message: $($_.Exception.Message)"
}

# 2. Test Valid Resume & Job Description Analysis
$validBody = @{
    resumeText = "Senior Software Engineer with 5 years experience building microservices using Java, Spring Boot, Hibernate, REST API, Docker, MySQL, and System Design."
    jobDescription = "Looking for a Senior Java Developer proficient in Java, Spring Boot, Microservices, Docker, Kubernetes, and System Design."
} | ConvertTo-Json

Write-Host "`n2. Submitting Valid Resume and Job Description..."
$validResult = Invoke-RestMethod -Uri "$baseUrl/api/ats/analyze" -Method Post -Body $validBody -Headers $headers -WebSession $loginSession
Write-Host "ATS Compatibility Score: $($validResult.atsScore)%"
Write-Host "Match Category: $($validResult.matchCategory)"
Write-Host "Matched Keywords: $($validResult.matchedKeywords -join ', ')"
Write-Host "Missing Skills: $($validResult.missingSkills -join ', ')"

if ($validResult.atsScore -gt 60) {
    Write-Host "PASS: Valid ATS Resume & JD analyzed successfully!"
} else {
    Write-Host "FAIL: ATS score unexpected: $($validResult.atsScore)"
}
