Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "         STOPPING ALL ESHOPPINGZONE MICROSERVICES          " -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

$ports = @(8761, 8081, 8082, 8083, 8084, 8085, 8086, 8087, 8080)

foreach ($port in $ports) {
    try {
        $netstat = netstat -ano | Select-String ":$port\s" | Select-String "LISTENING"
        if ($netstat) {
            foreach ($line in $netstat) {
                $tokens = ($line.ToString().Trim() -split '\s+')
                $pidToKill = $tokens[-1]
                if ($pidToKill -match '^\d+$' -and $pidToKill -ne "0") {
                    Stop-Process -Id ([int]$pidToKill) -Force -ErrorAction SilentlyContinue
                    Write-Host " [STOPPED] Port $port (PID: $pidToKill)" -ForegroundColor Yellow
                }
            }
        }
    } catch {
        # ignore
    }
}

Write-Host "`nAll EShoppingZone microservices stopped successfully." -ForegroundColor Green
