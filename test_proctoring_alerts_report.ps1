$ErrorActionPreference = "Stop"

Write-Host "======================================================="
Write-Host "TEST SUITE: PROCTORING ALERTS & BACKGROUND FACE MISMATCH"
Write-Host "======================================================="

$baseUrl = "http://localhost:8080"
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession

# 1. Fetch login page for initial CSRF
$loginPage = Invoke-WebRequest -Uri "$baseUrl/login" -WebSession $session -UseBasicParsing
$csrf = ""
if ($loginPage.Content -match 'name="_csrf" value="([^"]+)"') {
    $csrf = $matches[1]
}

$uEmail = "proctoruser$(Get-Random)@example.com"
$regPost = Invoke-WebRequest -Uri "$baseUrl/register" -Method Post -Body @{
    name = "Proctor Tester"
    email = $uEmail
    password = "Password123!"
    targetRole = "Data Engineer"
    experienceLevel = "MID_LEVEL"
    _csrf = $csrf
} -WebSession $session -UseBasicParsing -MaximumRedirection 5

# Explicit login step
$loginPage2 = Invoke-WebRequest -Uri "$baseUrl/login" -WebSession $session -UseBasicParsing
$loginCsrf = ""
if ($loginPage2.Content -match 'name="_csrf" value="([^"]+)"') {
    $loginCsrf = $matches[1]
}

$loginRes = Invoke-WebRequest -Uri "$baseUrl/login" -Method Post -Body @{
    email = $uEmail
    password = "Password123!"
    _csrf = $loginCsrf
} -WebSession $session -UseBasicParsing -MaximumRedirection 5
Write-Host "=== 1. Registered and Logged In Successfully (Status: $($loginRes.StatusCode)) ==="

# 2. Create Interview Session
$createBody = @{
    targetRole = "Data Engineer"
    interviewType = "TECHNICAL"
    difficulty = "MEDIUM"
    totalQuestions = 5
} | ConvertTo-Json

$createResp = Invoke-WebRequest -Uri "$baseUrl/api/interviews" -Method POST -Body $createBody -ContentType "application/json" -WebSession $session -UseBasicParsing
$interviewId = ($createResp.Content | ConvertFrom-Json).id
Write-Host "Created Interview ID: $interviewId"

# 3. Test Posting Proctoring Alerts Payload (including Face Mismatch = 1 as in user screenshot)
$alertsBody = @{
    tabSwitchCount = 0
    fullscreenExitCount = 0
    externalDeviceCount = 0
    noFaceDetectedCount = 0
    eyesClosedCount = 0
    headTurnedCount = 0
    gazeOffScreenCount = 0
    multipleFacesCount = 0
    faceMismatchCount = 1
} | ConvertTo-Json

$alertResp = Invoke-WebRequest -Uri "$baseUrl/api/interviews/$interviewId/proctoring-alerts" -Method POST -Body $alertsBody -ContentType "application/json" -WebSession $session -UseBasicParsing
Write-Host "Posted Proctoring Alerts Status: $($alertResp.StatusCode)"

# 4. Complete Interview Session
$compResp = Invoke-WebRequest -Uri "$baseUrl/api/interviews/$interviewId/complete" -Method POST -WebSession $session -UseBasicParsing
Write-Host "Completed Interview Status: $($compResp.StatusCode)"

# 5. Fetch Report HTML & Verify 9-Grid Proctoring Alerts Console
$reportHtml = (Invoke-WebRequest -Uri "$baseUrl/interviews/$interviewId/report" -WebSession $session -UseBasicParsing).Content

$hasAlertsHeader = $reportHtml.Contains("Proctoring Alerts :")
$hasTabSwitch = $reportHtml.Contains("Tab Switching")
$hasFullscreen = $reportHtml.Contains("Full-Screen Exits")
$hasDevices = $reportHtml.Contains("External devices detected")
$hasNoFace = $reportHtml.Contains("No Face Detected")
$hasEyesClosed = $reportHtml.Contains("Eyes Closed")
$hasHeadTurned = $reportHtml.Contains("Head Turned")
$hasGazeOff = $reportHtml.Contains("Gaze Off Screen")
$hasMultipleFaces = $reportHtml.Contains("Multiple Faces")
$hasFaceMismatch = $reportHtml.Contains("Face Mismatch")

Write-Host "`n=== Checking Report Card HTML for Proctoring Alerts Grid ==="
Write-Host "Report has 'Proctoring Alerts :' Header: $hasAlertsHeader"
Write-Host "Report has Tab Switching: $hasTabSwitch"
Write-Host "Report has Full-Screen Exits: $hasFullscreen"
Write-Host "Report has External devices detected: $hasDevices"
Write-Host "Report has No Face Detected: $hasNoFace"
Write-Host "Report has Eyes Closed: $hasEyesClosed"
Write-Host "Report has Head Turned: $hasHeadTurned"
Write-Host "Report has Gaze Off Screen: $hasGazeOff"
Write-Host "Report has Multiple Faces: $hasMultipleFaces"
Write-Host "Report has Face Mismatch: $hasFaceMismatch"

if ($hasAlertsHeader -and $hasFaceMismatch -and $hasMultipleFaces) {
    Write-Host "`n======================================================="
    Write-Host "ALL PROCTORING ALERTS & FACE MISMATCH TESTS PASSED!"
    Write-Host "======================================================="
} else {
    Write-Error "Test failed: Report HTML is missing proctoring alert elements."
}
