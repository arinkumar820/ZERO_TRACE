# Fix malformed auth replacement patterns
Write-Host "Fixing malformed auth replacements..." -ForegroundColor Green

$filePath = "app/src/main/java/com/sameetasadullah/i180479_180531/screen5RVAdaptor.java"

if (Test-Path $filePath) {
    Write-Host "Fixing: $filePath" -ForegroundColor Yellow
    
    $content = Get-Content $filePath -Raw
    
    # Fix the malformed replacements
    $content = $content -replace 'm"TODO_USER_ID"', '"TODO_USER_ID"'
    
    Set-Content -Path $filePath -Value $content -Encoding UTF8
    Write-Host "Fixed malformed auth replacements!" -ForegroundColor Green
} else {
    Write-Host "File not found: $filePath" -ForegroundColor Red
}

Write-Host "Auth replacement fixes complete!" -ForegroundColor Green