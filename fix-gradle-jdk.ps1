# Fix Gradle JDK Configuration Script
# This script resolves "Invalid Gradle JDK configuration found" errors

Write-Host "🔧 Gradle JDK Configuration Fix" -ForegroundColor Green
Write-Host "=================================" -ForegroundColor Green
Write-Host ""

# 1. Check current Java installation
Write-Host "1️⃣ Checking Java installation..." -ForegroundColor Cyan
try {
    $javaVersion = java -version 2>&1 | Select-String "version"
    Write-Host "   ✅ Java found: $javaVersion" -ForegroundColor Green
    
    $javaHome = $env:JAVA_HOME
    if ($javaHome) {
        Write-Host "   ✅ JAVA_HOME: $javaHome" -ForegroundColor Green
    } else {
        Write-Host "   ⚠️ JAVA_HOME not set" -ForegroundColor Yellow
    }
} catch {
    Write-Host "   ❌ Java not found or not properly configured" -ForegroundColor Red
    Write-Host "   📥 Please install Java JDK 17 or later" -ForegroundColor Yellow
    return
}

Write-Host ""

# 2. Set JAVA_HOME if not set
if (-not $env:JAVA_HOME) {
    Write-Host "2️⃣ Setting JAVA_HOME..." -ForegroundColor Cyan
    
    # Common Java installation paths
    $javaPaths = @(
        "C:\Program Files\Microsoft\jdk-17.0.16.8-hotspot",
        "C:\Program Files\Java\jdk-17*",
        "C:\Program Files\Java\jdk-11*",
        "C:\Program Files\OpenJDK\*"
    )
    
    $javaPath = $null
    foreach ($path in $javaPaths) {
        $resolved = Get-ChildItem -Path $path -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($resolved -and (Test-Path "$($resolved.FullName)\bin\java.exe")) {
            $javaPath = $resolved.FullName
            break
        }
    }
    
    if ($javaPath) {
        [Environment]::SetEnvironmentVariable("JAVA_HOME", $javaPath, "User")
        $env:JAVA_HOME = $javaPath
        Write-Host "   ✅ JAVA_HOME set to: $javaPath" -ForegroundColor Green
    } else {
        Write-Host "   ❌ Could not find Java installation" -ForegroundColor Red
        return
    }
} else {
    Write-Host "2️⃣ JAVA_HOME already configured ✅" -ForegroundColor Green
}

Write-Host ""

# 3. Update gradle.properties
Write-Host "3️⃣ Updating gradle.properties..." -ForegroundColor Cyan

$gradlePropsContent = @"
# Project-wide Gradle settings.
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.enableJetifier=true

# JDK Configuration
org.gradle.java.home=$($env:JAVA_HOME)
"@

Set-Content -Path "gradle.properties" -Value $gradlePropsContent
Write-Host "   ✅ gradle.properties updated with JDK path" -ForegroundColor Green

Write-Host ""

# 4. Create local.properties
Write-Host "4️⃣ Creating/updating local.properties..." -ForegroundColor Cyan

# Find Android SDK
$androidSdkPaths = @(
    "$env:LOCALAPPDATA\Android\Sdk",
    "$env:USERPROFILE\AppData\Local\Android\Sdk",
    "C:\Android\Sdk"
)

$androidSdk = $null
foreach ($path in $androidSdkPaths) {
    if (Test-Path $path) {
        $androidSdk = $path
        break
    }
}

$localPropsContent = @"
# This file was automatically generated.
# Do not modify this file -- YOUR CHANGES WILL BE ERASED!

# Location of the SDK. This is only used by Gradle.
sdk.dir=$($androidSdk -replace '\\', '\\')

# JDK Location
org.gradle.java.home=$($env:JAVA_HOME -replace '\\', '\\')
"@

Set-Content -Path "local.properties" -Value $localPropsContent
Write-Host "   ✅ local.properties updated" -ForegroundColor Green

if (-not $androidSdk) {
    Write-Host "   ⚠️ Android SDK not found. Please install Android Studio or set SDK path manually" -ForegroundColor Yellow
}

Write-Host ""

# 5. Clean Gradle cache
Write-Host "5️⃣ Cleaning Gradle cache..." -ForegroundColor Cyan

if (Test-Path ".gradle") {
    Remove-Item -Path ".gradle" -Recurse -Force
    Write-Host "   ✅ Removed .gradle cache" -ForegroundColor Green
}

if (Test-Path "build") {
    Remove-Item -Path "build" -Recurse -Force  
    Write-Host "   ✅ Removed build directory" -ForegroundColor Green
}

if (Test-Path "app\build") {
    Remove-Item -Path "app\build" -Recurse -Force
    Write-Host "   ✅ Removed app build directory" -ForegroundColor Green
}

Write-Host ""

# 6. Test Gradle
Write-Host "6️⃣ Testing Gradle configuration..." -ForegroundColor Cyan

try {
    $gradleOutput = .\gradlew.bat --version 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "   ✅ Gradle is working properly!" -ForegroundColor Green
        Write-Host "$gradleOutput" -ForegroundColor Gray
    } else {
        Write-Host "   ❌ Gradle test failed" -ForegroundColor Red
        Write-Host "$gradleOutput" -ForegroundColor Red
    }
} catch {
    Write-Host "   ❌ Error running Gradle: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "🎉 Gradle JDK Configuration Fix Complete!" -ForegroundColor Green
Write-Host ""
Write-Host "📋 Next Steps:" -ForegroundColor Cyan
Write-Host "   1. Open Android Studio" -ForegroundColor White
Write-Host "   2. File -> Sync Project with Gradle Files" -ForegroundColor White
Write-Host "   3. Or run: .\gradlew.bat assembleDebug" -ForegroundColor White
Write-Host ""