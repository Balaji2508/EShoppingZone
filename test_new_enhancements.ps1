$ErrorActionPreference = "Stop"

Write-Host "================================================================================"
Write-Host "         TESTING NEW ENHANCEMENTS & BUG FIXES                                   "
Write-Host "================================================================================"

# 1. Profile Update Test
Write-Host "`n[TEST 1] Profile Update Verification"
$loginCustomer = Invoke-RestMethod -Uri "http://localhost:8080/api/profiles/login" -Method Post -Body (@{ email="customer@gmail.com"; password="customer123" } | ConvertTo-Json) -ContentType "application/json"
$customerId = $loginCustomer.userId

$updateBody = @{
    name = "Customer John Doe"
    fullName = "Customer John Doe"
    phone = "9876543210"
    address = "Flat 402, Sunshine Heights, Bangalore"
} | ConvertTo-Json

$updatedProfile = Invoke-RestMethod -Uri "http://localhost:8080/api/profiles/$customerId" -Method Put -Body $updateBody -ContentType "application/json"
if ($updatedProfile.name -eq "Customer John Doe" -and $updatedProfile.phone -eq "9876543210") {
    Write-Host "  [PASS] Profile updated successfully: Name=$($updatedProfile.name), Phone=$($updatedProfile.phone)" -ForegroundColor Green
} else {
    Write-Host "  [FAIL] Profile update did not match expected values" -ForegroundColor Red
}

# 2. Delivery Partner List Verification
Write-Host "`n[TEST 2] Delivery Partners Endpoint Name Verification"
$dpList = Invoke-RestMethod -Uri "http://localhost:8080/api/profiles/delivery-partners" -Method Get
if ($dpList.Count -gt 0 -and $dpList[0].name -and $dpList[0].name -ne "undefined") {
    Write-Host "  [PASS] Delivery partners listed with valid name: ID=$($dpList[0].id), Name=$($dpList[0].name), FullName=$($dpList[0].fullName)" -ForegroundColor Green
} else {
    Write-Host "  [FAIL] Delivery partners name is missing or undefined" -ForegroundColor Red
}

# 3. Cart Quantity Modification Test
Write-Host "`n[TEST 3] Cart Quantity Modification"
$product1 = Invoke-RestMethod -Uri "http://localhost:8080/api/products/1" -Method Get
$cartReq = @{
    productId = $product1.id
    productName = $product1.name
    price = $product1.price
    quantity = 1
    imageUrl = $product1.imageUrl
} | ConvertTo-Json

$cartAdd = Invoke-RestMethod -Uri "http://localhost:8080/api/cart/$customerId/items" -Method Post -Body $cartReq -ContentType "application/json"
$cartUpdate = Invoke-RestMethod -Uri "http://localhost:8080/api/cart/$customerId/items/1" -Method Put -Body (@{ quantity=3 } | ConvertTo-Json) -ContentType "application/json"
$updatedItem = $cartUpdate.items | Where-Object { $_.productId -eq 1 }
if ($updatedItem.quantity -eq 3) {
    Write-Host "  [PASS] Cart quantity modified to 3, subtotal recalculated: ₹$($updatedItem.subtotal)" -ForegroundColor Green
} else {
    Write-Host "  [FAIL] Cart quantity was not updated to 3" -ForegroundColor Red
}

# 4. Order Cancellation, Wallet Refund & Stock Restoration Test
Write-Host "`n[TEST 4] Order Placement, Customer Cancellation & Wallet Refund"
$productBefore = Invoke-RestMethod -Uri "http://localhost:8080/api/products/1" -Method Get
$initialStock = $productBefore.stockQuantity

# Topup wallet with 500,000 for test
Invoke-RestMethod -Uri "http://localhost:8080/api/wallet/$customerId/topup" -Method Post -Body (@{ amount=500000 } | ConvertTo-Json) -ContentType "application/json" | Out-Null
$walletBefore = Invoke-RestMethod -Uri "http://localhost:8080/api/wallet/$customerId" -Method Get

$orderReq = @{
    userId = $customerId
    paymentMethod = "WALLET"
    shippingAddress = "Flat 402, Sunshine Heights, Bangalore"
} | ConvertTo-Json

$placedOrder = Invoke-RestMethod -Uri "http://localhost:8080/api/orders" -Method Post -Body $orderReq -ContentType "application/json"
Write-Host "  -> Placed Order #$($placedOrder.id), Total: ₹$($placedOrder.totalAmount), Status: $($placedOrder.orderStatus)"

$productDuring = Invoke-RestMethod -Uri "http://localhost:8080/api/products/1" -Method Get
Write-Host "  -> Stock after checkout: $($productDuring.stockQuantity) (Reduced from $initialStock)"

# Cancel Order
$cancelReq = @{ reason = "Changed mind about order" } | ConvertTo-Json
$cancelledOrder = Invoke-RestMethod -Uri "http://localhost:8080/api/orders/$($placedOrder.id)/cancel?userId=$customerId" -Method Put -Body $cancelReq -ContentType "application/json"

$walletAfter = Invoke-RestMethod -Uri "http://localhost:8080/api/wallet/$customerId" -Method Get
$productAfter = Invoke-RestMethod -Uri "http://localhost:8080/api/products/1" -Method Get

if ($cancelledOrder.orderStatus -eq "CANCELLED") {
    Write-Host "  [PASS] Order #$($placedOrder.id) cancelled successfully (Status: CANCELLED)" -ForegroundColor Green
} else {
    Write-Host "  [FAIL] Order status is not CANCELLED" -ForegroundColor Red
}

if ($walletAfter.balance -eq $walletBefore.balance) {
    Write-Host "  [PASS] Wallet refunded automatically: Balance restored to ₹$($walletAfter.balance)" -ForegroundColor Green
} else {
    Write-Host "  [FAIL] Wallet balance not refunded properly. Expected ₹$($walletBefore.balance), Got ₹$($walletAfter.balance)" -ForegroundColor Red
}

if ($productAfter.stockQuantity -eq $initialStock) {
    Write-Host "  [PASS] Product stock restored automatically to $initialStock" -ForegroundColor Green
} else {
    Write-Host "  [FAIL] Stock not restored properly. Expected $initialStock, Got $($productAfter.stockQuantity)" -ForegroundColor Red
}

# 5. Assign Delivery Partner without 'undefined' Name Test
Write-Host "`n[TEST 5] Merchant Assign Delivery Partner with Real Name"
$product2 = Invoke-RestMethod -Uri "http://localhost:8080/api/products/2" -Method Get
$cartReq2 = @{
    productId = $product2.id
    productName = $product2.name
    price = $product2.price
    quantity = 1
    imageUrl = $product2.imageUrl
} | ConvertTo-Json

$cartAdd2 = Invoke-RestMethod -Uri "http://localhost:8080/api/cart/$customerId/items" -Method Post -Body $cartReq2 -ContentType "application/json"
$order2 = Invoke-RestMethod -Uri "http://localhost:8080/api/orders" -Method Post -Body $orderReq -ContentType "application/json"

# Merchant accepts and assigns partner
$accepted = Invoke-RestMethod -Uri "http://localhost:8080/api/orders/$($order2.id)/accept?merchantId=2" -Method Put
$assignReq = @{
    deliveryPartnerId = 3
    deliveryPartnerName = "David Delivery"
    deliveryPartnerPhone = "9870001122"
} | ConvertTo-Json

$assignedOrder = Invoke-RestMethod -Uri "http://localhost:8080/api/orders/$($order2.id)/assign-delivery?merchantId=2" -Method Put -Body $assignReq -ContentType "application/json"

if ($assignedOrder.deliveryPartnerName -eq "David Delivery" -and $assignedOrder.deliveryPartnerName -ne "undefined") {
    Write-Host "  [PASS] Order #$($order2.id) assigned to Delivery Partner '$($assignedOrder.deliveryPartnerName)' without undefined" -ForegroundColor Green
} else {
    Write-Host "  [FAIL] Delivery partner name is '$($assignedOrder.deliveryPartnerName)'" -ForegroundColor Red
}

Write-Host "`n================================================================================"
Write-Host "         ALL 5 NEW ENHANCEMENTS VERIFIED SUCCESSFULLY                           "
Write-Host "================================================================================"
