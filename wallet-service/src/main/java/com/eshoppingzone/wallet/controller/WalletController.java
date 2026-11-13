package com.eshoppingzone.wallet.controller;

import com.eshoppingzone.wallet.dto.AddMoneyRequest;
import com.eshoppingzone.wallet.dto.TransactionResponse;
import com.eshoppingzone.wallet.dto.WalletPaymentRequest;
import com.eshoppingzone.wallet.dto.WalletResponse;
import com.eshoppingzone.wallet.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<WalletResponse> getWallet(@PathVariable Long userId) {
        return ResponseEntity.ok(walletService.getWalletByUserId(userId));
    }

    @PostMapping(value = {"/{userId}/add-money", "/{userId}/topup"})
    public ResponseEntity<WalletResponse> addMoney(@PathVariable Long userId,
                                                   @Valid @RequestBody AddMoneyRequest request) {
        return ResponseEntity.ok(walletService.addMoney(userId, request));
    }

    @PostMapping("/{userId}/pay")
    public ResponseEntity<WalletResponse> makePayment(@PathVariable Long userId,
                                                      @Valid @RequestBody WalletPaymentRequest request) {
        return ResponseEntity.ok(walletService.makePayment(userId, request));
    }

    @GetMapping("/{userId}/transactions")
    public ResponseEntity<List<TransactionResponse>> getTransactions(@PathVariable Long userId) {
        return ResponseEntity.ok(walletService.getTransactionsByUserId(userId));
    }
}
