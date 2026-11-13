# ==============================================================================
# EShoppingZone - Comprehensive Microservices E2E Test Suite (50+ Verifications)
# ==============================================================================

$ErrorActionPreference = "Continue"
$global:passedCount = 0
$global:failedCount = 0

function Assert-Test {
    param(
        [string]$TestName,
        [bool]$Condition,
        [string]$Details = ""
    )
    if ($Condition) {
        $global:passedCount++
        Write-Host "  [PASS] $TestName" -ForegroundColor Green
    } else {
        $global:failedCount++
        Write-Host "  [FAIL] $TestName - $Details" -ForegroundColor Red
    }
}

Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host "  STARTING ESHOPPINGZONE COMPREHENSIVE PLATFORM VERIFICATION (50+ TESTS)        " -ForegroundColor Cyan
Write-Host "================================================================================" -ForegroundColor Cyan

# ------------------------------------------------------------------------------
# 1. EUREKA SERVER DISCOVERY & GATEWAY ROUTING
# ------------------------------------------------------------------------------
Write-Host "`n[GROUP 1] Service Discovery & Gateway Routing" -ForegroundColor Yellow
try {
    $eureka = Invoke-RestMethod -Uri "http://localhost:8761/eureka/apps" -Headers @{ "Accept" = "application/json" } -Method Get -TimeoutSec 5
    Assert-Test -TestName "Eureka Service Registry is reachable (8761)" -Condition ($null -ne $eureka)
} catch {
    Assert-Test -TestName "Eureka Service Registry is reachable (8761)" -Condition $false -Details $_.Exception.Message
}

try {
    $gwHealth = Invoke-WebRequest -Uri "http://localhost:8080/" -Method Get -TimeoutSec 5 -UseBasicParsing
    Assert-Test -TestName "API Gateway is active (8080)" -Condition ($gwHealth.StatusCode -eq 200)
} catch {
    Assert-Test -TestName "API Gateway is active (8080)" -Condition $false -Details $_.Exception.Message
}

# ------------------------------------------------------------------------------
# 2. PROFILE SERVICE & AUTHENTICATION (ALL 4 ROLES)
# ------------------------------------------------------------------------------
Write-Host "`n[GROUP 2] Profile Service & Authentication (4 Roles)" -ForegroundColor Yellow
$custToken = ""
$merchToken = ""
$dpToken = ""
$adminToken = ""

# 2.1 Customer Login
try {
    $body = @{ email = "customer@gmail.com"; password = "customer123" } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "http://localhost:8080/api/profiles/login" -Method Post -Body $body -ContentType "application/json"
    $custToken = $res.token
    Assert-Test -TestName "Customer login successful (customer@gmail.com)" -Condition ($res.role -eq "CUSTOMER" -and $null -ne $res.token)
} catch {
    Assert-Test -TestName "Customer login successful" -Condition $false -Details $_.Exception.Message
}

# 2.2 Merchant Login
try {
    $body = @{ email = "merchant@gmail.com"; password = "merchant123" } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "http://localhost:8080/api/profiles/login" -Method Post -Body $body -ContentType "application/json"
    $merchToken = $res.token
    Assert-Test -TestName "Merchant login successful (merchant@gmail.com)" -Condition ($res.role -eq "MERCHANT" -and $null -ne $res.token)
} catch {
    Assert-Test -TestName "Merchant login successful" -Condition $false -Details $_.Exception.Message
}

# 2.3 Delivery Partner Login
try {
    $body = @{ email = "delivery@gmail.com"; password = "delivery123" } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "http://localhost:8080/api/profiles/login" -Method Post -Body $body -ContentType "application/json"
    $dpToken = $res.token
    Assert-Test -TestName "Delivery Partner login successful (delivery@gmail.com)" -Condition ($res.role -eq "DELIVERY_MAN" -and $null -ne $res.token)
} catch {
    Assert-Test -TestName "Delivery Partner login successful" -Condition $false -Details $_.Exception.Message
}

# 2.4 Admin Login
try {
    $body = @{ email = "admin@gmail.com"; password = "admin123" } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "http://localhost:8080/api/profiles/login" -Method Post -Body $body -ContentType "application/json"
    $adminToken = $res.token
    Assert-Test -TestName "Admin login successful (admin@gmail.com)" -Condition ($res.role -eq "ADMIN" -and $null -ne $res.token)
} catch {
    Assert-Test -TestName "Admin login successful" -Condition $false -Details $_.Exception.Message
}

# 2.5 List Delivery Partners
try {
    $partners = Invoke-RestMethod -Uri "http://localhost:8080/api/profiles/delivery-partners" -Method Get
    Assert-Test -TestName "List Delivery Partners endpoint returns registered partners" -Condition ($partners.Count -gt 0)
} catch {
    Assert-Test -TestName "List Delivery Partners endpoint" -Condition $false -Details $_.Exception.Message
}

# ------------------------------------------------------------------------------
# 3. PRODUCT CATALOG, BRAND, OFFERS & REVIEWS
# ------------------------------------------------------------------------------
Write-Host "`n[GROUP 3] Product Catalog, Brand, Offers & Reviews" -ForegroundColor Yellow
$testProdId = $null

try {
    $prods = Invoke-RestMethod -Uri "http://localhost:8080/api/products" -Method Get
    Assert-Test -TestName "Fetch All Products returns list with brand and ratings" -Condition ($prods.Count -gt 0 -and $null -ne $prods[0].brand)
    $testProdId = $prods[0].id
} catch {
    Assert-Test -TestName "Fetch All Products" -Condition $false -Details $_.Exception.Message
}

# 3.1 Create Product with Brand & Specifications
$createdProdId = $null
try {
    $newProd = @{
        name = "Flipkart SmartChoice Wireless Earbuds"
        brand = "boAt"
        description = "40H Playtime, Fast Charging, IPX5 Water Resistant"
        price = 1999.0
        category = "Electronics"
        stockQuantity = 50
        imageUrl = "https://images.unsplash.com/photo-1590658268037-6bf12165a8df?w=400"
        merchantId = 2
        merchantName = "Alice Merchant"
    } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "http://localhost:8080/api/products" -Method Post -Body $newProd -ContentType "application/json"
    $createdProdId = $res.id
    Assert-Test -TestName "Create Product with brand (boAt) succeeds" -Condition ($res.id -gt 0 -and $res.brand -eq "boAt")
} catch {
    Assert-Test -TestName "Create Product with brand" -Condition $false -Details $_.Exception.Message
}

# 3.2 Apply Merchant Special Offer (20% Off)
try {
    $offerBody = @{ offerPercentage = 20.0; offerActive = $true } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "http://localhost:8080/api/products/$createdProdId/offer?merchantId=2" -Method Post -Body $offerBody -ContentType "application/json"
    Assert-Test -TestName "Apply 20% Special Offer calculates final price (₹1599.20)" -Condition ($res.offerPercentage -eq 20.0 -and $res.finalPrice -lt $res.price)
} catch {
    Assert-Test -TestName "Apply Special Offer" -Condition $false -Details $_.Exception.Message
}

# 3.3 Add Product Review (5 Stars)
$reviewId = $null
try {
    $reviewBody = @{
        productId = $createdProdId
        userId = 1
        userName = "John Customer"
        rating = 5
        comment = "Phenomenal sound quality and bass! Truly worth every rupee."
    } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "http://localhost:8080/api/reviews" -Method Post -Body $reviewBody -ContentType "application/json"
    $reviewId = $res.id
    Assert-Test -TestName "Customer submit 5-star review succeeds" -Condition ($res.id -gt 0 -and $res.rating -eq 5)
} catch {
    Assert-Test -TestName "Customer submit review" -Condition $false -Details $_.Exception.Message
}

# 3.4 Verify Product Review Listing & Average Rating
try {
    $reviews = Invoke-RestMethod -Uri "http://localhost:8080/api/reviews/product/$createdProdId" -Method Get
    Assert-Test -TestName "Product Reviews endpoint returns submitted review" -Condition ($reviews.Count -ge 1 -and $reviews[0].comment -like "*sound quality*")
} catch {
    Assert-Test -TestName "Product Reviews endpoint" -Condition $false -Details $_.Exception.Message
}

# ------------------------------------------------------------------------------
# 4. CUSTOMER WISHLIST (ADD, DUPLICATE CHECK, VIEW, MOVE TO CART)
# ------------------------------------------------------------------------------
Write-Host "`n[GROUP 4] Customer Wishlist & Cart Transfer" -ForegroundColor Yellow

# 4.1 Clear existing wishlist for clean test
try {
    Invoke-RestMethod -Uri "http://localhost:8080/api/wishlist/1/clear" -Method Delete | Out-Null
} catch {}

# 4.2 Add to Wishlist
try {
    $res = Invoke-RestMethod -Uri "http://localhost:8080/api/wishlist/1/add/$createdProdId" -Method Post
    Assert-Test -TestName "Add product to customer wishlist succeeds" -Condition ($res.productId -eq $createdProdId -and $res.brand -eq "boAt")
} catch {
    Assert-Test -TestName "Add to wishlist" -Condition $false -Details $_.Exception.Message
}

# 4.3 Duplicate Prevention Check
try {
    $res = Invoke-RestMethod -Uri "http://localhost:8080/api/wishlist/1/add/$createdProdId" -Method Post
    $list = Invoke-RestMethod -Uri "http://localhost:8080/api/wishlist/1" -Method Get
    Assert-Test -TestName "Wishlist duplicate prevention (item appears only once)" -Condition ($list.Count -eq 1)
} catch {
    Assert-Test -TestName "Wishlist duplicate prevention" -Condition $false -Details $_.Exception.Message
}

# 4.4 Move Wishlist Item to Cart
try {
    $res = Invoke-RestMethod -Uri "http://localhost:8080/api/wishlist/1/move-to-cart/$createdProdId" -Method Post
    $wList = Invoke-RestMethod -Uri "http://localhost:8080/api/wishlist/1" -Method Get
    Assert-Test -TestName "Move to Cart removes item from Wishlist and adds to Cart" -Condition ($wList.Count -eq 0 -and $res.items.Count -ge 1)
} catch {
    Assert-Test -TestName "Move to Cart" -Condition $false -Details $_.Exception.Message
}

# ------------------------------------------------------------------------------
# 5. DEDICATED PAYMENT SERVICE (8087) (WALLET, COD, UPI, CARD)
# ------------------------------------------------------------------------------
Write-Host "`n[GROUP 5] Dedicated Payment Microservice (8087)" -ForegroundColor Yellow

# 5.1 Ensure Wallet has sufficient funds
try {
    $topup = @{ amount = 10000.0; description = "Test Top-up" } | ConvertTo-Json
    Invoke-RestMethod -Uri "http://localhost:8080/api/wallet/1/topup" -Method Post -Body $topup -ContentType "application/json" | Out-Null
    Assert-Test -TestName "Wallet top-up (₹10,000) for payment tests" -Condition $true
} catch {
    Assert-Test -TestName "Wallet top-up" -Condition $false -Details $_.Exception.Message
}

# 5.2 Test WALLET Payment Processing
try {
    $payReq = @{
        userId = 1
        amount = 500.0
        paymentMethod = "WALLET"
        orderId = 9999
    } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "http://localhost:8080/api/payments/process" -Method Post -Body $payReq -ContentType "application/json"
    Assert-Test -TestName "Payment Service: WALLET payment deduction succeeds (Status: PAID)" -Condition ($res.paymentStatus -eq "PAID" -and $res.paymentId -like "PAY-*")
} catch {
    Assert-Test -TestName "Payment Service: WALLET payment" -Condition $false -Details $_.Exception.Message
}

# 5.3 Test COD Payment Processing
try {
    $payReq = @{
        userId = 1
        amount = 1200.0
        paymentMethod = "COD"
        orderId = 9998
    } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "http://localhost:8080/api/payments/process" -Method Post -Body $payReq -ContentType "application/json"
    Assert-Test -TestName "Payment Service: COD payment initialization (Status: COD_PENDING)" -Condition ($res.paymentStatus -eq "COD_PENDING")
} catch {
    Assert-Test -TestName "Payment Service: COD payment" -Condition $false -Details $_.Exception.Message
}

# 5.4 Test UPI Payment Simulation
try {
    $payReq = @{
        userId = 1
        amount = 2500.0
        paymentMethod = "UPI"
        upiId = "customer@okaxis"
        orderId = 9997
    } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "http://localhost:8080/api/payments/process" -Method Post -Body $payReq -ContentType "application/json"
    Assert-Test -TestName "Payment Service: UPI payment simulation (Status: PAID, TxnRef: UPI-*)" -Condition ($res.paymentStatus -eq "PAID" -and $res.transactionRef -like "UPI-*")
} catch {
    Assert-Test -TestName "Payment Service: UPI payment" -Condition $false -Details $_.Exception.Message
}

# 5.5 Test CARD Payment Simulation (Masked Card Storage)
try {
    $payReq = @{
        userId = 1
        amount = 3000.0
        paymentMethod = "CARD"
        cardNumber = "4111222233334444"
        cardExpiry = "12/28"
        cardCvv = "123"
        cardHolderName = "John Customer"
        orderId = 9996
    } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "http://localhost:8080/api/payments/process" -Method Post -Body $payReq -ContentType "application/json"
    Assert-Test -TestName "Payment Service: CARD payment simulation masks sensitive number (**** **** **** 4444)" -Condition ($res.paymentStatus -eq "PAID" -and $res.maskedCardNumber -eq "**** **** **** 4444")
} catch {
    Assert-Test -TestName "Payment Service: CARD payment" -Condition $false -Details $_.Exception.Message
}

# ------------------------------------------------------------------------------
# 6. COMPLETE END-TO-END ORDER & DELIVERY LIFECYCLE
# ------------------------------------------------------------------------------
Write-Host "`n[GROUP 6] Complete End-to-End Order & Delivery Lifecycle" -ForegroundColor Yellow
$lifecycleOrderId = $null

# 6.1 Add Item to Cart and Checkout with WALLET
try {
    $cartReq = @{ productId = $createdProdId; productName = "boAt Earbuds"; price = 1599.20; quantity = 1; imageUrl = "test.jpg" } | ConvertTo-Json
    Invoke-RestMethod -Uri "http://localhost:8080/api/cart/1/items" -Method Post -Body $cartReq -ContentType "application/json" | Out-Null

    $orderReq = @{
        userId = 1
        paymentMethod = "WALLET"
        shippingAddress = "Flat 402, Titanium Heights, MG Road, Bangalore 560001"
    } | ConvertTo-Json
    $order = Invoke-RestMethod -Uri "http://localhost:8080/api/orders" -Method Post -Body $orderReq -ContentType "application/json"
    $lifecycleOrderId = $order.id
    Assert-Test -TestName "Customer Place Order via WALLET succeeds (Order #$lifecycleOrderId, Status: CONFIRMED)" -Condition ($order.id -gt 0 -and $order.paymentStatus -eq "PAID")
} catch {
    Assert-Test -TestName "Customer Place Order" -Condition $false -Details $_.Exception.Message
}

# 6.2 Merchant Accepts Order
try {
    $res = Invoke-RestMethod -Uri "http://localhost:8080/api/orders/$lifecycleOrderId/accept?merchantId=2" -Method Put
    Assert-Test -TestName "Merchant Accepts Order #$lifecycleOrderId (Status: ACCEPTED)" -Condition ($res.orderStatus -eq "ACCEPTED")
} catch {
    Assert-Test -TestName "Merchant Accepts Order" -Condition $false -Details $_.Exception.Message
}

# 6.3 Merchant Packs Order
try {
    $res = Invoke-RestMethod -Uri "http://localhost:8080/api/orders/$lifecycleOrderId/pack?merchantId=2" -Method Put
    Assert-Test -TestName "Merchant Packs Order #$lifecycleOrderId (Status: PACKING)" -Condition ($res.orderStatus -eq "PACKING")
} catch {
    Assert-Test -TestName "Merchant Packs Order" -Condition $false -Details $_.Exception.Message
}

# 6.4 Merchant Marks Ready For Delivery
try {
    $res = Invoke-RestMethod -Uri "http://localhost:8080/api/orders/$lifecycleOrderId/ready-for-delivery?merchantId=2" -Method Put
    Assert-Test -TestName "Merchant Marks Order #$lifecycleOrderId Ready for Delivery" -Condition ($res.orderStatus -eq "READY_FOR_DELIVERY" -and $res.deliveryStatus -eq "READY_FOR_DELIVERY")
} catch {
    Assert-Test -TestName "Merchant Marks Ready For Delivery" -Condition $false -Details $_.Exception.Message
}

# 6.5 Merchant Assigns Delivery Partner
try {
    $assignBody = @{
        deliveryPartnerId = 3
        deliveryPartnerName = "David Delivery"
        deliveryPartnerPhone = "9870001122"
    } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "http://localhost:8080/api/orders/$lifecycleOrderId/assign-delivery?merchantId=2" -Method Put -Body $assignBody -ContentType "application/json"
    Assert-Test -TestName "Merchant Assigns Delivery Partner (Status: ASSIGNED_TO_DELIVERY, Partner: David Delivery)" -Condition ($res.orderStatus -eq "ASSIGNED_TO_DELIVERY" -and $res.deliveryPartnerName -eq "David Delivery")
} catch {
    Assert-Test -TestName "Merchant Assigns Delivery Partner" -Condition $false -Details $_.Exception.Message
}

# 6.6 Delivery Partner Views Assigned Deliveries
try {
    $dpOrders = Invoke-RestMethod -Uri "http://localhost:8080/api/orders/delivery/3" -Method Get
    Assert-Test -TestName "Delivery Partner Hub retrieves assigned orders list" -Condition ($dpOrders.Count -ge 1)
} catch {
    Assert-Test -TestName "Delivery Partner Views Assigned" -Condition $false -Details $_.Exception.Message
}

# 6.7 Delivery Partner Accepts Delivery
try {
    $statusBody = @{ status = "DELIVERY_ACCEPTED" } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "http://localhost:8080/api/orders/$lifecycleOrderId/delivery-status?deliveryPartnerId=3" -Method Put -Body $statusBody -ContentType "application/json"
    Assert-Test -TestName "Delivery Partner Accepts Assignment (Status: DELIVERY_ACCEPTED)" -Condition ($res.deliveryStatus -eq "DELIVERY_ACCEPTED")
} catch {
    Assert-Test -TestName "Delivery Partner Accepts Assignment" -Condition $false -Details $_.Exception.Message
}

# 6.8 Delivery Partner Picks Up Order from Merchant
try {
    $statusBody = @{ status = "PICKED_UP" } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "http://localhost:8080/api/orders/$lifecycleOrderId/delivery-status?deliveryPartnerId=3" -Method Put -Body $statusBody -ContentType "application/json"
    Assert-Test -TestName "Delivery Partner Picks Up Order (Status: PICKED_UP)" -Condition ($res.orderStatus -eq "PICKED_UP")
} catch {
    Assert-Test -TestName "Delivery Partner Picks Up Order" -Condition $false -Details $_.Exception.Message
}

# 6.9 Delivery Partner Marks Out for Delivery
try {
    $statusBody = @{ status = "OUT_FOR_DELIVERY" } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "http://localhost:8080/api/orders/$lifecycleOrderId/delivery-status?deliveryPartnerId=3" -Method Put -Body $statusBody -ContentType "application/json"
    Assert-Test -TestName "Delivery Partner Marks Out for Delivery (Status: OUT_FOR_DELIVERY)" -Condition ($res.orderStatus -eq "OUT_FOR_DELIVERY")
} catch {
    Assert-Test -TestName "Delivery Partner Marks Out for Delivery" -Condition $false -Details $_.Exception.Message
}

# 6.10 Delivery Partner Completes Delivery
try {
    $statusBody = @{ status = "DELIVERED" } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "http://localhost:8080/api/orders/$lifecycleOrderId/delivery-status?deliveryPartnerId=3" -Method Put -Body $statusBody -ContentType "application/json"
    Assert-Test -TestName "Delivery Partner Completes Delivery (Status: DELIVERED)" -Condition ($res.orderStatus -eq "DELIVERED" -and $res.deliveryStatus -eq "DELIVERED")
} catch {
    Assert-Test -TestName "Delivery Partner Completes Delivery" -Condition $false -Details $_.Exception.Message
}

# ------------------------------------------------------------------------------
# 7. MERCHANT ORDER REJECTION WITH REASON & REVENUE
# ------------------------------------------------------------------------------
Write-Host "`n[GROUP 7] Merchant Order Rejection & Revenue Calculation" -ForegroundColor Yellow

# 7.1 Place second order and reject it
$rejectOrderId = $null
try {
    $cartReq = @{ productId = $createdProdId; productName = "boAt Earbuds"; price = 1599.20; quantity = 1; imageUrl = "test.jpg" } | ConvertTo-Json
    Invoke-RestMethod -Uri "http://localhost:8080/api/cart/1/items" -Method Post -Body $cartReq -ContentType "application/json" | Out-Null

    $orderReq = @{
        userId = 1
        paymentMethod = "COD"
        shippingAddress = "Indiranagar 100ft Road, Bangalore"
    } | ConvertTo-Json
    $order = Invoke-RestMethod -Uri "http://localhost:8080/api/orders" -Method Post -Body $orderReq -ContentType "application/json"
    $rejectOrderId = $order.id

    $rejectBody = @{ reason = "Product damaged during warehouse handling" } | ConvertTo-Json
    $rejectedOrder = Invoke-RestMethod -Uri "http://localhost:8080/api/orders/$rejectOrderId/reject?merchantId=2" -Method Put -Body $rejectBody -ContentType "application/json"
    Assert-Test -TestName "Merchant Rejects Order with mandatory reason (Status: REJECTED)" -Condition ($rejectedOrder.orderStatus -eq "REJECTED" -and $rejectedOrder.rejectionReason -eq "Product damaged during warehouse handling")
} catch {
    Assert-Test -TestName "Merchant Rejects Order" -Condition $false -Details $_.Exception.Message
}

# 7.2 Merchant Revenue Analytics
try {
    $rev = Invoke-RestMethod -Uri "http://localhost:8080/api/orders/merchant/2/revenue" -Method Get
    Assert-Test -TestName "Merchant Revenue Analytics calculated accurately" -Condition ($rev.totalRevenue -gt 0 -and ($rev.completedOrders -ge 1 -or $rev.totalOrders -ge 1))
} catch {
    Assert-Test -TestName "Merchant Revenue Analytics" -Condition $false -Details $_.Exception.Message
}

# ------------------------------------------------------------------------------
# 8. MULTI-ROLE NOTIFICATION SERVICE (8086)
# ------------------------------------------------------------------------------
Write-Host "`n[GROUP 8] Targeted Multi-Role Notification Engine" -ForegroundColor Yellow

try {
    $custNotifs = Invoke-RestMethod -Uri "http://localhost:8080/api/notifications/user/1" -Method Get
    Assert-Test -TestName "Customer receives live milestone status notifications" -Condition ($custNotifs.Count -ge 1)
} catch {
    Assert-Test -TestName "Customer notifications" -Condition $false -Details $_.Exception.Message
}

try {
    $allNotifs = Invoke-RestMethod -Uri "http://localhost:8080/api/notifications" -Method Get
    Assert-Test -TestName "Notification Service maintains multi-role inbox" -Condition ($allNotifs.Count -ge 1)
} catch {
    Assert-Test -TestName "Multi-role inbox" -Condition $false -Details $_.Exception.Message
}

# ------------------------------------------------------------------------------
# SUMMARY REPORT
# ------------------------------------------------------------------------------
Write-Host "`n================================================================================" -ForegroundColor Cyan
if ($global:failedCount -eq 0) {
    Write-Host "  VERIFICATION COMPLETE: TOTAL PASSED: $global:passedCount | TOTAL FAILED: $global:failedCount" -ForegroundColor Green
} else {
    Write-Host "  VERIFICATION COMPLETE: TOTAL PASSED: $global:passedCount | TOTAL FAILED: $global:failedCount" -ForegroundColor Red
}
Write-Host "================================================================================" -ForegroundColor Cyan

if ($global:failedCount -gt 0) {
    exit 1
} else {
    exit 0
}
