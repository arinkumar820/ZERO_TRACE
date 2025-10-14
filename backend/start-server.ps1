# Secure Messaging Backend Startup Script
Write-Host "🚀 Starting Secure Messaging Backend Server..." -ForegroundColor Green
Write-Host ""

# Get computer's IP address
$ip = (Get-NetIPAddress -AddressFamily IPv4 -InterfaceAlias "Wi-Fi*" | Where-Object {$_.PrefixOrigin -eq "Dhcp"}).IPAddress
if (-not $ip) { 
    $ip = (Get-NetIPAddress -AddressFamily IPv4 | Where-Object {$_.IPAddress -ne "127.0.0.1" -and $_.PrefixOrigin -eq "Dhcp"}).IPAddress 
}

Write-Host "📱 Your computer's IP address: $ip" -ForegroundColor Cyan
Write-Host "🌐 Other devices can access at: http://${ip}:3000" -ForegroundColor Yellow
Write-Host ""
Write-Host "⚠️  Make sure Windows Firewall allows Node.js through port 3000" -ForegroundColor Red
Write-Host ""
Write-Host "📋 API Endpoints accessible from other devices:"
Write-Host "   • Health Check: http://${ip}:3000/health"
Write-Host "   • Register User: POST http://${ip}:3000/api/users/register"
Write-Host "   • Get Chat Users: GET http://${ip}:3000/api/users/{id}/permissible-chats"
Write-Host ""

# Check if firewall rule exists, if not, suggest creating it
$firewallRule = Get-NetFirewallRule -DisplayName "*Node*" -ErrorAction SilentlyContinue
if (-not $firewallRule) {
    Write-Host "🔥 Firewall Rule Suggestion:" -ForegroundColor Magenta
    Write-Host "   Run this command as Administrator to allow Node.js through firewall:"
    Write-Host '   New-NetFirewallRule -DisplayName "Node.js Server" -Direction Inbound -Protocol TCP -LocalPort 3000 -Action Allow'
    Write-Host ""
}

Write-Host "Starting server..." -ForegroundColor Green
node server.js