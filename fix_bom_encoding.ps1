# Script to fix BOM encoding issues in Java files
Write-Host "Fixing BOM encoding issues in Java files..." -ForegroundColor Green

$javaFiles = Get-ChildItem -Path "app/src" -Recurse -Filter "*.java"

foreach ($file in $javaFiles) {
    Write-Host "Fixing: $($file.Name)" -ForegroundColor Yellow
    
    # Read content as bytes to detect BOM
    $bytes = [System.IO.File]::ReadAllBytes($file.FullName)
    
    # Check for UTF-8 BOM (EF BB BF)
    if ($bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) {
        Write-Host "  Removing BOM from $($file.Name)" -ForegroundColor Red
        # Remove BOM and save as UTF-8 without BOM
        $content = [System.IO.File]::ReadAllText($file.FullName, [System.Text.Encoding]::UTF8)
        [System.IO.File]::WriteAllText($file.FullName, $content, (New-Object System.Text.UTF8Encoding $false))
    } else {
        # Ensure file is saved as UTF-8 without BOM
        $content = [System.IO.File]::ReadAllText($file.FullName)
        [System.IO.File]::WriteAllText($file.FullName, $content, (New-Object System.Text.UTF8Encoding $false))
    }
}

Write-Host "BOM encoding issues fixed!" -ForegroundColor Green