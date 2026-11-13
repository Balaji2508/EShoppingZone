@echo off
title EShoppingZone Microservices Launcher
echo ============================================================
echo          STARTING ALL ESHOPPINGZONE MICROSERVICES          
echo ============================================================
echo.

set BASE_DIR=%~dp0
if "%BASE_DIR:~-1%"=="\" set BASE_DIR=%BASE_DIR:~0,-1%

echo 1. Starting Eureka Service Registry (Port 8761)...
start "Eureka-Server" /min java -jar "%BASE_DIR%\eureka-server\target\eureka-server-1.0.0.jar"
ping 127.0.0.1 -n 10 >nul

echo 2. Starting Profile Service (Port 8081)...
start "Profile-Service" /min java -jar "%BASE_DIR%\profile-service\target\profile-service-1.0.0.jar"
ping 127.0.0.1 -n 3 >nul

echo 3. Starting Product Service (Port 8082)...
start "Product-Service" /min java -jar "%BASE_DIR%\product-service\target\product-service-1.0.0.jar"
ping 127.0.0.1 -n 3 >nul

echo 4. Starting Cart Service (Port 8083)...
start "Cart-Service" /min java -jar "%BASE_DIR%\cart-service\target\cart-service-1.0.0.jar"
ping 127.0.0.1 -n 3 >nul

echo 5. Starting Wallet Service (Port 8085)...
start "Wallet-Service" /min java -jar "%BASE_DIR%\wallet-service\target\wallet-service-1.0.0.jar"
ping 127.0.0.1 -n 3 >nul

echo 6. Starting Payment Service (Port 8087)...
start "Payment-Service" /min java -jar "%BASE_DIR%\payment-service\target\payment-service-1.0.0.jar"
ping 127.0.0.1 -n 3 >nul

echo 7. Starting Order Service (Port 8084)...
start "Order-Service" /min java -jar "%BASE_DIR%\order-service\target\order-service-1.0.0.jar"
ping 127.0.0.1 -n 3 >nul

echo 8. Starting Notification Service (Port 8086)...
start "Notification-Service" /min java -jar "%BASE_DIR%\notification-service\target\notification-service-1.0.0.jar"
ping 127.0.0.1 -n 3 >nul

echo 9. Starting API Gateway (Port 8080)...
start "API-Gateway" /min java -jar "%BASE_DIR%\api-gateway\target\api-gateway-1.0.0.jar"

echo.
echo Waiting for all microservices to finish initializing (15 seconds)...
ping 127.0.0.1 -n 15 >nul

echo.
echo ============================================================
echo  ALL 9 SERVICES STARTED! Open Browser at: http://localhost:8080
echo  Eureka Dashboard: http://localhost:8761                     
echo  RabbitMQ Dashboard: http://localhost:15672 (guest/guest)   
echo ============================================================
pause
