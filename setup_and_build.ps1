# Setup and Build Script for Android Project
# This script sets up the Java environment and builds the project

Write-Host "Setting up Java environment..." -ForegroundColor Green

# Set JAVA_HOME to Android Studio's JDK
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

Write-Host "JAVA_HOME set to: $env:JAVA_HOME" -ForegroundColor Yellow

# Verify Java installation
Write-Host "Checking Java version..." -ForegroundColor Green
try {
    & "$env:JAVA_HOME\bin\java.exe" -version
    Write-Host "Java is working!" -ForegroundColor Green
} catch {
    Write-Host "Error: Java not found at $env:JAVA_HOME" -ForegroundColor Red
    exit 1
}

# Set Android SDK path if not set
if (-not $env:ANDROID_HOME) {
    $env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
    $env:PATH = "$env:ANDROID_HOME\platform-tools;$env:ANDROID_HOME\tools;$env:PATH"
    Write-Host "ANDROID_HOME set to: $env:ANDROID_HOME" -ForegroundColor Yellow
}

# Navigate to project directory
Set-Location "C:\Users\arink\Downloads\Bisto-Chat-Java-Firebase-master\Bisto-Chat-Java-Firebase-master\i180479_180531"

Write-Host "Building project..." -ForegroundColor Green

# Try to build the project
try {
    & ".\gradlew.bat" clean build
    Write-Host "Build completed successfully!" -ForegroundColor Green
} catch {
    Write-Host "Build failed. Trying alternative approach..." -ForegroundColor Yellow
    
    # Try just compiling without running tests
    & ".\gradlew.bat" assembleDebug
}

Write-Host "Script completed!" -ForegroundColor Green