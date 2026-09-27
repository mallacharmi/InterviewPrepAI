$baseUrl = "http://localhost:8080"
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession

# Register new user
$regPage = Invoke-WebRequest -Uri "$baseUrl/register" -SessionVariable session -UseBasicParsing
$csrf = ""
if ($regPage.Content -match 'name="_csrf" value="([^"]+)"') {
    $csrf = $matches[1]
}

$uEmail = "dashuser$(Get-Random)@example.com"
$regPost = Invoke-WebRequest -Uri "$baseUrl/register" -Method Post -Body @{
    name = "Dash Tester"
    email = $uEmail
    password = "Password123!"
    targetRole = "Java Developer"
    experienceLevel = "MID_LEVEL"
    _csrf = $csrf
} -WebSession $session -UseBasicParsing -MaximumRedirection 5

# Login
$loginPage = Invoke-WebRequest -Uri "$baseUrl/login" -SessionVariable session -UseBasicParsing
if ($loginPage.Content -match 'name="_csrf" value="([^"]+)"') {
    $csrf = $matches[1]
}

$loginRes = Invoke-WebRequest -Uri "$baseUrl/login" -Method Post -Body @{
    email = $uEmail
    password = "Password123!"
    _csrf = $csrf
} -WebSession $session -UseBasicParsing -MaximumRedirection 5

# Fetch Dashboard
$dashRes = Invoke-WebRequest -Uri "$baseUrl/dashboard" -WebSession $session -UseBasicParsing
Write-Host "Dashboard HTTP Status:" $dashRes.StatusCode

$hasOverall = $dashRes.Content.Contains('Overall Percentage')
$hasAvgMaint = $dashRes.Content.Contains('Average % (Maintenance)')
$hasHighest = $dashRes.Content.Contains('Highest %')
$hasMonth = $dashRes.Content.Contains('id="monthSelect"')
$hasYear = $dashRes.Content.Contains('id="yearSelect"')
$hasType = $dashRes.Content.Contains('id="typeFilterSelect"')
$hasDuplicateSection4 = $dashRes.Content.Contains('Average Performance Maintenance')

Write-Host "Contains Overall Percentage: $hasOverall"
Write-Host "Contains Average % (Maintenance): $hasAvgMaint"
Write-Host "Contains Highest %: $hasHighest"
Write-Host "Contains Month Select: $hasMonth"
Write-Host "Contains Year Select: $hasYear"
Write-Host "Contains Type Filter Select: $hasType"
Write-Host "Has Old Duplicate Section 4: $hasDuplicateSection4"

if ($hasOverall -and $hasAvgMaint -and $hasHighest -and $hasMonth -and $hasYear -and $hasType -and (-not $hasDuplicateSection4)) {
    Write-Host "DASHBOARD VERIFICATION SUCCESSFUL!"
} else {
    Write-Error "DASHBOARD VERIFICATION FAILED!"
}
