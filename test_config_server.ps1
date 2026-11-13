$ErrorActionPreference = "Stop"

Write-Host "================================================================================"
Write-Host "       TESTING CENTRALIZED CONFIGURATION SERVER (Port 8888)                     "
Write-Host "================================================================================"

$services = @("order-service", "product-service", "cart-service", "profile-service", "payment-service", "wallet-service", "notification-service", "api-gateway")

foreach ($svc in $services) {
    try {
        $res = Invoke-RestMethod -Uri "http://localhost:8888/$svc/default" -Method Get
        if ($res.propertySources -and $res.propertySources.Count -gt 0) {
            Write-Host "  [PASS] Centralized config resolved for '$svc' (Sources: $($res.propertySources.Count))" -ForegroundColor Green
        } else {
            Write-Host "  [FAIL] Centralized config empty for '$svc'" -ForegroundColor Red
        }
    } catch {
        Write-Host "  [FAIL] Failed to retrieve config for '$svc': $_" -ForegroundColor Red
    }
}

Write-Host "`n================================================================================"
Write-Host "       CONFIG SERVER VERIFICATION COMPLETED                                     "
Write-Host "================================================================================"
