package com.brokerx.service;

import com.brokerx.entity.Account;
import com.brokerx.repository.AccountRepository;
import org.springframework.stereotype.Service;
import com.brokerx.entity.User;
import com.brokerx.service.WalletService;

import java.util.List;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final WalletService walletService;

    public AccountService(AccountRepository accountRepository, WalletService walletService) {
        this.accountRepository = accountRepository;
        this.walletService = walletService;
    }

    public Account createAccount(User user) {
        Account account = new Account();
        account.setUser(user);
        account.setStatus("PENDING");
        return accountRepository.save(account);
    }

    public Account saveAccount(Account account) {
        Account saved = accountRepository.save(account);

        if ("ACTIVE".equals(saved.getStatus()) && walletService.getWalletByAccountId(saved.getAccountId()) == null) {
            walletService.createWallet(saved);
        }

        return saved;
    }


    public Account getAccountById(UUID accountId) {
        return accountRepository.findById(accountId).orElse(null);
    }   

    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }
    public Account getAccountByUserId(UUID userId) {
        return accountRepository.findByUser_UserId(userId).orElse(null);
    }
}
