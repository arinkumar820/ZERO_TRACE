# Fix null pointer exceptions from commented findViewById calls
Write-Host "Fixing null pointer exceptions..." -ForegroundColor Green

# Fix screen3.java
$screen3Path = "app/src/main/java/com/sameetasadullah/i180479_180531/screen3.java"
if (Test-Path $screen3Path) {
    Write-Host "Fixing: screen3.java" -ForegroundColor Yellow
    
    $content = Get-Content $screen3Path -Raw
    
    # Replace the problematic null reference code with null checks
    $content = $content -replace 'registerButton\.setOnClickListener\(new View\.OnClickListener\(\) \{([^}]+\})+\}\);', @'
// Temporary: Check if button exists before setting listener to prevent crashes
        if (registerButton != null) {
            registerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO: Replace with Supabase authentication
                    if (email != null && password != null && confirmPassword != null) {
                        String emailText = email.getText().toString().trim();
                        String passwordText = password.getText().toString().trim();
                        String confirmPasswordText = confirmPassword.getText().toString().trim();
                        
                        if (emailText.isEmpty() || passwordText.isEmpty() || confirmPasswordText.isEmpty()) {
                            Toast.makeText(screen3.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        if (!passwordText.equals(confirmPasswordText)) {
                            Toast.makeText(screen3.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    
                    // TODO: Implement Supabase sign up
                    Toast.makeText(screen3.this, "Registration functionality to be implemented with Supabase", Toast.LENGTH_SHORT).show();
                    
                    // For now, redirect to input credentials
                    Intent intent = new Intent(screen3.this, inputCredentials.class);
                    startActivity(intent);
                }
            });
        } else {
            // Show message that UI is being migrated
            Toast.makeText(this, "Screen under migration - missing layout resources", Toast.LENGTH_LONG).show();
        }
'@
    
    Set-Content -Path $screen3Path -Value $content -Encoding UTF8
    Write-Host "Fixed screen3.java" -ForegroundColor Green
}

# Fix inputCredentials.java
$inputCredPath = "app/src/main/java/com/sameetasadullah/i180479_180531/inputCredentials.java"
if (Test-Path $inputCredPath) {
    Write-Host "Fixing: inputCredentials.java" -ForegroundColor Yellow
    
    $content = Get-Content $inputCredPath -Raw
    
    # Add null check for nextButton
    $content = $content -replace 'nextButton\.setOnClickListener', 'if (nextButton != null) nextButton.setOnClickListener'
    
    Set-Content -Path $inputCredPath -Value $content -Encoding UTF8
    Write-Host "Fixed inputCredentials.java" -ForegroundColor Green
}

# Fix screen5.java
$screen5Path = "app/src/main/java/com/sameetasadullah/i180479_180531/screen5.java"
if (Test-Path $screen5Path) {
    Write-Host "Fixing: screen5.java" -ForegroundColor Yellow
    
    $content = Get-Content $screen5Path -Raw
    
    # Add null checks for buttons
    $content = $content -replace '(\w+)\.setOnClickListener', 'if ($1 != null) $1.setOnClickListener'
    
    Set-Content -Path $screen5Path -Value $content -Encoding UTF8
    Write-Host "Fixed screen5.java" -ForegroundColor Green
}

Write-Host "Null pointer fixes complete!" -ForegroundColor Green