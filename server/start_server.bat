@echo off
echo Bisto Chat Database Server Startup Script
echo =========================================

echo Checking Python installation...
python --version
if %errorlevel% neq 0 (
    echo Python is not installed or not in PATH!
    echo Please install Python 3.7+ from https://python.org
    pause
    exit /b 1
)

echo.
echo Installing Python dependencies...
pip install -r requirements.txt

if %errorlevel% neq 0 (
    echo Failed to install dependencies!
    echo Make sure you have pip installed and internet connection
    pause
    exit /b 1
)

echo.
echo Starting Bisto Chat Database Server...
echo Server will be available at: http://localhost:8080
echo Press Ctrl+C to stop the server
echo.

python server.py

pause