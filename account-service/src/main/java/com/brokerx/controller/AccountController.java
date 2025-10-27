package com.brokerx.controller;

import java.util.List;
import com.brokerx.entity.Account;
import com.brokerx.entity.User;
import com.brokerx.service.AccountService;
import com.brokerx.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.brokerx.dto.account.AccountDto;
import java.util.stream.Collectors;

import java.util.UUID;

@RestController
@RequestMapping("/accounts")
public class AccountController {
    private final AccountService accountService;
    private final UserService userService;
    
    public AccountController(AccountService accountService, UserService userService) {
        this.accountService = accountService;
        this.userService = userService;
    }

    @PostMapping("/{userId}")
    public ResponseEntity<Account> createAccount(@PathVariable UUID userId) {
        User user = userService.getUser(userId);
        if (user == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(accountService.createAccount(user));
    }
    
    @GetMapping
    public List<AccountDto> getAllAccounts() {
        return accountService.getAllAccounts()
                            .stream()
                            .map(AccountDto::new)
                            .collect(Collectors.toList());
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<Account> getAccountById(@PathVariable UUID accountId) {
        Account account = accountService.getAccountById(accountId);
        if (account == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(account);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Account> getAccountByUserId(@PathVariable UUID userId) {
        Account account = accountService.getAccountByUserId(userId);
        if (account == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(account);
    }
}