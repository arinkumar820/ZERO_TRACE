@echo off
echo Bisto Chat WebSocket Server Startup Script
echo ==========================================

echo Checking Python installation...
python --version
if %errorlevel% neq 0 (
    echo Python is not installed or not in PATH!
    echo Please install Python 3.7+ from https://python.org
    pause
    exit /b 1
)

echo.
echo Installing WebSocket server dependencies...
pip install -r websocket_requirements.txt

if %errorlevel% neq 0 (
    echo Failed to install dependencies!
    echo Make sure you have pip installed and internet connection
    echo Also ensure MySQL is installed and running
    pause
    exit /b 1
)

echo.
echo =============================================
echo IMPORTANT: Make sure MySQL is running!
echo Database: message_database
echo User: root
echo Password: rudra@69420 (change in websocket_server.py)
echo =============================================
echo.

echo Starting Bisto Chat WebSocket Server...
echo Server will be available at: ws://localhost:8080
echo Press Ctrl+C to stop the server
echo.

python websocket_server.py

pause