param(
    [string[]]$Tasks = @('assembleDebug', 'lintDebug'),
    [switch]$Offline
)
$ErrorActionPreference = 'Stop'
if (-not $env:JAVA_HOME) { throw 'Set JAVA_HOME to a JDK 17 installation before building.' }
$foliJava = Join-Path $env:JAVA_HOME 'bin\java.exe'
if (-not (Test-Path -LiteralPath $foliJava)) { throw 'JAVA_HOME does not contain bin\java.exe.' }
Push-Location $PSScriptRoot
try {
    # Invoke Java directly to retain Unicode workspace paths on Windows.
    $foliArguments = @('-Dfile.encoding=UTF-8', '-Dorg.gradle.internal.http.connectionTimeout=10000', '-Dorg.gradle.internal.http.socketTimeout=15000', '-cp', 'gradle/wrapper/gradle-wrapper.jar', 'org.gradle.wrapper.GradleWrapperMain', '--no-daemon')
    if ($Offline) { $foliArguments += '--offline' }
    & $foliJava @foliArguments @Tasks
    if ($LASTEXITCODE -ne 0) { throw "Android build exited with code $LASTEXITCODE" }
} finally { Pop-Location }
