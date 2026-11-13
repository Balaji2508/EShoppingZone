package com.eshoppingzone.wallet.service.impl;

import com.eshoppingzone.wallet.dto.AddMoneyRequest;
import com.eshoppingzone.wallet.dto.TransactionResponse;
import com.eshoppingzone.wallet.dto.WalletPaymentRequest;
import com.eshoppingzone.wallet.dto.WalletResponse;
import com.eshoppingzone.wallet.entity.Wallet;
import com.eshoppingzone.wallet.entity.WalletTransaction;
import com.eshoppingzone.wallet.exception.BadRequestException;
import com.eshoppingzone.wallet.repository.WalletRepository;
import com.eshoppingzone.wallet.repository.WalletTransactionRepository;
import com.eshoppingzone.wallet.service.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class WalletServiceImpl implements WalletService {

    private static final Logger logger = LoggerFactory.getLogger(WalletServiceImpl.class);

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    public WalletServiceImpl(WalletRepository walletRepository,
                             WalletTransactionRepository walletTransactionRepository) {
        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
    }

    private Wallet getOrCreateWallet(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseGet(() -> {
                    logger.info("Creating new wallet for user ID: {}", userId);
                    Wallet newWallet = new Wallet();
                    newWallet.setUserId(userId);
                    newWallet.setBalance(0.0);
                    return walletRepository.save(newWallet);
                });
    }

    @Override
    public WalletResponse getWalletByUserId(Long userId) {
        logger.info("Fetching wallet for user ID: {}", userId);
        Wallet wallet = getOrCreateWallet(userId);
        return new WalletResponse(wallet.getId(), wallet.getUserId(), wallet.getBalance(), "Wallet fetched successfully");
    }

    @Override
    public WalletResponse addMoney(Long userId, AddMoneyRequest request) {
        logger.info("Adding ₹{} to wallet for user ID: {}", request.getAmount(), userId);
        if (request.getAmount() <= 0) {
            throw new BadRequestException("Amount to add must be greater than zero");
        }

        Wallet wallet = getOrCreateWallet(userId);
        wallet.setBalance(wallet.getBalance() + request.getAmount());
        Wallet saved = walletRepository.save(wallet);

        // Record CREDIT transaction
        WalletTransaction transaction = new WalletTransaction();
        transaction.setUserId(userId);
        transaction.setAmount(request.getAmount());
        transaction.setType("CREDIT");
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setDescription("Added money to wallet");
        transaction.setStatus("SUCCESS");
        walletTransactionRepository.save(transaction);

        logger.info("Money added successfully. New balance for user ID {}: ₹{}", userId, saved.getBalance());
        return new WalletResponse(saved.getId(), saved.getUserId(), saved.getBalance(), "Money added to wallet successfully");
    }

    @Override
    public WalletResponse makePayment(Long userId, WalletPaymentRequest request) {
        logger.info("Processing wallet payment of ₹{} for user ID: {}", request.getAmount(), userId);
        if (request.getAmount() <= 0) {
            throw new BadRequestException("Payment amount must be greater than zero");
        }

        Wallet wallet = getOrCreateWallet(userId);

        if (wallet.getBalance() < request.getAmount()) {
            logger.warn("Payment failed: Insufficient balance for user ID: {}. Required: ₹{}, Available: ₹{}",
                    userId, request.getAmount(), wallet.getBalance());

            // Record FAILED DEBIT transaction
            WalletTransaction failedTx = new WalletTransaction();
            failedTx.setUserId(userId);
            failedTx.setAmount(request.getAmount());
            failedTx.setType("DEBIT");
            failedTx.setTransactionDate(LocalDateTime.now());
            failedTx.setDescription(request.getDescription() != null ? request.getDescription() : "Order Payment (Failed)");
            failedTx.setStatus("FAILED");
            walletTransactionRepository.save(failedTx);

            throw new BadRequestException("Insufficient wallet balance. Available: ₹" + wallet.getBalance() + ", Required: ₹" + request.getAmount());
        }

        // Deduct balance
        wallet.setBalance(wallet.getBalance() - request.getAmount());
        Wallet saved = walletRepository.save(wallet);

        // Record SUCCESSFUL DEBIT transaction
        WalletTransaction tx = new WalletTransaction();
        tx.setUserId(userId);
        tx.setAmount(request.getAmount());
        tx.setType("DEBIT");
        tx.setTransactionDate(LocalDateTime.now());
        tx.setDescription(request.getDescription() != null ? request.getDescription() : "Order Payment");
        tx.setStatus("SUCCESS");
        walletTransactionRepository.save(tx);

        logger.info("Wallet payment of ₹{} successful for user ID: {}. Remaining balance: ₹{}", request.getAmount(), userId, saved.getBalance());
        return new WalletResponse(saved.getId(), saved.getUserId(), saved.getBalance(), "Payment completed successfully");
    }

    @Override
    public List<TransactionResponse> getTransactionsByUserId(Long userId) {
        logger.info("Fetching transaction history for user ID: {}", userId);
        return walletTransactionRepository.findByUserIdOrderByTransactionDateDesc(userId).stream()
                .map(tx -> new TransactionResponse(
                        tx.getId(),
                        tx.getUserId(),
                        tx.getAmount(),
                        tx.getType(),
                        tx.getTransactionDate(),
                        tx.getDescription(),
                        tx.getStatus()
                ))
                .collect(Collectors.toList());
    }
}
