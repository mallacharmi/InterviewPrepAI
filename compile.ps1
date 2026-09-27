$jars = Get-ChildItem -Recurse 'C:\Users\Charmi\.m2\repository\*.jar' | ForEach-Object { $_.FullName }
$cp = ("target/classes" + [IO.Path]::PathSeparator + ($jars -join [IO.Path]::PathSeparator))
Set-Content -Path "cp.txt" -Value "-classpath`n$cp"
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot"
$sources = (Get-ChildItem -Recurse 'src/main/java/*.java').FullName
& "$env:JAVA_HOME\bin\javac.exe" -encoding UTF-8 -parameters "@cp.txt" -d target/classes $sources
Remove-Item "cp.txt" -Force
Write-Host "Compilation finished with code $LASTEXITCODE"
