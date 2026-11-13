$randomSuffix = Get-Random
$email = "testuser_$randomSuffix@example.com"

Write-Host "1. Testing Registration..."
$regBody = @{
    name = "Test User"
    email = $email
    password = "password123"
    phone = "9876543210"
    role = "CUSTOMER"
    address = "123 Indiranagar, Bangalore"
} | ConvertTo-Json

$regRes = Invoke-RestMethod -Uri "http://localhost:8080/api/profiles/register" -Method Post -Body $regBody -ContentType "application/json"
Write-Host " -> Registration Result: User ID = $($regRes.userId), Name = $($regRes.name)"

Write-Host "`n2. Testing Change Password..."
$pwBody = @{
    currentPassword = "password123"
    newPassword = "newSecurePassword456"
} | ConvertTo-Json

$pwRes = Invoke-RestMethod -Uri "http://localhost:8080/api/profiles/$($regRes.userId)/change-password" -Method Put -Body $pwBody -ContentType "application/json"
Write-Host " -> Change Password Result: $($pwRes.message)"

Write-Host "`n3. Testing Login with New Password..."
$loginBody = @{
    email = $email
    password = "newSecurePassword456"
} | ConvertTo-Json

$loginRes = Invoke-RestMethod -Uri "http://localhost:8080/api/profiles/login" -Method Post -Body $loginBody -ContentType "application/json"
Write-Host " -> Login Success: User = $($loginRes.name), Role = $($loginRes.role), Token Valid = $($null -ne $loginRes.token)"
