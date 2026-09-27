# Test Custom Role Registration, Profile Update, and Interview Creation
$baseUrl = "http://localhost:8080"
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession

Write-Host "=== 1. Check Register Page HTML for Custom Role Controls ==="
$regPage = Invoke-WebRequest -Uri "$baseUrl/register" -SessionVariable session -UseBasicParsing
$regHtml = $regPage.Content

$csrf = ""
if ($regHtml -match 'name="_csrf" value="([^"]+)"') {
    $csrf = $matches[1]
}

$hasToggleBtn = $regHtml.Contains('id="toggleCustomRoleBtn"')
$hasCustomWrapper = $regHtml.Contains('id="customRoleWrapper"')
$hasCustomInput = $regHtml.Contains('id="customRoleInput"')
$hasOtherOption = $regHtml.Contains('value="OTHER"')

Write-Host "Register has toggle button: $hasToggleBtn"
Write-Host "Register has custom wrapper: $hasCustomWrapper"
Write-Host "Register has custom input: $hasCustomInput"
Write-Host "Register has OTHER option: $hasOtherOption"

if (-not ($hasToggleBtn -and $hasCustomWrapper -and $hasCustomInput -and $hasOtherOption)) {
    Write-Error "Register page missing custom role elements!"
    exit 1
}

$rand = Get-Random -Minimum 10000 -Maximum 99999
$email = "customrole_$rand@example.com"
$password = "StrongPass1!"
$name = "Custom Role Tester"
$customRole = "DevOps Engineer"

Write-Host "`n=== 2. Register User with Custom Role: $customRole ==="
$regPost = Invoke-WebRequest -Uri "$baseUrl/register" -Method Post -Body @{
    name = $name
    email = $email
    password = $password
    targetRole = $customRole
    experienceLevel = "MID"
    _csrf = $csrf
} -WebSession $session -UseBasicParsing -MaximumRedirection 5

Write-Host "Registration status: $($regPost.StatusCode)"

Write-Host "`n=== 3. Log In with Newly Registered User ==="
$loginPage = Invoke-WebRequest -Uri "$baseUrl/login" -SessionVariable session -UseBasicParsing
$loginCsrf = ""
if ($loginPage.Content -match 'name="_csrf" value="([^"]+)"') {
    $loginCsrf = $matches[1]
}

$loginResp = Invoke-WebRequest -Uri "$baseUrl/login" -Method Post -Body @{
    email = $email
    password = $password
    _csrf = $loginCsrf
} -WebSession $session -UseBasicParsing -MaximumRedirection 5
Write-Host "Login status: $($loginResp.StatusCode)"

# Get active CSRF token for subsequent REST calls
$dashboardPage = Invoke-WebRequest -Uri "$baseUrl/dashboard" -WebSession $session -UseBasicParsing
$apiCsrf = ""
if ($dashboardPage.Content -match 'name="_csrf" content="([^"]+)"' -or $dashboardPage.Content -match 'name="_csrf" value="([^"]+)"') {
    $apiCsrf = $matches[1]
}

$headers = @{}
if ($apiCsrf) {
    $headers["X-CSRF-TOKEN"] = $apiCsrf
}

Write-Host "`n=== 4. Check Current User API for Custom Role ==="
$userApiResp = Invoke-RestMethod -Uri "$baseUrl/api/users/me" -Method GET -WebSession $session -Headers $headers
Write-Host "User Target Role from API: $($userApiResp.targetRole)"
if ($userApiResp.targetRole -ne $customRole) {
    Write-Error "Expected role $customRole, got $($userApiResp.targetRole)"
    exit 1
}

Write-Host "`n=== 5. Check Profile Page HTML ==="
$profileHtml = (Invoke-WebRequest -Uri "$baseUrl/profile" -WebSession $session -UseBasicParsing).Content
$profileHasToggle = $profileHtml.Contains('id="toggleProfileRoleBtn"')
$profileHasWrapper = $profileHtml.Contains('id="profileCustomRoleWrapper"')
$profileHasInput = $profileHtml.Contains('id="profileCustomRoleInput"')
$profileHasCustomRole = $profileHtml.Contains("$customRole (Custom)") -or $profileHtml.Contains($customRole)

Write-Host "Profile has toggle button: $profileHasToggle"
Write-Host "Profile has custom wrapper: $profileHasWrapper"
Write-Host "Profile has custom input: $profileHasInput"
Write-Host "Profile reflects custom role: $profileHasCustomRole"

if (-not ($profileHasToggle -and $profileHasWrapper -and $profileHasInput -and $profileHasCustomRole)) {
    Write-Error "Profile page missing custom role controls or user's custom role!"
    exit 1
}

Write-Host "`n=== 6. Update Profile to Another Custom Role: Cloud Architect ==="
$newCustomRole = "Cloud Architect"
$updateBody = @{
    name = $name
    targetRole = $newCustomRole
    experienceLevel = "SENIOR"
    reminderTime = "10:30"
} | ConvertTo-Json

$updateResp = Invoke-RestMethod -Uri "$baseUrl/api/users/me" -Method PUT -Body $updateBody -ContentType "application/json" -WebSession $session -Headers $headers
Write-Host "Updated role from API: $($updateResp.targetRole)"
if ($updateResp.targetRole -ne $newCustomRole) {
    Write-Error "Profile update failed! Expected $newCustomRole, got $($updateResp.targetRole)"
    exit 1
}

Write-Host "`n=== 7. Check Interview Setup Page HTML ==="
$setupHtml = (Invoke-WebRequest -Uri "$baseUrl/interviews/new" -WebSession $session -UseBasicParsing).Content
$setupHasToggle = $setupHtml.Contains('id="toggleSetupRoleBtn"')
$setupHasWrapper = $setupHtml.Contains('id="setupCustomRoleWrapper"')
$setupHasInput = $setupHtml.Contains('id="setupCustomRoleInput"')
$setupHasCustomRole = $setupHtml.Contains("$newCustomRole (Custom)") -or $setupHtml.Contains($newCustomRole)

Write-Host "Setup has toggle button: $setupHasToggle"
Write-Host "Setup has custom wrapper: $setupHasWrapper"
Write-Host "Setup has custom input: $setupHasInput"
Write-Host "Setup reflects custom role: $setupHasCustomRole"

if (-not ($setupHasToggle -and $setupHasWrapper -and $setupHasInput)) {
    Write-Error "Interview setup page missing custom role controls!"
    exit 1
}

Write-Host "`n=== 8. Create Interview with Custom Role: Mobile App Engineer ==="
$manualRole = "Mobile App Engineer"
$interviewBody = @{
    targetRole = $manualRole
    interviewType = "TECHNICAL"
    difficulty = "MEDIUM"
    totalQuestions = 5
    topics = @("Flutter Architecture", "State Management", "Mobile Performance")
} | ConvertTo-Json

$interviewResp = Invoke-RestMethod -Uri "$baseUrl/api/interviews" -Method POST -Body $interviewBody -ContentType "application/json" -WebSession $session -Headers $headers
Write-Host "Created Interview ID: $($interviewResp.id)"
Write-Host "Interview Target Role: $($interviewResp.targetRole)"

if ($interviewResp.targetRole -ne $manualRole) {
    Write-Error "Interview target role mismatch! Expected $manualRole, got $($interviewResp.targetRole)"
    exit 1
}

Write-Host "`n=== 9. Start Interview and Fetch First AI Question ==="
$startResp = Invoke-RestMethod -Uri "$baseUrl/api/interviews/$($interviewResp.id)/start" -Method POST -WebSession $session -Headers $headers
Write-Host "First question generated: $($startResp.questionText)"

Write-Host "`n=== 10. Check History Page ==="
$historyHtml = (Invoke-WebRequest -Uri "$baseUrl/interviews/history" -WebSession $session -UseBasicParsing).Content
$historyHasRole = $historyHtml.Contains($manualRole)
Write-Host "History displays custom role ${manualRole}: $historyHasRole"

if (-not $historyHasRole) {
    Write-Error "History page did not display the custom role $manualRole!"
    exit 1
}

Write-Host "`n======================================================="
Write-Host "ALL CUSTOM ROLE MANUAL ADDITION TESTS PASSED SUCCESSFULLY!"
Write-Host "======================================================="
