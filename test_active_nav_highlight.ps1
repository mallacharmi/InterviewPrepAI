$baseUrl = "http://localhost:8080"
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession

Write-Host "=== 1. Check CSS for Active and Hover Styles ==="
$cssRes = Invoke-WebRequest -Uri "$baseUrl/css/style.css" -UseBasicParsing
$hasActiveDark = $cssRes.Content.Contains('.nav-link.active')
$hasActiveLight = $cssRes.Content.Contains('[data-theme="light"] .nav-link.active')
$hasHover = $cssRes.Content.Contains('.nav-link:hover')

Write-Host "CSS has .nav-link.active (Dark Mode): $hasActiveDark"
Write-Host "CSS has [data-theme='light'] .nav-link.active (Light Mode): $hasActiveLight"
Write-Host "CSS has .nav-link:hover: $hasHover"

if (-not ($hasActiveDark -and $hasActiveLight -and $hasHover)) {
    Write-Error "Missing active or hover nav CSS rules!"
    exit 1
}

Write-Host "`n=== 2. Register and Login ==="
$regPage = Invoke-WebRequest -Uri "$baseUrl/register" -SessionVariable session -UseBasicParsing
$csrf = ""
if ($regPage.Content -match 'name="_csrf" value="([^"]+)"') {
    $csrf = $matches[1]
}

$uEmail = "navuser$(Get-Random)@example.com"
$regPost = Invoke-WebRequest -Uri "$baseUrl/register" -Method Post -Body @{
    name = "Nav Tester"
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

Write-Host "`n=== 3. Verify Navbar Structure Across All Pages ==="
$pages = @(
    @{ Name = "Dashboard"; Url = "$baseUrl/dashboard"; NavTarget = 'data-nav="dashboard"' },
    @{ Name = "New Interview"; Url = "$baseUrl/interviews/new"; NavTarget = 'data-nav="new-interview"' },
    @{ Name = "ATS Analyzer"; Url = "$baseUrl/ats"; NavTarget = 'data-nav="ats"' },
    @{ Name = "History"; Url = "$baseUrl/interviews/history"; NavTarget = 'data-nav="history"' },
    @{ Name = "Profile"; Url = "$baseUrl/profile"; NavTarget = 'data-nav="profile"' }
)

foreach ($p in $pages) {
    $res = Invoke-WebRequest -Uri $p.Url -WebSession $session -UseBasicParsing
    $hasNavTarget = $res.Content.Contains($p.NavTarget)
    $hasHighlightScript = $res.Content.Contains('updateActiveNavHighlight')
    Write-Host "$($p.Name) (Status $($res.StatusCode)): Contains $($p.NavTarget): $hasNavTarget, Has Script: $hasHighlightScript"
    
    if ($res.StatusCode -ne 200 -or -not $hasNavTarget -or -not $hasHighlightScript) {
        Write-Error "Failed check on page $($p.Name)!"
        exit 1
    }
}

Write-Host "`n======================================================="
Write-Host "ALL ACTIVE NAV HIGHLIGHT TESTS PASSED SUCCESSFULLY!"
Write-Host "======================================================="
