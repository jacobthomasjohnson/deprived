$ErrorActionPreference = 'Stop'
$jdk = Get-ChildItem -LiteralPath "$PSScriptRoot\.tooling\jdk21" -Directory | Select-Object -First 1
if ($jdk) {
    $env:JAVA_HOME = $jdk.FullName
    $env:Path = "$env:JAVA_HOME\bin;$env:Path"
} elseif (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw 'Java 17 or newer was not found. Install a JDK or add it to PATH.'
}
& "$PSScriptRoot\gradlew.bat" build --no-daemon --console=plain
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Get-ChildItem -LiteralPath "$PSScriptRoot\..\mods" -Filter 'deprived-*.jar' | Remove-Item -Force
$jar = Get-ChildItem -LiteralPath "$PSScriptRoot\build\libs" -Filter 'deprived-*.jar' | Where-Object { $_.Name -notlike '*-sources.jar' } | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $jar) { throw 'Built mod JAR was not found.' }
Copy-Item -LiteralPath $jar.FullName -Destination "$PSScriptRoot\..\mods\$($jar.Name)" -Force
Write-Host "Built and installed mods\$($jar.Name)"
