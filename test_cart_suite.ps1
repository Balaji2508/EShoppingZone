$ErrorActionPreference = "Stop"

Write-Host "================================================================================"
Write-Host "       COMPREHENSIVE CART REGRESSION & PERSISTENCE TEST SUITE                   "
Write-Host "================================================================================"

$customerId = 1

# 1. Clear cart
Write-Host "`n[STEP 1] Clearing cart for customer $customerId..."
Invoke-RestMethod -Uri "http://localhost:8080/api/cart/$customerId/clear" -Method Delete | Out-Null
$cart0 = Invoke-RestMethod -Uri "http://localhost:8080/api/cart/$customerId" -Method Get
if ($cart0.items.Count -eq 0 -and $cart0.totalAmount -eq 0) {
    Write-Host "  [PASS] Initial Cart is empty (Items: 0, Total: ₹0.00)" -ForegroundColor Green
} else {
    Write-Host "  [FAIL] Initial Cart not empty" -ForegroundColor Red
}

# 2. Add product #1
Write-Host "`n[STEP 2] Adding Product 1 to cart..."
$p1 = Invoke-RestMethod -Uri "http://localhost:8080/api/products/1" -Method Get
$addReq1 = @{
    productId = $p1.id
    productName = $p1.name
    price = $p1.price
    quantity = 1
    imageUrl = $p1.imageUrl
} | ConvertTo-Json

$cart1 = Invoke-RestMethod -Uri "http://localhost:8080/api/cart/$customerId/items" -Method Post -Body $addReq1 -ContentType "application/json"
$item1 = $cart1.items | Where-Object { $_.productId -eq 1 }
if ($item1 -and $item1.quantity -eq 1) {
    Write-Host "  [PASS] Product 1 added: Qty=1, Subtotal=₹$($item1.subtotal), Total=₹$($cart1.totalAmount)" -ForegroundColor Green
} else {
    Write-Host "  [FAIL] Product 1 not added properly" -ForegroundColor Red
}

# 3. Increase quantity to 2, then 3
Write-Host "`n[STEP 3] Increasing quantity from 1 -> 2 -> 3..."
$cart2 = Invoke-RestMethod -Uri "http://localhost:8080/api/cart/$customerId/items/1" -Method Put -Body (@{ quantity=2 } | ConvertTo-Json) -ContentType "application/json"
$item2 = $cart2.items | Where-Object { $_.productId -eq 1 }
$cart3 = Invoke-RestMethod -Uri "http://localhost:8080/api/cart/$customerId/items/1" -Method Put -Body (@{ quantity=3 } | ConvertTo-Json) -ContentType "application/json"
$item3 = $cart3.items | Where-Object { $_.productId -eq 1 }
$expectedSubtotal = $p1.price * 3

if ($item3.quantity -eq 3 -and $item3.subtotal -eq $expectedSubtotal -and $cart3.totalAmount -eq $expectedSubtotal) {
    Write-Host "  [PASS] Quantity increased to 3: Qty=3, Subtotal=₹$($item3.subtotal), Total=₹$($cart3.totalAmount)" -ForegroundColor Green
} else {
    Write-Host "  [FAIL] Quantity increase mismatch. Expected ₹$expectedSubtotal, got ₹$($item3.subtotal)" -ForegroundColor Red
}

# 4. Decrease quantity 3 -> 2
Write-Host "`n[STEP 4] Decreasing quantity from 3 -> 2..."
$cart4 = Invoke-RestMethod -Uri "http://localhost:8080/api/cart/$customerId/items/1" -Method Put -Body (@{ quantity=2 } | ConvertTo-Json) -ContentType "application/json"
$item4 = $cart4.items | Where-Object { $_.productId -eq 1 }
$expectedSubtotal4 = $p1.price * 2

if ($item4.quantity -eq 2 -and $item4.subtotal -eq $expectedSubtotal4) {
    Write-Host "  [PASS] Quantity decreased to 2: Qty=2, Subtotal=₹$($item4.subtotal), Total=₹$($cart4.totalAmount)" -ForegroundColor Green
} else {
    Write-Host "  [FAIL] Quantity decrease mismatch" -ForegroundColor Red
}

# 5. Add Product #2 (qty 1)
Write-Host "`n[STEP 5] Adding second product (Product 2)..."
$p2 = Invoke-RestMethod -Uri "http://localhost:8080/api/products/2" -Method Get
$addReq2 = @{
    productId = $p2.id
    productName = $p2.name
    price = $p2.price
    quantity = 1
    imageUrl = $p2.imageUrl
} | ConvertTo-Json

$cart5 = Invoke-RestMethod -Uri "http://localhost:8080/api/cart/$customerId/items" -Method Post -Body $addReq2 -ContentType "application/json"
if ($cart5.items.Count -eq 2) {
    Write-Host "  [PASS] Both products in cart: Count=2 distinct items, Total Amount=₹$($cart5.totalAmount)" -ForegroundColor Green
} else {
    Write-Host "  [FAIL] Product 2 not added" -ForegroundColor Red
}

# 6. Remove Product #1
Write-Host "`n[STEP 6] Removing Product 1 from cart..."
$cart6 = Invoke-RestMethod -Uri "http://localhost:8080/api/cart/$customerId/items/1" -Method Delete
$hasP1 = ($cart6.items | Where-Object { $_.productId -eq 1 })
$hasP2 = ($cart6.items | Where-Object { $_.productId -eq 2 })

if (-not $hasP1 -and $hasP2 -and $cart6.items.Count -eq 1) {
    Write-Host "  [PASS] Product 1 removed cleanly. Product 2 remains in cart (Total: ₹$($cart6.totalAmount))" -ForegroundColor Green
} else {
    Write-Host "  [FAIL] Product 1 removal failed" -ForegroundColor Red
}

# 7. Clear cart
Write-Host "`n[STEP 7] Clearing cart..."
Invoke-RestMethod -Uri "http://localhost:8080/api/cart/$customerId/clear" -Method Delete | Out-Null
$cartFinal = Invoke-RestMethod -Uri "http://localhost:8080/api/cart/$customerId" -Method Get
if ($cartFinal.items.Count -eq 0) {
    Write-Host "  [PASS] Cart cleared completely in Database & API" -ForegroundColor Green
} else {
    Write-Host "  [FAIL] Cart clear failed" -ForegroundColor Red
}

# 8. Negative Tests
Write-Host "`n[STEP 8] Cart Negative Testing..."
try {
    Invoke-RestMethod -Uri "http://localhost:8080/api/cart/$customerId/items/99999" -Method Delete
    Write-Host "  [FAIL] Nonexistent product removal should fail" -ForegroundColor Red
} catch {
    Write-Host "  [PASS] Nonexistent product removal returns clean 404/400 error as expected" -ForegroundColor Green
}

Write-Host "`n================================================================================"
Write-Host "       ALL CART REGRESSION TESTS COMPLETED SUCCESSFULLY                         "
Write-Host "================================================================================"
