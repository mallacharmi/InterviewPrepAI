$ErrorActionPreference = "Stop"

Write-Host "======================================================="
Write-Host "TEST SUITE: PROFILE PHOTO UPLOAD & INTERVIEW FACE MATCHING"
Write-Host "======================================================="

$baseUrl = "http://localhost:8080"

# Generate synthetic candidate face image using System.Drawing
Add-Type -AssemblyName System.Drawing
$bmp = New-Object System.Drawing.Bitmap 64, 64
$gfx = [System.Drawing.Graphics]::FromImage($bmp)
$gfx.Clear([System.Drawing.Color]::FromArgb(40, 50, 70))
$brushFace = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(235, 185, 155))
$gfx.FillEllipse($brushFace, 12, 10, 40, 48)
$brushEye = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(30, 30, 40))
$gfx.FillEllipse($brushEye, 20, 24, 6, 6)
$gfx.FillEllipse($brushEye, 38, 24, 6, 6)
$brushMouth = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(180, 70, 70))
$gfx.FillEllipse($brushMouth, 24, 42, 16, 6)
$gfx.Dispose()
$ms = New-Object System.IO.MemoryStream
$bmp.Save($ms, [System.Drawing.Imaging.ImageFormat]::Jpeg)
$samplePhotoBase64 = "data:image/jpeg;base64," + [Convert]::ToBase64String($ms.ToArray())
$samplePhotoBase64Match = $samplePhotoBase64
$ms.Dispose()
$bmp.Dispose()

# Generate friend / impostor face image (different eye positions, smaller face, different skin undertone & background)
$bmpFriend = New-Object System.Drawing.Bitmap 64, 64
$gfxFriend = [System.Drawing.Graphics]::FromImage($bmpFriend)
$gfxFriend.Clear([System.Drawing.Color]::FromArgb(90, 40, 30))
$brushFriendFace = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(150, 180, 210))
$gfxFriend.FillEllipse($brushFriendFace, 18, 14, 28, 42)
$brushFriendEye = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(10, 10, 15))
$gfxFriend.FillEllipse($brushFriendEye, 23, 28, 4, 4)
$gfxFriend.FillEllipse($brushFriendEye, 37, 28, 4, 4)
$brushFriendMouth = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(150, 50, 50))
$gfxFriend.FillEllipse($brushFriendMouth, 26, 45, 12, 4)
$gfxFriend.Dispose()
$msFriend = New-Object System.IO.MemoryStream
$bmpFriend.Save($msFriend, [System.Drawing.Imaging.ImageFormat]::Jpeg)
$friendPhotoBase64 = "data:image/jpeg;base64," + [Convert]::ToBase64String($msFriend.ToArray())
$msFriend.Dispose()
$bmpFriend.Dispose()

# Blank covered camera image (all black, zero variance)
$bmpBlank = New-Object System.Drawing.Bitmap 64, 64
$gfxBlank = [System.Drawing.Graphics]::FromImage($bmpBlank)
$gfxBlank.Clear([System.Drawing.Color]::Black)
$gfxBlank.Dispose()
$msBlank = New-Object System.IO.MemoryStream
$bmpBlank.Save($msBlank, [System.Drawing.Imaging.ImageFormat]::Jpeg)
$blankImage = "data:image/jpeg;base64," + [Convert]::ToBase64String($msBlank.ToArray())
$msBlank.Dispose()
$bmpBlank.Dispose()

# 1. Check Register Page HTML for Photo Upload & Webcam Snapshot Elements
Write-Host "`n=== 1. Checking Register Page HTML for Photo Elements ==="
$regPage = Invoke-WebRequest -Uri "$baseUrl/register" -UseBasicParsing
$hasPhotoWrapper = $regPage.Content.Contains('id="photoGroupWrapper"')
$hasProfilePhotoInput = $regPage.Content.Contains('id="profilePhoto"')
$hasUploadBtn = $regPage.Content.Contains('Upload Photo File')
$hasCamSnapBtn = $regPage.Content.Contains('Snap via Webcam')
$hasWebcamDrawer = $regPage.Content.Contains('id="regWebcamDrawer"')

Write-Host "Register has Photo Wrapper: $hasPhotoWrapper"
Write-Host "Register has profilePhoto Input: $hasProfilePhotoInput"
Write-Host "Register has Upload Button: $hasUploadBtn"
Write-Host "Register has Camera Snap Button: $hasCamSnapBtn"
Write-Host "Register has Webcam Drawer: $hasWebcamDrawer"

if (-not ($hasPhotoWrapper -and $hasProfilePhotoInput -and $hasUploadBtn)) {
    throw "Register page is missing photo upload elements!"
}

# 2. Register New Candidate with Photo
Write-Host "`n=== 2. Registering Candidate with Verification Photo ==="
$uniqueId = Get-Random -Minimum 1000 -Maximum 9999
$email = "photouser$uniqueId@example.com"
$password = "PhotoPass@123"
$regPayload = @{
    name = "Alex Mercer"
    email = $email
    password = $password
    targetRole = "Java Developer"
    experienceLevel = "MID"
    profilePhoto = $samplePhotoBase64
} | ConvertTo-Json

$regResp = Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Body $regPayload -ContentType "application/json"
Write-Host "Registered User ID: $($regResp.id), Name: $($regResp.name)"
Write-Host "User has Profile Photo in response: $([string]::IsNullOrEmpty($regResp.profilePhoto) -eq $false)"

if ([string]::IsNullOrEmpty($regResp.profilePhoto)) {
    throw "Registered user does not have profilePhoto set!"
}

# 3. Log In Candidate with Session & CSRF
Write-Host "`n=== 3. Logging in as Candidate ==="
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

# 4. Check Current User API for profilePhoto
Write-Host "`n=== 4. Fetching /api/users/me ==="
$meResp = Invoke-RestMethod -Uri "$baseUrl/api/users/me" -Method Get -WebSession $session -Headers $headers
Write-Host "Me API Name: $($meResp.name)"
Write-Host "Me API Photo present: $([string]::IsNullOrEmpty($meResp.profilePhoto) -eq $false)"

# 5. Check Profile Page HTML
Write-Host "`n=== 5. Checking Profile Page HTML ==="
$profilePage = Invoke-WebRequest -Uri "$baseUrl/profile" -Method Get -WebSession $session -UseBasicParsing
$profileHasPhotoCard = $profilePage.Content.Contains('Verification ID Photo')
$profileHasPhotoInput = $profilePage.Content.Contains('id="profilePhoto"')
$profileHasWebcamDrawer = $profilePage.Content.Contains('id="profileWebcamDrawer"')

Write-Host "Profile page has Verification Photo Card: $profileHasPhotoCard"
Write-Host "Profile page has profilePhoto input: $profileHasPhotoInput"
Write-Host "Profile page has Webcam Drawer: $profileHasWebcamDrawer"

if (-not ($profileHasPhotoCard -and $profileHasPhotoInput)) {
    throw "Profile page is missing photo elements!"
}

# 6. Update Profile with Updated Photo
Write-Host "`n=== 6. Updating Profile with New Photo ==="
$updatePayload = @{
    name = "Alex Mercer Senior"
    targetRole = "System Architect"
    experienceLevel = "SENIOR"
    reminderTime = "10:00"
    profilePhoto = $samplePhotoBase64Match
} | ConvertTo-Json

$updateResp = Invoke-RestMethod -Uri "$baseUrl/api/users/me" -Method Put -Body $updatePayload -ContentType "application/json" -WebSession $session -Headers $headers
Write-Host "Updated Profile Name: $($updateResp.name), Target Role: $($updateResp.targetRole)"
Write-Host "Updated Profile Photo present: $([string]::IsNullOrEmpty($updateResp.profilePhoto) -eq $false)"

# 7. Create Mock Interview
Write-Host "`n=== 7. Creating Interview Session ==="
$createInterviewPayload = @{
    targetRole = "System Architect"
    interviewType = "TECHNICAL"
    difficulty = "HARD"
    totalQuestions = 5
    topics = @("High Availability", "Distributed Systems")
} | ConvertTo-Json

$interviewResp = Invoke-RestMethod -Uri "$baseUrl/api/interviews" -Method Post -Body $createInterviewPayload -ContentType "application/json" -WebSession $session -Headers $headers
$interviewId = $interviewResp.id
Write-Host "Created Interview ID: $interviewId, Status: $($interviewResp.status)"

# 8. Test Face Verification API
Write-Host "`n=== 8. Testing Face Verification API (/api/users/me/verify-face) ==="
# Genuine candidate matching frame test
$verifyPayloadMatch = @{
    liveImage = $samplePhotoBase64Match
} | ConvertTo-Json

$verifyRespMatch = Invoke-RestMethod -Uri "$baseUrl/api/users/me/verify-face" -Method Post -Body $verifyPayloadMatch -ContentType "application/json" -WebSession $session -Headers $headers
Write-Host "Genuine Candidate Match Result: matched = $($verifyRespMatch.matched), similarityScore = $($verifyRespMatch.similarityScore)%"
Write-Host "Message: $($verifyRespMatch.message)"

if (-not $verifyRespMatch.matched -or $verifyRespMatch.similarityScore -lt 90.0) {
    throw "Genuine candidate face verification should have passed with score >= 90.0%! Got $($verifyRespMatch.similarityScore)%"
}

# Impostor / Friend seated test (simulating a friend trying to take the interview)
$verifyPayloadFriend = @{
    liveImage = $friendPhotoBase64
} | ConvertTo-Json

$verifyRespFriend = Invoke-RestMethod -Uri "$baseUrl/api/users/me/verify-face" -Method Post -Body $verifyPayloadFriend -ContentType "application/json" -WebSession $session -Headers $headers
Write-Host "Friend / Impostor Test Result: matched = $($verifyRespFriend.matched), similarityScore = $($verifyRespFriend.similarityScore)%"
Write-Host "Message: $($verifyRespFriend.message)"

if ($verifyRespFriend.matched -eq $true -or $verifyRespFriend.similarityScore -ge 90.0) {
    throw "Friend/impostor must NOT pass verification! Expected matched=false and score < 90.0%, but got matched=$($verifyRespFriend.matched), score=$($verifyRespFriend.similarityScore)%"
}
Write-Host "SUCCESS: Friend/impostor was successfully REJECTED ($($verifyRespFriend.similarityScore)% < 90.0%)!"

# Blank/no-contrast image test (simulates camera covered)
$verifyPayloadBlank = @{
    liveImage = $blankImage
} | ConvertTo-Json

$verifyRespBlank = Invoke-RestMethod -Uri "$baseUrl/api/users/me/verify-face" -Method Post -Body $verifyPayloadBlank -ContentType "application/json" -WebSession $session -Headers $headers
Write-Host "Blank Camera Test Result: matched = $($verifyRespBlank.matched), faceDetected = $($verifyRespBlank.faceDetected), similarityScore = $($verifyRespBlank.similarityScore)%"
Write-Host "Message: $($verifyRespBlank.message)"

if ($verifyRespBlank.matched -eq $true) {
    throw "Blank camera frame should NOT match!"
}

# 9. Verify Live Interview Screen UI Gate Elements
Write-Host "`n=== 9. Checking Interview Page HTML for Face Verification Gate ==="
$interviewPage = Invoke-WebRequest -Uri "$baseUrl/interviews/$interviewId" -Method Get -WebSession $session -UseBasicParsing

$hasOverlayTitle = $interviewPage.Content.Contains('Candidate Identity Verification')
$hasRegPhotoImg = $interviewPage.Content.Contains('id="verifyRegPhotoImg"')
$hasLiveVideo = $interviewPage.Content.Contains('id="verifyLiveVideo"')
$hasStartVerifyBtn = $interviewPage.Content.Contains('id="startVerifyCamBtn"')
$hasFaceMatchBanner = $interviewPage.Content.Contains('id="faceMatchBanner"')
$hasLockedLaunchBtn = $interviewPage.Content.Contains('id="enable-proctoring-btn"') -and $interviewPage.Content.Contains('disabled')
$hasVerifyScript = $interviewPage.Content.Contains('captureAndVerifyFace')
$hasStartCameraScript = $interviewPage.Content.Contains('startVerificationCamera')
$has90PercentMention = $interviewPage.Content.Contains('90.0%') -or $interviewPage.Content.Contains('90%')
$hasThresholdMarker = $interviewPage.Content.Contains('threshold-marker')

Write-Host "Interview has Identity Verification Overlay: $hasOverlayTitle"
Write-Host "Interview has Registered Photo Element: $hasRegPhotoImg"
Write-Host "Interview has Live Camera Video Element: $hasLiveVideo"
Write-Host "Interview has Start Camera & Match Button: $hasStartVerifyBtn"
Write-Host "Interview has Face Match Progress Banner: $hasFaceMatchBanner"
Write-Host "Interview Launch Button is LOCKED (disabled initially): $hasLockedLaunchBtn"
Write-Host "Interview has captureAndVerifyFace JS: $hasVerifyScript"
Write-Host "Interview has startVerificationCamera JS: $hasStartCameraScript"
Write-Host "Interview enforces 90% threshold in UI: $has90PercentMention"
Write-Host "Interview has 90% threshold visual marker: $hasThresholdMarker"

if (-not ($hasOverlayTitle -and $hasRegPhotoImg -and $hasLiveVideo -and $hasLockedLaunchBtn -and $hasVerifyScript -and $has90PercentMention)) {
    throw "Interview page is missing identity verification proctoring gate elements or 90% threshold specification!"
}

# 10. Start Interview via API to verify full life-cycle
Write-Host "`n=== 10. Testing Interview Start Endpoint ==="
$startResp = Invoke-RestMethod -Uri "$baseUrl/api/interviews/$interviewId/start" -Method Post -WebSession $session -Headers $headers
Write-Host "Interview Status after verified start: $($startResp.status)"

Write-Host "`n======================================================="
Write-Host "ALL PHOTO UPLOAD & FACE MATCHING TESTS PASSED SUCCESSFULLY!"
Write-Host "======================================================="
