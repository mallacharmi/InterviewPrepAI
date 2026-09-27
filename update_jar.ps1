# Copy all compiled classes and resources into BOOT-INF/classes
if (Test-Path "BOOT-INF") { Remove-Item -Recurse -Force "BOOT-INF" }
New-Item -ItemType Directory -Force -Path "BOOT-INF/classes"

Copy-Item -Path "target/classes/*" -Destination "BOOT-INF/classes/" -Recurse -Force
Copy-Item -Path "src/main/resources/*" -Destination "BOOT-INF/classes/" -Recurse -Force

$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot"
& "$env:JAVA_HOME\bin\jar.exe" uf target/ai-interview-prep-1.0.0.jar -C . BOOT-INF

Remove-Item -Recurse -Force BOOT-INF
Write-Host "JAR successfully updated with all new UI and logic fixes!"
