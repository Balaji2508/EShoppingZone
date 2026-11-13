package com.eshoppingzone.wallet.service;

import com.eshoppingzone.wallet.dto.AddMoneyRequest;
import com.eshoppingzone.wallet.dto.TransactionResponse;
import com.eshoppingzone.wallet.dto.WalletPaymentRequest;
import com.eshoppingzone.wallet.dto.WalletResponse;

import java.util.List;

public interface WalletService {
    WalletResponse getWalletByUserId(Long userId);
    WalletResponse addMoney(Long userId, AddMoneyRequest request);
    WalletResponse makePayment(Long userId, WalletPaymentRequest request);
    List<TransactionResponse> getTransactionsByUserId(Long userId);
}
