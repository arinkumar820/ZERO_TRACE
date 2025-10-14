# Script to remove Firebase imports and references from Java files
Write-Host "Removing Firebase imports and references from Java files..." -ForegroundColor Green

$javaFiles = Get-ChildItem -Path "app/src" -Recurse -Filter "*.java"

foreach ($file in $javaFiles) {
    Write-Host "Processing: $($file.Name)" -ForegroundColor Yellow
    
    $content = Get-Content $file.FullName
    $newContent = @()
    $inFirebaseBlock = $false
    
    foreach ($line in $content) {
        # Comment out Firebase imports
        if ($line -match "import com\.google\.firebase\.") {
            $newContent += "// $line // Removed for Supabase migration"
        }
        elseif ($line -match "import com\.google\.android\.gms\.tasks\.") {
            $newContent += "// $line // Removed for Supabase migration"
        }
        # Comment out Firebase variable declarations
        elseif ($line -match "^\s*(FirebaseAuth|FirebaseDatabase|FirebaseStorage|DatabaseReference|StorageReference)\s+") {
            $newContent += "    // $line.Trim() // TODO: Replace with Supabase equivalent"
        }
        # Comment out Firebase method calls (basic patterns)
        elseif ($line -match "FirebaseDatabase\.getInstance\(\)") {
            $newContent += "        // $line.Trim() // TODO: Replace with Supabase database call"
        }
        elseif ($line -match "FirebaseAuth\.getInstance\(\)") {
            $newContent += "        // $line.Trim() // TODO: Replace with Supabase auth call"
        }
        elseif ($line -match "FirebaseStorage\.getInstance\(\)") {
            $newContent += "        // $line.Trim() // TODO: Replace with Supabase storage call"
        }
        # Comment out Firebase event listeners
        elseif ($line -match "addChildEventListener|ChildEventListener") {
            $newContent += "        // $line.Trim() // TODO: Replace with Supabase real-time subscription"
        }
        # Comment out Task-related Firebase operations
        elseif ($line -match "(OnCompleteListener|OnSuccessListener|OnFailureListener|Task<)") {
            $newContent += "        // $line.Trim() // TODO: Replace with Supabase callback"
        }
        else {
            $newContent += $line
        }
    }
    
    # Write the modified content back to the file
    $newContent | Set-Content $file.FullName -Encoding UTF8
}

Write-Host "Firebase references removed from Java files!" -ForegroundColor Green