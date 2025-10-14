# Remove BOM from all Java files
Write-Host "Removing BOM from Java files..." -ForegroundColor Green

$javaFiles = Get-ChildItem -Path "app/src/main/java" -Recurse -Filter "*.java"

foreach ($file in $javaFiles) {
    Write-Host "Checking: $($file.FullName)" -ForegroundColor Yellow
    
    # Read file as bytes
    $bytes = [System.IO.File]::ReadAllBytes($file.FullName)
    
    # Check if file has BOM (UTF-8 BOM is EF BB BF)
    if ($bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) {
        Write-Host "  Removing BOM from: $($file.Name)" -ForegroundColor Red
        
        # Remove BOM by skipping first 3 bytes
        $newBytes = $bytes[3..($bytes.Length-1)]
        [System.IO.File]::WriteAllBytes($file.FullName, $newBytes)
    }
}

Write-Host "BOM removal complete!" -ForegroundColor Green