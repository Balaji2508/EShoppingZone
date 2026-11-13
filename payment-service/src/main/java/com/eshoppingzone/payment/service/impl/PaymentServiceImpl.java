package com.eshoppingzone.payment.service.impl;

import com.eshoppingzone.payment.dto.PaymentRequest;
import com.eshoppingzone.payment.dto.PaymentResponse;
import com.eshoppingzone.payment.entity.Payment;
import com.eshoppingzone.payment.exception.BadRequestException;
import com.eshoppingzone.payment.exception.ResourceNotFoundException;
import com.eshoppingzone.payment.repository.PaymentRepository;
import com.eshoppingzone.payment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentRepository paymentRepository;
    private final RestTemplate restTemplate;

    @Value("${wallet.service.url:http://localhost:8085}")
    private String walletServiceUrl;

    public PaymentServiceImpl(PaymentRepository paymentRepository, RestTemplate restTemplate) {
        this.paymentRepository = paymentRepository;
        this.restTemplate = restTemplate;
    }

    @Override
    public PaymentResponse processPayment(PaymentRequest request) {
        logger.info("Processing payment for User #{}, Amount: ₹{}, Method: {}",
                request.getUserId(), request.getAmount(), request.getPaymentMethod());

        String method = request.getPaymentMethod() != null ? request.getPaymentMethod().trim().toUpperCase() : "COD";
        String paymentId = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String transactionRef = null;
        String paymentStatus = "SUCCESS";
        String maskedCard = null;
        String upiId = null;
        String errorMessage = null;

        try {
            switch (method) {
                case "WALLET":
                    transactionRef = executeWalletPayment(request.getUserId(), request.getOrderId(), request.getAmount());
                    paymentStatus = "PAID";
                    break;

                case "COD":
                case "CASH_ON_DELIVERY":
                    paymentStatus = "COD_PENDING";
                    transactionRef = "COD-REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                    break;

                case "UPI":
                    upiId = request.getUpiId() != null && !request.getUpiId().isBlank() ? request.getUpiId().trim() : "customer@okaxis";
                    if (!upiId.contains("@")) {
                        throw new BadRequestException("Invalid UPI ID format. Expected user@bank format.");
                    }
                    transactionRef = "UPI-TXN-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
                    paymentStatus = "PAID";
                    break;

                case "CARD":
                    String rawCard = request.getCardNumber() != null ? request.getCardNumber().replaceAll("\\s+", "") : "4111111111111234";
                    if (rawCard.length() < 12) {
                        throw new BadRequestException("Invalid card number. Minimum 12 digits required.");
                    }
                    maskedCard = "**** **** **** " + rawCard.substring(rawCard.length() - 4);
                    transactionRef = "CARD-TXN-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
                    paymentStatus = "PAID";
                    break;

                default:
                    throw new BadRequestException("Unsupported payment method: " + method + ". Supported methods: WALLET, COD, UPI, CARD");
            }
        } catch (BadRequestException ex) {
            paymentStatus = "FAILED";
            errorMessage = ex.getMessage();

            Payment failedPayment = new Payment(
                    null,
                    paymentId,
                    request.getOrderId(),
                    request.getUserId(),
                    request.getAmount(),
                    method,
                    paymentStatus,
                    transactionRef,
                    maskedCard,
                    upiId,
                    errorMessage
            );
            paymentRepository.save(failedPayment);
            throw ex;
        }

        Payment payment = new Payment(
                null,
                paymentId,
                request.getOrderId(),
                request.getUserId(),
                request.getAmount(),
                method,
                paymentStatus,
                transactionRef,
                maskedCard,
                upiId,
                null
        );

        Payment saved = paymentRepository.save(payment);
        logger.info("Payment processed successfully: PaymentID {}, Status {}, TxnRef {}",
                saved.getPaymentId(), saved.getPaymentStatus(), saved.getTransactionRef());

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(String paymentId) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with ID: " + paymentId));
        return mapToResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for Order ID: " + orderId));
        return mapToResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByUserId(Long userId) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private String executeWalletPayment(Long userId, Long orderId, Double amount) {
        String url = walletServiceUrl + "/api/wallet/" + userId + "/pay";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("amount", amount);
        body.put("orderId", orderId);
        body.put("description", "Order Payment for Order #" + (orderId != null ? orderId : "NEW"));

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForObject(url, requestEntity, Map.class);
            return "WAL-TXN-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
        } catch (HttpClientErrorException ex) {
            logger.error("Wallet service rejected payment: {}", ex.getResponseBodyAsString());
            throw new BadRequestException("Wallet payment failed: Insufficient balance or invalid wallet");
        } catch (Exception ex) {
            logger.error("Error communicating with wallet-service: {}", ex.getMessage());
            throw new BadRequestException("Wallet payment failed: " + ex.getMessage());
        }
    }

    private PaymentResponse mapToResponse(Payment p) {
        return new PaymentResponse(
                p.getPaymentId(),
                p.getOrderId(),
                p.getUserId(),
                p.getAmount(),
                p.getPaymentMethod(),
                p.getPaymentStatus(),
                p.getTransactionRef(),
                p.getMaskedCardNumber(),
                p.getErrorMessage(),
                p.getCreatedAt()
        );
    }
}
