package com.eshoppingzone.wallet.config;

import com.eshoppingzone.wallet.entity.Wallet;
import com.eshoppingzone.wallet.entity.WalletTransaction;
import com.eshoppingzone.wallet.repository.WalletRepository;
import com.eshoppingzone.wallet.repository.WalletTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    public DataSeeder(WalletRepository walletRepository, WalletTransactionRepository walletTransactionRepository) {
        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
    }

    @Override
    public void run(String... args) {
        // Customer ID 1 initial wallet balance
        if (walletRepository.findByUserId(1L).isEmpty()) {
            logger.info("Seeding initial wallet balance of ₹100,000 for Customer ID 1...");
            Wallet customerWallet = new Wallet();
            customerWallet.setUserId(1L);
            customerWallet.setBalance(100000.0);
            walletRepository.save(customerWallet);

            WalletTransaction seedTx = new WalletTransaction();
            seedTx.setUserId(1L);
            seedTx.setAmount(100000.0);
            seedTx.setType("CREDIT");
            seedTx.setTransactionDate(LocalDateTime.now());
            seedTx.setDescription("Initial Welcome Credit");
            seedTx.setStatus("SUCCESS");
            walletTransactionRepository.save(seedTx);

            logger.info("Customer ID 1 wallet seeded with ₹100,000 successfully!");
        }
    }
}
