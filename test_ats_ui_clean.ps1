$baseUrl = "http://localhost:8080"
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession

Write-Host "=== 1. Login and Fetch ATS Analyzer Page ==="
$loginPage = Invoke-WebRequest -Uri "$baseUrl/login" -SessionVariable session -UseBasicParsing
$csrf = ""
if ($loginPage.Content -match 'name="_csrf" value="([^"]+)"') {
    $csrf = $matches[1]
}

$uEmail = "atsuser$(Get-Random)@example.com"
$regPost = Invoke-WebRequest -Uri "$baseUrl/register" -Method Post -Body @{
    name = "ATS Clean Tester"
    email = $uEmail
    password = "Password123!"
    targetRole = "Java Developer"
    experienceLevel = "MID_LEVEL"
    _csrf = $csrf
} -WebSession $session -UseBasicParsing -MaximumRedirection 5

# Explicit login step
$loginPage2 = Invoke-WebRequest -Uri "$baseUrl/login" -SessionVariable session -UseBasicParsing
$loginCsrf = ""
if ($loginPage2.Content -match 'name="_csrf" value="([^"]+)"') {
    $loginCsrf = $matches[1]
}

$loginRes = Invoke-WebRequest -Uri "$baseUrl/login" -Method Post -Body @{
    email = $uEmail
    password = "Password123!"
    _csrf = $loginCsrf
} -WebSession $session -UseBasicParsing -MaximumRedirection 5
Write-Host "Login response status:" $loginRes.StatusCode

$atsPage = Invoke-WebRequest -Uri "$baseUrl/ats" -WebSession $session -UseBasicParsing
Write-Host "ATS Page Status:" $atsPage.StatusCode

Write-Host "`n=== 2. Verify Clean ATS UI Structure ==="
$hasDropzone = $atsPage.Content.Contains('id="fileUploadDropzone"')
$hasAttachedCard = $atsPage.Content.Contains('id="fileAttachedCard"')
$hasAttachedFileName = $atsPage.Content.Contains('id="attachedFileName"')
$hasRemoveBtn = $atsPage.Content.Contains('removeAttachedFile')
$hasHiddenResumeText = $atsPage.Content.Contains('id="resumeText" style="display: none;"')
$hasUploadTab = $atsPage.Content.Contains('id="tabUploadBtn"')
$hasPasteTab = $atsPage.Content.Contains('id="tabPasteBtn"')
$hasJobDescription = $atsPage.Content.Contains('id="jobDescription"')
$hasAnalyzeBtn = $atsPage.Content.Contains('id="atsBtn"')

Write-Host "Has File Upload Dropzone: $hasDropzone"
Write-Host "Has File Attached Preview Card: $hasAttachedCard"
Write-Host "Has Attached File Name Display: $hasAttachedFileName"
Write-Host "Has Remove Attached File Button: $hasRemoveBtn"
Write-Host "Resume Text is Hidden (Not dumping raw PDF text): $hasHiddenResumeText"
Write-Host "Has Upload Tab Button: $hasUploadTab"
Write-Host "Has Paste Tab Button: $hasPasteTab"
Write-Host "Has Job Description Textarea: $hasJobDescription"
Write-Host "Has Analyze Button: $hasAnalyzeBtn"

if (-not ($hasDropzone -and $hasAttachedCard -and $hasAttachedFileName -and $hasRemoveBtn -and $hasHiddenResumeText -and $hasUploadTab -and $hasPasteTab -and $hasJobDescription -and $hasAnalyzeBtn)) {
    Write-Error "ATS Analyzer page missing expected clean UI components!"
    exit 1
}

Write-Host "`n=== 3. Test ATS Analyze API with Sample Resume & JD ==="
$apiCsrf = ""
if ($atsPage.Content -match 'name="_csrf" content="([^"]+)"') {
    $apiCsrf = $matches[1]
} elseif ($atsPage.Content -match 'name="_csrf" value="([^"]+)"') {
    $apiCsrf = $matches[1]
}

$headers = @{ "Content-Type" = "application/json" }
if ($apiCsrf) { $headers["X-CSRF-TOKEN"] = $apiCsrf }

$reqBody = @{
    resumeText = "Senior Software Engineer with 6 years experience in Java, Spring Boot, Microservices, Hibernate, REST APIs, Docker, and PostgreSQL."
    jobDescription = "Seeking a Senior Java Developer proficient in Java, Spring Boot, Microservices, Kubernetes, Docker, and SQL."
} | ConvertTo-Json

$res = Invoke-RestMethod -Uri "$baseUrl/api/ats/analyze" -Method Post -Body $reqBody -Headers $headers -WebSession $session
Write-Host "ATS Score:" $res.atsScore "%"
Write-Host "Match Category:" $res.matchCategory
Write-Host "Matched Keywords:" ($res.matchedKeywords -join ", ")
Write-Host "Missing Skills:" ($res.missingSkills -join ", ")

if ($res.atsScore -lt 50) {
    Write-Error "Unexpected ATS score!"
    exit 1
}

Write-Host "`n======================================================="
Write-Host "ALL ATS ANALYZER CLEAN UI TESTS PASSED SUCCESSFULLY!"
Write-Host "======================================================="
