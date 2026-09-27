& "C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot\bin\jar.exe" xf target/ai-interview-prep-1.0.0.jar BOOT-INF/lib
$libJars = (Get-ChildItem -Path "BOOT-INF/lib/*.jar" | ForEach-Object { $_.FullName }) -join ";"

$lombokJar = "C:\Users\Charmi\.m2\repository\org\projectlombok\lombok\1.18.32\lombok-1.18.32.jar"
if (-not (Test-Path $lombokJar)) {
    $lombokJar = (Get-ChildItem -Path "C:\Users\Charmi\.m2\repository\org\projectlombok\lombok" -Recurse -Filter "*.jar" | Select-Object -First 1).FullName
}

$cp = "$libJars;$lombokJar;target/classes"

Write-Host "Compiling Java sources with javac..."
$javaFiles = Get-ChildItem -Path "src/main/java" -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
& "C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot\bin\javac.exe" -encoding UTF-8 -cp $cp -processorpath $lombokJar -d target/classes $javaFiles

if (Test-Path "BOOT-INF") { Remove-Item -Recurse -Force "BOOT-INF" }
Write-Host "Java compilation successful!"

# Run update_jar.ps1
powershell -ExecutionPolicy Bypass -File .\update_jar.ps1
