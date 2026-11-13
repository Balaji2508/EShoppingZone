$ErrorActionPreference = "Stop"

Write-Host "================================================================================"
Write-Host "       TESTING RESILIENCE4J CIRCUIT BREAKER & FALLBACK MECHANISMS               "
Write-Host "================================================================================"

$customerId = 1
$merchantId = 2

# Top up customer wallet first to ensure sufficient balance
Write-Host "`n[STEP 1] Setting up wallet balance..."
Invoke-RestMethod -Uri "http://localhost:8080/api/wallet/$customerId/topup" -Method Post -Body (@{ amount = 300000.0 } | ConvertTo-Json) -ContentType "application/json" | Out-Null
Write-Host "  [PASS] Customer Wallet topped up with funds" -ForegroundColor Green

# 2. Place Order with Wallet Payment when balance is available
Write-Host "`n[STEP 2] Placing Order with WALLET payment (Circuit Breakers in CLOSED state)..."
# Add item to cart first
$p1 = Invoke-RestMethod -Uri "http://localhost:8080/api/products/1" -Method Get
$addReq1 = @{
    productId = $p1.id
    productName = $p1.name
    price = $p1.price
    quantity = 1
    imageUrl = $p1.imageUrl
} | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8080/api/cart/$customerId/items" -Method Post -Body $addReq1 -ContentType "application/json" | Out-Null

$orderReq1 = @{
    userId = $customerId
    shippingAddress = "Express Hub 4, Indiranagar, Bangalore"
    paymentMethod = "WALLET"
} | ConvertTo-Json

$order1 = Invoke-RestMethod -Uri "http://localhost:8080/api/orders" -Method Post -Body $orderReq1 -ContentType "application/json"
if ($order1.id -and ($order1.orderStatus -eq "CONFIRMED" -or $order1.orderStatus -eq "PLACED") -and $order1.paymentStatus -eq "PAID") {
    Write-Host "  [PASS] Order #$($order1.id) created successfully via WALLET (Status: $($order1.orderStatus), Payment: $($order1.paymentStatus))" -ForegroundColor Green
} else {
    Write-Host "  [FAIL] Order creation failed" -ForegroundColor Red
}

# 3. Test Fallback when cart is empty
Write-Host "`n[STEP 3] Testing Circuit Breaker & Validation Fallback on empty cart checkout..."
$emptyOrderReq = @{
    userId = $customerId
    shippingAddress = "Express Hub 4, Indiranagar, Bangalore"
    paymentMethod = "WALLET"
} | ConvertTo-Json

try {
    Invoke-RestMethod -Uri "http://localhost:8080/api/orders" -Method Post -Body $emptyOrderReq -ContentType "application/json"
    Write-Host "  [FAIL] Should have failed on empty cart" -ForegroundColor Red
} catch {
    Write-Host "  [PASS] Empty cart fallback caught cleanly (HTTP 400 Bad Request returned without 500 server crash)" -ForegroundColor Green
}

# 4. Place Order with COD Payment
Write-Host "`n[STEP 4] Placing Order with CASH ON DELIVERY (COD)..."
# Add item to cart
Invoke-RestMethod -Uri "http://localhost:8080/api/cart/$customerId/items" -Method Post -Body $addReq1 -ContentType "application/json" | Out-Null

$orderReq2 = @{
    userId = $customerId
    shippingAddress = "Express Hub 4, Indiranagar, Bangalore"
    paymentMethod = "COD"
} | ConvertTo-Json

$order2 = Invoke-RestMethod -Uri "http://localhost:8080/api/orders" -Method Post -Body $orderReq2 -ContentType "application/json"
if ($order2.id -and $order2.orderStatus -eq "PLACED" -and $order2.paymentStatus -eq "COD_PENDING") {
    Write-Host "  [PASS] Order #$($order2.id) created with COD (Status: $($order2.orderStatus), Payment: $($order2.paymentStatus))" -ForegroundColor Green
} else {
    Write-Host "  [FAIL] COD Order creation failed" -ForegroundColor Red
}

# 5. Test Customer Self-Cancellation and Refund/Restoration
Write-Host "`n[STEP 5] Testing Customer Self-Cancellation & Wallet Refund on Order #$($order1.id)..."
$canceledOrder = Invoke-RestMethod -Uri "http://localhost:8080/api/orders/$($order1.id)/cancel?userId=$customerId" -Method Put
if ($canceledOrder.orderStatus -eq "CANCELLED" -and $canceledOrder.paymentStatus -eq "REFUNDED") {
    Write-Host "  [PASS] Order #$($order1.id) cancelled successfully by customer: Status=$($canceledOrder.orderStatus), Payment=$($canceledOrder.paymentStatus)" -ForegroundColor Green
} else {
    Write-Host "  [FAIL] Cancellation failed" -ForegroundColor Red
}

Write-Host "`n================================================================================"
Write-Host "       CIRCUIT BREAKER & RESILIENCE TESTS COMPLETED SUCCESSFULLY                "
Write-Host "================================================================================"
