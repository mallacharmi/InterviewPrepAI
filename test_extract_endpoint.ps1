Start-Sleep -Seconds 2

$baseUrl = "http://localhost:8080"

Write-Host "=== Testing ATS Text Extraction API Endpoint with Auth Session ==="
$loginPage = Invoke-WebRequest -Uri "$baseUrl/login" -SessionVariable session -UseBasicParsing
$csrfToken = ""
if ($loginPage.Content -match 'name="_csrf" value="([^"]+)"') {
    $csrfToken = $matches[1]
}

# Register & Login new user
$uEmail = "extuser$(Get-Random)@example.com"
$regBody = @{
    name = "Extractor User"
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

# Create sample file
$sampleFile = "sample_resume.txt"
Set-Content -Path $sampleFile -Value "Malla Charmi - AI/ML Engineer with 3 years experience in Python, PyTorch, Java, Spring Boot, MySQL, and REST APIs."

# Prepare Multipart HTTP Request in PowerShell
Add-Type -AssemblyName System.Net.Http
$httpClientHandler = New-Object System.Net.Http.HttpClientHandler
$cookieContainer = New-Object System.Net.CookieContainer

# Transfer cookies from $loginSession
foreach ($cookie in $loginSession.Cookies.GetCookies($baseUrl)) {
    $cookieContainer.Add($cookie)
}
$httpClientHandler.CookieContainer = $cookieContainer

$httpClient = New-Object System.Net.Http.HttpClient($httpClientHandler)
if ($apiCsrf) {
    $httpClient.DefaultRequestHeaders.Add("X-CSRF-TOKEN", $apiCsrf)
}

$multipartContent = New-Object System.Net.Http.MultipartFormDataContent
$fileBytes = [System.IO.File]::ReadAllBytes((Get-Item $sampleFile).FullName)
$fileContent = New-Object System.Net.Http.ByteArrayContent(,$fileBytes)
$fileContent.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse("text/plain")
$multipartContent.Add($fileContent, "file", "sample_resume.txt")

$response = $httpClient.PostAsync("$baseUrl/api/ats/extract-resume-text", $multipartContent).Result
$resBody = $response.Content.ReadAsStringAsync().Result

Write-Host "Response Status: $($response.StatusCode)"
Write-Host "Extracted Content: $resBody"

Remove-Item $sampleFile -Force

if ($resBody -like "*Malla Charmi*") {
    Write-Host "`nPASS: Text extraction endpoint extracted actual file content cleanly!"
} else {
    Write-Host "`nFAIL: Extracted text failed."
}
