$baseDir = "D:\E-ShoppingZone"

Write-Host "Starting Eureka Server (8761)..."
Start-Process -FilePath "java" -ArgumentList "-jar `"$baseDir\eureka-server\target\eureka-server-1.0.0.jar`"" -WindowStyle Hidden
Start-Sleep -Seconds 8

$services = @(
    @{ Name = "Profile Service"; Jar = "profile-service\target\profile-service-1.0.0.jar"; Port = 8081 },
    @{ Name = "Product Service"; Jar = "product-service\target\product-service-1.0.0.jar"; Port = 8082 },
    @{ Name = "Cart Service"; Jar = "cart-service\target\cart-service-1.0.0.jar"; Port = 8083 },
    @{ Name = "Wallet Service"; Jar = "wallet-service\target\wallet-service-1.0.0.jar"; Port = 8085 },
    @{ Name = "Payment Service"; Jar = "payment-service\target\payment-service-1.0.0.jar"; Port = 8087 },
    @{ Name = "Order Service"; Jar = "order-service\target\order-service-1.0.0.jar"; Port = 8084 },
    @{ Name = "Notification Service"; Jar = "notification-service\target\notification-service-1.0.0.jar"; Port = 8086 },
    @{ Name = "API Gateway"; Jar = "api-gateway\target\api-gateway-1.0.0.jar"; Port = 8080 }
)

foreach ($s in $services) {
    Write-Host "Starting $($s.Name) (Port $($s.Port))..."
    Start-Process -FilePath "java" -ArgumentList "-jar `"$baseDir\$($s.Jar)`"" -WindowStyle Hidden
    Start-Sleep -Seconds 2
}

Write-Host "`nWaiting 15 seconds for all microservices to complete initialization..."
Start-Sleep -Seconds 15
Write-Host "All services started."
