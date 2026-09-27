$baseUrl = "http://localhost:8080"
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession

Write-Host "=== 1. Register and Login ==="
$regPage = Invoke-WebRequest -Uri "$baseUrl/register" -SessionVariable session -UseBasicParsing
$csrf = ""
if ($regPage.Content -match 'name="_csrf" value="([^"]+)"') {
    $csrf = $matches[1]
}

$uEmail = "deluser$(Get-Random)@example.com"
$regPost = Invoke-WebRequest -Uri "$baseUrl/register" -Method Post -Body @{
    name = "Delete Tester"
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
Write-Host "Login response status:" $loginRes.StatusCode

Write-Host "`n=== 2. Create 3 Sample Mock Interviews ==="
$dashPage = Invoke-WebRequest -Uri "$baseUrl/dashboard" -WebSession $session -UseBasicParsing
$apiCsrf = ""
if ($dashPage.Content -match 'name="_csrf" content="([^"]+)"') {
    $apiCsrf = $matches[1]
} elseif ($dashPage.Content -match 'name="_csrf" value="([^"]+)"') {
    $apiCsrf = $matches[1]
}

$headers = @{ "Content-Type" = "application/json" }
if ($apiCsrf) { $headers["X-CSRF-TOKEN"] = $apiCsrf }

function Create-Interview($role, $type, $topics) {
    $body = @{
        targetRole = $role
        interviewType = $type
        difficulty = "MEDIUM"
        totalQuestions = 5
        topics = $topics
    } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "$baseUrl/api/interviews" -Method Post -Body $body -Headers $headers -WebSession $session
    return $res.id
}

$id1 = Create-Interview "Java Developer" "TECHNICAL" @("Java", "OOP")
$id2 = Create-Interview "HR Specialist" "HR" @("Leadership", "Communication")
$id3 = Create-Interview "Backend Engineer" "MIXED" @("Architecture", "SQL")

Write-Host "Created Interview 1 ID: $id1"
Write-Host "Created Interview 2 ID: $id2"
Write-Host "Created Interview 3 ID: $id3"

Write-Host "`n=== 3. Check History HTML for Selective Delete UI Elements ==="
$histPage = Invoke-WebRequest -Uri "$baseUrl/interviews/history" -WebSession $session -UseBasicParsing
$hasSelectAll = $histPage.Content.Contains('id="selectAllCheckbox"')
$hasDeleteSelectedBtn = $histPage.Content.Contains('id="deleteSelectedBtn"')
$hasSelectedCount = $histPage.Content.Contains('id="selectedCount"')
$hasToggleSelectAll = $histPage.Content.Contains('toggleSelectAll')
$hasDeleteSelectedInterviews = $histPage.Content.Contains('deleteSelectedInterviews')
$hasDeleteSingleInterview = $histPage.Content.Contains('deleteSingleInterview')
$hasRow1 = $histPage.Content.Contains("row-interview-$id1")
$hasRow2 = $histPage.Content.Contains("row-interview-$id2")
$hasRow3 = $histPage.Content.Contains("row-interview-$id3")

Write-Host "History has Select All checkbox: $hasSelectAll"
Write-Host "History has Delete Selected button: $hasDeleteSelectedBtn"
Write-Host "History has Selected Count span: $hasSelectedCount"
Write-Host "History has toggleSelectAll JS: $hasToggleSelectAll"
Write-Host "History has deleteSelectedInterviews JS: $hasDeleteSelectedInterviews"
Write-Host "History has deleteSingleInterview JS: $hasDeleteSingleInterview"
Write-Host "Interview 1 in history: $hasRow1"
Write-Host "Interview 2 in history: $hasRow2"
Write-Host "Interview 3 in history: $hasRow3"

if (-not ($hasSelectAll -and $hasDeleteSelectedBtn -and $hasSelectedCount -and $hasToggleSelectAll -and $hasDeleteSelectedInterviews -and $hasDeleteSingleInterview -and $hasRow1 -and $hasRow2 -and $hasRow3)) {
    Write-Error "History page missing selective deletion UI elements or rows!"
    exit 1
}


Write-Host "`n=== 4. Test Single Interview Deletion (DELETE /api/interviews/$id1) ==="
$del1Res = Invoke-RestMethod -Uri "$baseUrl/api/interviews/$id1" -Method Delete -Headers $headers -WebSession $session
Write-Host "Single Delete Response: $($del1Res.status) - $($del1Res.message)"

$histAfterDel1 = Invoke-WebRequest -Uri "$baseUrl/interviews/history" -WebSession $session -UseBasicParsing
$has1After = $histAfterDel1.Content.Contains("row-interview-$id1")
$has2After = $histAfterDel1.Content.Contains("row-interview-$id2")
$has3After = $histAfterDel1.Content.Contains("row-interview-$id3")

Write-Host "Interview 1 removed from history: $(-not $has1After)"
Write-Host "Interview 2 still present: $has2After"
Write-Host "Interview 3 still present: $has3After"

if ($has1After -or (-not $has2After) -or (-not $has3After)) {
    Write-Error "Single interview deletion failed to remove only Interview 1!"
    exit 1
}

Write-Host "`n=== 5. Test Batch Selected Interviews Deletion (POST /api/interviews/batch-delete) ==="
$batchBody = @{
    interviewIds = @($id2)
} | ConvertTo-Json

$batchRes = Invoke-RestMethod -Uri "$baseUrl/api/interviews/batch-delete" -Method Post -Body $batchBody -Headers $headers -WebSession $session
Write-Host "Batch Delete Response: $($batchRes.status) - $($batchRes.message) (deletedCount: $($batchRes.deletedCount))"

$histAfterBatch = Invoke-WebRequest -Uri "$baseUrl/interviews/history" -WebSession $session -UseBasicParsing
$has2AfterBatch = $histAfterBatch.Content.Contains("row-interview-$id2")
$has3AfterBatch = $histAfterBatch.Content.Contains("row-interview-$id3")

Write-Host "Interview 2 removed from history: $(-not $has2AfterBatch)"
Write-Host "Interview 3 still present: $has3AfterBatch"

if ($has2AfterBatch -or (-not $has3AfterBatch)) {
    Write-Error "Batch selective interview deletion failed to remove selected Interview 2!"
    exit 1
}

Write-Host "`n=== 6. Delete Final Remaining Interview (Batch Delete) ==="
$batchBody2 = @{
    interviewIds = @($id3)
} | ConvertTo-Json

$batchRes2 = Invoke-RestMethod -Uri "$baseUrl/api/interviews/batch-delete" -Method Post -Body $batchBody2 -Headers $headers -WebSession $session
Write-Host "Batch Delete Final Response: $($batchRes2.status) - $($batchRes2.message)"

$histAfterFinal = Invoke-WebRequest -Uri "$baseUrl/interviews/history" -WebSession $session -UseBasicParsing
$hasEmptyMsg = $histAfterFinal.Content.Contains("You have no interview history yet")
Write-Host "Zero-state banner shown when all interviews are deleted: $hasEmptyMsg"

if (-not $hasEmptyMsg) {
    Write-Error "History zero-state banner not rendered after deleting all interviews!"
    exit 1
}

Write-Host "`n======================================================="
Write-Host "ALL SELECTIVE INTERVIEW DELETION TESTS PASSED SUCCESSFULLY!"
Write-Host "======================================================="
