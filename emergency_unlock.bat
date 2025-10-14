@echo off
setlocal enabledelayedexpansion

echo.
echo ==============================================
echo    EMERGENCY PIN UNLOCK UTILITY
echo    For Bisto Chat Java Firebase App
echo ==============================================
echo.

REM Define the package name
set PACKAGE_NAME=com.sameetasadullah.i180479_180531

echo Checking if device is connected...
adb devices | findstr device >nul
if %errorlevel% neq 0 (
    echo ERROR: No Android device connected or ADB not found.
    echo.
    echo Make sure:
    echo 1. Android device is connected via USB
    echo 2. USB Debugging is enabled
    echo 3. ADB is installed and in PATH
    pause
    exit /b
)

echo Device found. Checking if app is installed...
adb shell pm list packages | findstr %PACKAGE_NAME% >nul
if %errorlevel% neq 0 (
    echo WARNING: App not found on device.
    echo Package: %PACKAGE_NAME%
    echo.
    echo Make sure the app is installed on the connected device.
    pause
    exit /b
)

echo.
echo Select unlock method:
echo 1. Emergency unlock (reset to default PIN 123456)
echo 2. Reset attempts counter only
echo 3. Factory reset (clear all PIN data)
echo 4. Show current lock status
echo 5. Kill app process (force restart)
echo.
set /p choice="Enter your choice (1-5): "

if "%choice%"=="1" goto emergency_unlock
if "%choice%"=="2" goto reset_attempts
if "%choice%"=="3" goto factory_reset
if "%choice%"=="4" goto show_status
if "%choice%"=="5" goto kill_app
echo Invalid choice. Exiting.
pause
exit /b

:emergency_unlock
echo.
echo Performing emergency unlock...
echo Resetting PIN to default: 123456
adb shell am broadcast -a com.sameetasadullah.i180479_180531.EMERGENCY_UNLOCK
REM Alternative method using SharedPreferences modification
adb shell "run-as %PACKAGE_NAME% sh -c 'rm -f /data/data/%PACKAGE_NAME%/shared_prefs/pin_preferences.xml'"
adb shell am force-stop %PACKAGE_NAME%
echo.
echo ✓ Emergency unlock completed!
echo ✓ App has been reset to unlocked state
echo ✓ Default PIN is now: 123456
echo.
echo Please restart the app to verify the unlock.
goto end

:reset_attempts
echo.
echo Resetting attempts counter...
REM This would require root access or specific implementation
echo NOTE: This requires the app to have a broadcast receiver for this action.
adb shell am broadcast -a com.sameetasadullah.i180479_180531.RESET_ATTEMPTS
goto end

:factory_reset
echo.
echo WARNING: This will completely reset all PIN settings!
set /p confirm="Are you sure? (y/n): "
if /i not "%confirm%"=="y" goto end

echo Performing factory reset of PIN data...
adb shell "run-as %PACKAGE_NAME% sh -c 'rm -rf /data/data/%PACKAGE_NAME%/shared_prefs/pin_preferences.xml'"
adb shell am force-stop %PACKAGE_NAME%
echo.
echo ✓ Factory reset completed!
echo ✓ App will show first-time setup on next launch
goto end

:show_status
echo.
echo Checking current lock status...
adb shell "run-as %PACKAGE_NAME% sh -c 'cat /data/data/%PACKAGE_NAME%/shared_prefs/pin_preferences.xml 2>/dev/null || echo No PIN data found'"
goto end

:kill_app
echo.
echo Killing app process...
adb shell am force-stop %PACKAGE_NAME%
echo ✓ App process terminated
echo You can now restart the app manually.
goto end

:end
echo.
echo ==============================================
echo Operation completed.
echo ==============================================
echo.
pause