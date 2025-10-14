@echo off
echo ========================================
echo   Bisto Chat - Debug Logs Monitor
echo ========================================
echo.
echo Monitoring app logs for:
echo - LoginActivity
echo - RegistrationActivity  
echo - UserProfile
echo - Firebase operations
echo.
echo Press Ctrl+C to stop monitoring
echo ========================================
echo.

adb logcat -s "LoginActivity:*" "RegistrationActivity:*" "UserProfile:*" "FirebaseAuth:*" "FirebaseDatabase:*" "BistoChatApp:*"