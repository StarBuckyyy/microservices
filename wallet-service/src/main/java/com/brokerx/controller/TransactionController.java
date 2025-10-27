package com.brokerx.controller;

import com.brokerx.dto.transaction.TransactionDto;
import com.brokerx.entity.Transaction;
import com.brokerx.repository.TransactionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionRepository transactionRepository;

    public TransactionController(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @GetMapping
    public List<TransactionDto> getAllTransactions() {
        return transactionRepository.findAll()
                .stream()
                .map(TransactionDto::new)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionDto> getTransactionById(@PathVariable UUID id) {
        return transactionRepository.findById(id)
                .map(transaction -> ResponseEntity.ok(new TransactionDto(transaction)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/wallet/{walletId}")
    public List<TransactionDto> getTransactionsByWalletId(@PathVariable UUID walletId) {
        return transactionRepository.findByWallet_WalletId(walletId)
                .stream()
                .map(TransactionDto::new)
                .collect(Collectors.toList());
    }
}