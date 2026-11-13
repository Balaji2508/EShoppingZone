# EShoppingZone - Spring Boot Microservices

EShoppingZone is a complete, beginner-friendly e-commerce application built with **Spring Boot Microservices**, **Spring Cloud Netflix Eureka**, **Spring Cloud Gateway**, **Spring Data JPA**, **MySQL**, **RabbitMQ**, and **Spring Security (JWT & BCrypt)**.

---

## 🏗 Microservices Architecture

```
                                  +------------------------------------+
                                  |     Browser UI / Postman / Client  |
                                  +------------------------------------+
                                                    |
                                                    v
+------------------------+        +------------------------------------+
|  Eureka Registry:8761  |<------>|     API Gateway / Portal: 8080     |
+------------------------+        +------------------------------------+
                                    |        |        |        |        |
         +--------------------------+        |        |        |        +--------------------------+
         |                                   |        |        |                                   |
         v                                   v        v        v                                   v
+------------------+             +-----------------+ +----------------+ +------------------+ +-----------------------+
| Profile: 8081    |             | Product: 8082   | | Cart: 8083     | | Order: 8084      | | Wallet: 8085          |
| DB: profile      |             | DB: product     | | DB: cart       | | DB: order        | | DB: wallet            |
+------------------+             +-----------------+ +----------------+ +------------------+ +-----------------------+
                                          ^                  ^                  |                       ^
                                          |                  |                  |                       |
                                          +------------------+------------------+-----------------------+
                                                                                |
                                                                                v (RabbitMQ order.queue)
                                                                        +-----------------------+
                                                                        | Notification: 8086    |
                                                                        +-----------------------+
```

---

## ⚙️ Service Ports & Databases

| Service Name | Port | Database Name | Description |
| :--- | :--- | :--- | :--- |
| **Eureka Server** | `8761` | — | Service discovery & registry |
| **API Gateway** | `8080` | — | Single entry point & Web UI (`http://localhost:8080`) |
| **Profile Service** | `8081` | `eshopping_profile` | User registration, login, JWT & BCrypt |
| **Product Service** | `8082` | `eshopping_product` | Public catalog, search, categories, stock |
| **Cart Service** | `8083` | `eshopping_cart` | Shopping cart, items, subtotals, total |
| **Order Service** | `8084` | `eshopping_order` | Checkout, Wallet & COD, stock deduction |
| **Wallet Service** | `8085` | `eshopping_wallet` | Digital wallet, payments, CREDIT/DEBIT history |
| **Notification Service** | `8086` | — | RabbitMQ consumer for order notifications |

---

## 🔑 Sample Seed Credentials

| Role | Email | Password | Initial Balance |
| :--- | :--- | :--- | :--- |
| **Customer** | `customer@gmail.com` | `customer123` | ₹100,000.00 |
| **Merchant** | `merchant@gmail.com` | `merchant123` | — |

---

## 🚀 How to Build & Run

### 1. Prerequisites
- **Java 17**
- **Maven 3.9+**
- **MySQL 8.0** running on port `3306` (username: `naresh`, password: `vnaresh2004` or configured in `application.properties`)
- **RabbitMQ** running on port `5672` (e.g. via Docker `docker start rabbitmq`)

### 2. Build All Modules
```powershell
mvn clean package -DskipTests
```

### 3. Startup Order
Start services in the following order:
1. `eureka-server` (Port 8761)
2. `profile-service` (Port 8081)
3. `product-service` (Port 8082)
4. `cart-service` (Port 8083)
5. `wallet-service` (Port 8085)
6. `order-service` (Port 8084)
7. `notification-service` (Port 8086)
8. `api-gateway` (Port 8080)

### 4. Access the Application
- **Web Client UI**: `http://localhost:8080/`
- **Eureka Dashboard**: `http://localhost:8761/`
- **RabbitMQ Management**: `http://localhost:15672/` (guest/guest)
- **Postman Collection**: Import `EShoppingZone_Postman_Collection.json`
