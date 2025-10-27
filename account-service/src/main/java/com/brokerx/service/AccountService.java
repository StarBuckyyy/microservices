package com.brokerx.service;

import com.brokerx.entity.Account;
import com.brokerx.entity.User;
import com.brokerx.repository.AccountRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class AccountService {
    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public Account createAccount(User user) {
        Account account = new Account();
        account.setUser(user);
        account.setStatus("PENDING");
        return accountRepository.save(account);
    }

    public Account saveAccount(Account account) {
        return accountRepository.save(account);
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