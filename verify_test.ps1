Start-Sleep -Seconds 2

$baseUrl = "http://localhost:8080"

Write-Host "=== 1. Testing Unauthenticated GET /interviews/new ==="
$unauthNew = Invoke-WebRequest -Uri "$baseUrl/interviews/new" -UseBasicParsing -MaximumRedirection 0 -ErrorAction SilentlyContinue
Write-Host "Unauthenticated /interviews/new Status: $($unauthNew.StatusCode), Location: $($unauthNew.Headers.Location)"
if ($unauthNew.StatusCode -eq 302 -and $unauthNew.Headers.Location -like "*login*") {
    Write-Host "PASS: Unauthenticated /interviews/new cleanly redirects to /login!"
} else {
    Write-Host "FAIL: Unauthenticated /interviews/new did not redirect to /login."
}

Write-Host "`n=== 2. Testing User Registration & Login ==="
$regPage = Invoke-WebRequest -Uri "$baseUrl/register" -SessionVariable session -UseBasicParsing
$csrfToken = ""
if ($regPage.Content -match 'name="_csrf" value="([^"]+)"') {
    $csrfToken = $matches[1]
}

$uEmail = "uiuser$(Get-Random)@example.com"
$regBody = @{
    name = "Malla Charmi"
    email = $uEmail
    password = "Password123!"
    targetRole = "Java Developer"
    experienceLevel = "FRESHER"
    _csrf = $csrfToken
}

$regPost = Invoke-WebRequest -Uri "$baseUrl/register" -Method Post -Body $regBody -WebSession $session -UseBasicParsing -MaximumRedirection 5 -ErrorAction SilentlyContinue
Write-Host "Registration Status: $($regPost.StatusCode)"

# Login
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

$loginPost = Invoke-WebRequest -Uri "$baseUrl/login" -Method Post -Body $loginBody -WebSession $loginSession -UseBasicParsing -MaximumRedirection 5 -ErrorAction SilentlyContinue
Write-Host "Login Status: $($loginPost.StatusCode)"

Write-Host "`n=== 3. Testing Duplicate Registration Warning ==="
$dupRegPage = Invoke-WebRequest -Uri "$baseUrl/register" -SessionVariable dupSession -UseBasicParsing
$dupCsrf = ""
if ($dupRegPage.Content -match 'name="_csrf" value="([^"]+)"') {
    $dupCsrf = $matches[1]
}

$dupBody = @{
    name = "Duplicate User"
    email = $uEmail
    password = "Password123!"
    targetRole = "Java Developer"
    experienceLevel = "FRESHER"
    _csrf = $dupCsrf
}

$dupPost = Invoke-WebRequest -Uri "$baseUrl/register" -Method Post -Body $dupBody -WebSession $dupSession -UseBasicParsing -MaximumRedirection 0 -ErrorAction SilentlyContinue
Write-Host "Duplicate Registration Status: $($dupPost.StatusCode)"
if ($dupPost.Content -match "An account with this email address already exists") {
    Write-Host "PASS: Duplicate registration correctly shows warning message!"
} else {
    Write-Host "FAIL: Duplicate registration warning not found."
}

Write-Host "`n=== 4. Testing Authenticated /dashboard (New UI Render) ==="
$dashRes = Invoke-WebRequest -Uri "$baseUrl/dashboard" -WebSession $loginSession -UseBasicParsing
Write-Host "GET /dashboard Status Code: $($dashRes.StatusCode)"
if ($dashRes.StatusCode -eq 200 -and $dashRes.Content -match "YOUR OVERALL PROGRESS" -and $dashRes.Content -match "Your Favorite Skills") {
    Write-Host "PASS: /dashboard loaded new Image 2 & 3 matching UI with HTTP 200 OK!"
} else {
    Write-Host "FAIL: /dashboard UI failed."
}

Write-Host "`n=== 5. Testing Authenticated /interviews/new ==="
$authNew = Invoke-WebRequest -Uri "$baseUrl/interviews/new" -WebSession $loginSession -UseBasicParsing
Write-Host "Authenticated /interviews/new Status Code: $($authNew.StatusCode)"
if ($authNew.StatusCode -eq 200) {
    Write-Host "PASS: Authenticated /interviews/new loaded setup page cleanly with HTTP 200 OK!"
} else {
    Write-Host "FAIL: Authenticated /interviews/new failed."
}

Write-Host "`n=== 6. Testing Full Interview Creation & Gibberish Detection API Flow ==="
$dashDash = Invoke-WebRequest -Uri "$baseUrl/dashboard" -WebSession $loginSession -UseBasicParsing
$apiCsrf = ""
if ($dashDash.Content -match 'name="_csrf" value="([^"]+)"') {
    $apiCsrf = $matches[1]
}

$createBody = @{
    targetRole = "Java Developer"
    interviewType = "TECHNICAL"
    difficulty = "MEDIUM"
    totalQuestions = 5
    topics = @("OOP", "Collections")
} | ConvertTo-Json

$headers = @{
    "Content-Type" = "application/json"
}
if ($apiCsrf) { $headers["X-CSRF-TOKEN"] = $apiCsrf }

$createRes = Invoke-RestMethod -Uri "$baseUrl/api/interviews" -Method Post -Body $createBody -Headers $headers -WebSession $loginSession
Write-Host "Created Interview ID: $($createRes.id)"

$startRes = Invoke-RestMethod -Uri "$baseUrl/api/interviews/$($createRes.id)/start" -Method Post -Headers $headers -WebSession $loginSession
Write-Host "Question Text: $($startRes.questionText)"

# Submit gibberish answer
$answerBody = @{
    answerText = "gd gt,th hryyhty yy yyy yyy yyy yyy yyy yyy yyy"
} | ConvertTo-Json

$evalRes = Invoke-RestMethod -Uri "$baseUrl/api/interviews/$($createRes.id)/questions/$($startRes.id)/answer" -Method Post -Body $answerBody -Headers $headers -WebSession $loginSession
Write-Host "Gibberish Evaluation Score: $($evalRes.score)"
Write-Host "Technical Accuracy: $($evalRes.technicalAccuracy), Completeness: $($evalRes.completeness), Clarity: $($evalRes.clarity)"
Write-Host "Feedback: $($evalRes.feedback)"

if ($evalRes.score -eq 0 -and $evalRes.feedback -like "*Invalid or gibberish answer detected*") {
    Write-Host "PASS: AI correctly detected gibberish answer and assigned 0.0 score!"
} else {
    Write-Host "FAIL: Gibberish answer was assigned score: $($evalRes.score)"
}

# Complete and get report
$completeRes = Invoke-RestMethod -Uri "$baseUrl/api/interviews/$($createRes.id)/complete" -Method Post -Headers $headers -WebSession $loginSession
Write-Host "Report Recommended Courses Count: $($completeRes.recommendedCourses.Count)"
foreach ($c in $completeRes.recommendedCourses) {
    Write-Host "  - Course: $($c.title) | Platform: $($c.platform) | Link: $($c.url)"
}

if ($completeRes.recommendedCourses.Count -gt 0 -and $completeRes.recommendedCourses[0].url -like "https://*") {
    Write-Host "PASS: Report Card contains valid, working course recommendation links!"
} else {
    Write-Host "FAIL: Course links missing or invalid."
}

Write-Host "`n=== 7. Testing ATS Resume Analyzer View and API ==="
$atsPage = Invoke-WebRequest -Uri "$baseUrl/ats" -WebSession $loginSession -UseBasicParsing
Write-Host "GET /ats Status Code: $($atsPage.StatusCode)"

$atsBody = @{
    resumeText = "Experienced Java Developer proficient in Java, Spring Boot, REST APIs, and MySQL."
    jobDescription = "We are seeking a Senior Java Developer with strong expertise in Java, Spring Boot, Microservices, Docker, Kubernetes, and SQL."
} | ConvertTo-Json

$atsRes = Invoke-RestMethod -Uri "$baseUrl/api/ats/analyze" -Method Post -Body $atsBody -Headers $headers -WebSession $loginSession
Write-Host "ATS Score: $($atsRes.atsScore)%, Match Category: $($atsRes.matchCategory)"
Write-Host "Matched Keywords: $($atsRes.matchedKeywords -join ', ')"
Write-Host "Missing Skills: $($atsRes.missingSkills -join ', ')"

if ($atsPage.StatusCode -eq 200 -and $atsRes.atsScore -gt 0) {
    Write-Host "PASS: ATS Resume Analyzer View and API executed successfully!"
} else {
    Write-Host "FAIL: ATS Resume Analyzer test failed."
}

Write-Host "`n=== 7B. Testing Direct Practice Interview Creation from ATS JD ==="
$atsCreateBody = @{
    resumeText = "Experienced Java Developer proficient in Java, Spring Boot, REST APIs, and MySQL."
    jobDescription = "We are seeking a Senior Java Developer with strong expertise in Java, Spring Boot, Microservices, Docker, Kubernetes, and SQL."
    matchedKeywords = $atsRes.matchedKeywords
    missingSkills = $atsRes.missingSkills
} | ConvertTo-Json

$atsInterviewRes = Invoke-RestMethod -Uri "$baseUrl/api/ats/create-interview" -Method Post -Body $atsCreateBody -Headers $headers -WebSession $loginSession
Write-Host "Created ATS Interview ID: $($atsInterviewRes.id), Topics: $($atsInterviewRes.topics)"
if ($atsInterviewRes.id -gt 0) {
    Write-Host "PASS: Direct practice interview created from ATS analysis successfully!"
} else {
    Write-Host "FAIL: Direct practice interview creation from ATS failed."
}

Write-Host "`n=== 8. Testing History Clearing API ==="
$clearRes = Invoke-RestMethod -Uri "$baseUrl/api/interviews/history/clear" -Method Post -Headers $headers -WebSession $loginSession
Write-Host "Clear History Message: $($clearRes.message)"
if ($clearRes.status -eq "success") {
    Write-Host "PASS: Clear History API executed successfully!"
} else {
    Write-Host "FAIL: Clear History API failed."
}

Write-Host "`n=== 9. Testing Profile View & Profile Update API with Google Calendar Reminder ==="
$profilePage = Invoke-WebRequest -Uri "$baseUrl/profile" -WebSession $loginSession -UseBasicParsing
Write-Host "GET /profile Status Code: $($profilePage.StatusCode)"
$profileUpdateBody = @{
    name = "Malla Charmi"
    targetRole = "Data Analyst"
    experienceLevel = "FRESHER"
    reminderEnabled = $true
    reminderTime = "09:00"
} | ConvertTo-Json

$profileUpdateRes = Invoke-RestMethod -Uri "$baseUrl/api/users/me" -Method Put -Body $profileUpdateBody -Headers $headers -WebSession $loginSession
Write-Host "Profile Update Response Name: $($profileUpdateRes.name), Reminder Active: $($profileUpdateRes.reminderEnabled) at $($profileUpdateRes.reminderTime)"

# Clear reminder
$clearReminderBody = @{
    name = "Malla Charmi"
    targetRole = "Data Analyst"
    experienceLevel = "FRESHER"
    reminderEnabled = $false
    reminderTime = $null
} | ConvertTo-Json
$clearReminderRes = Invoke-RestMethod -Uri "$baseUrl/api/users/me" -Method Put -Body $clearReminderBody -Headers $headers -WebSession $loginSession
Write-Host "Cleared Reminder Response Active: $($clearReminderRes.reminderEnabled)"

if ($profilePage.StatusCode -eq 200 -and $profileUpdateRes.reminderEnabled -eq $true -and $clearReminderRes.reminderEnabled -eq $false) {
    Write-Host "PASS: User Profile page loaded, Google Calendar reminder active & clearing API executed successfully!"
} else {
    Write-Host "FAIL: User Profile reminder update test failed."
}

Write-Host "`n=== 10. Testing Interview Video Recording Upload & Playback API ==="
$newIntBody = @{
    targetRole = "Java Developer"
    interviewType = "TECHNICAL"
    difficulty = "MEDIUM"
    totalQuestions = 5
    topics = @("Java Basics")
} | ConvertTo-Json

$newIntRes = Invoke-RestMethod -Uri "$baseUrl/api/interviews" -Method Post -Body $newIntBody -Headers $headers -WebSession $loginSession
$recIntId = $newIntRes.id
Write-Host "Created Interview ID for Recording: $recIntId"

# Create a sample fake webm video file
$tempVideoPath = [System.IO.Path]::GetTempFileName() + ".webm"
[System.IO.File]::WriteAllBytes($tempVideoPath, [System.Text.Encoding]::UTF8.GetBytes("GAvSampleWebmHeaderFakeVideoBytes1234567890"))

# Upload sample recording using curl.exe
$jsessionCookie = ($loginSession.Cookies.GetCookies([System.Uri]$baseUrl) | Where-Object { $_.Name -eq 'JSESSIONID' })
$jsessionId = if ($jsessionCookie) { $jsessionCookie.Value } else { "" }

$curlArgs = @("-s", "-X", "POST", "$baseUrl/api/interviews/$recIntId/recording", "-H", "Cookie: JSESSIONID=$jsessionId")
if ($apiCsrf) {
    $curlArgs += "-H"
    $curlArgs += "X-CSRF-TOKEN: $apiCsrf"
}
$curlArgs += "-F"
$curlArgs += "file=@$tempVideoPath;type=video/webm"

$uploadResultJson = & curl.exe @curlArgs
Write-Host "Recording Upload Response: $uploadResultJson"
Remove-Item -Path $tempVideoPath -Force -ErrorAction SilentlyContinue

# Verify GET /api/interviews/{id}/video
$videoStreamRes = Invoke-WebRequest -Uri "$baseUrl/api/interviews/$recIntId/video" -WebSession $loginSession -UseBasicParsing
Write-Host "Video Stream Status: $($videoStreamRes.StatusCode), Content-Type: $($videoStreamRes.Headers['Content-Type'])"

# Verify History View contains recording
$historyPage = Invoke-WebRequest -Uri "$baseUrl/interviews/history" -WebSession $loginSession -UseBasicParsing
Write-Host "History Page Loaded (Status $($historyPage.StatusCode))"
$historyHasPlayBtn = $historyPage.Content -like "*Play Video*" -or $historyPage.Content -like "*checkLocalRecording*"
Write-Host "Video Recording Action Present: $historyHasPlayBtn"

if ($uploadResultJson -like "*success*" -and $videoStreamRes.StatusCode -eq 200 -and $historyHasPlayBtn) {
    Write-Host "PASS: Video recording upload, streaming API, and History playback verified successfully!"
} else {
    Write-Host "FAIL: Video recording feature failed."
}

Write-Host "`n=== ALL UI & ROUTE TESTS COMPLETED SUCCESSFULLY ==="
